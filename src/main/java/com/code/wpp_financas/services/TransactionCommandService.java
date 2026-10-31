package com.code.wpp_financas.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.code.wpp_financas.dtos.CreateTransactionRequest;
import com.code.wpp_financas.models.Account;
import com.code.wpp_financas.models.AccountType;
import com.code.wpp_financas.models.Category;
import com.code.wpp_financas.models.PendingStep;
import com.code.wpp_financas.models.PendingTransaction;
import com.code.wpp_financas.models.TransactionType;
import com.code.wpp_financas.models.User;
import com.code.wpp_financas.repositories.PendingTransactionRepository;
import com.code.wpp_financas.utils.TextUtils;
import com.code.wpp_financas.utils.TransactionInputParser;

@Service
public class TransactionCommandService {

    private static final long PENDING_MINUTES = 10;

    private final PendingTransactionRepository pendingRepository;
    private final TransactionService transactionService;
    private final CategoryService categoryService;
    private final AccountService accountService;
    private final BalanceService balanceService;

    public TransactionCommandService(
            PendingTransactionRepository pendingRepository,
            TransactionService transactionService,
            CategoryService categoryService,
            AccountService accountService,
            BalanceService balanceService) {
        this.pendingRepository = pendingRepository;
        this.transactionService = transactionService;
        this.categoryService = categoryService;
        this.accountService = accountService;
        this.balanceService = balanceService;
    }

    // chamado por !gasto e !ganho
    public String register(User user, TransactionType type, String args) {
        TransactionInputParser.Parsed parsed = TransactionInputParser.parse(args);
        if (parsed == null) {
            return usage(type);
        }
        if (type == TransactionType.INCOME && parsed.useCard()) {
            return "Cartão de crédito não recebe ganho. Tire a palavra \"cartão\".";
        }

        // gasto com "cartão": escolhe entre os cartões; senão, entre os bancos
        List<Account> options = optionsFor(user.getId(), type, parsed.useCard());
        if (options.isEmpty()) {
            return parsed.useCard()
                    ? "Você não tem cartão cadastrado. Use !novocartao para cadastrar um."
                    : "Você não tem nenhum banco cadastrado. Use !novobanco para cadastrar um.";
        }

        // gasto com #categoria: atalho, não precisa perguntar
        Category category = null;
        if (type == TransactionType.EXPENSE && parsed.categoryName() != null) {
            category = categoryService.findByInput(parsed.categoryName())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Não conheço a categoria \"" + parsed.categoryName() + "\". Categorias:\n"
                                    + categoryService.menu()));
        }

        boolean needsCategory = type == TransactionType.EXPENSE && category == null;
        // com uma só opção, não precisa perguntar a conta
        Account account = options.size() == 1 ? options.get(0) : null;

        if (!needsCategory && account != null) {
            return complete(user, type, account, category, parsed.amount(), parsed.description());
        }

        // falta alguma resposta: guarda o que já se sabe e pergunta
        PendingTransaction pending = pendingRepository.findByPhone(user.getPhone())
                .orElseGet(PendingTransaction::new);
        pending.setPhone(user.getPhone());
        pending.setType(type);
        pending.setAmount(parsed.amount());
        pending.setDescription(parsed.description());
        pending.setUseCard(parsed.useCard());
        pending.setCategoryId(category != null ? category.getId() : null);
        pending.setAccountId(account != null ? account.getId() : null);
        pending.setStep(needsCategory ? PendingStep.CATEGORY : PendingStep.ACCOUNT);
        pendingRepository.save(pending);

        return needsCategory ? categoryQuestion(pending) : accountQuestion(pending, options);
    }

    // lançamento esperando resposta; se passou do prazo, descarta e devolve vazio
    public Optional<PendingTransaction> findPending(String phone) {
        Optional<PendingTransaction> pending = pendingRepository.findByPhone(phone);
        if (pending.isPresent() && pending.get().getUpdatedAt()
                .isBefore(Instant.now().minus(PENDING_MINUTES, ChronoUnit.MINUTES))) {
            pendingRepository.delete(pending.get());
            return Optional.empty();
        }
        return pending;
    }

    // a mensagem do usuário é a resposta da pergunta pendente
    public String answerPending(User user, PendingTransaction pending, String text) {
        if (pending.getStep() == PendingStep.CATEGORY) {
            Optional<Category> category = categoryService.findByInput(text);
            if (category.isEmpty()) {
                return "Não entendi a categoria. Escolha pelo número ou pelo nome:\n" + categoryService.menu();
            }
            pending.setCategoryId(category.get().getId());

            // a conta ainda não foi escolhida: segue para a próxima pergunta
            if (pending.getAccountId() == null) {
                pending.setStep(PendingStep.ACCOUNT);
                pendingRepository.save(pending);
                return accountQuestion(pending, optionsFor(user.getId(), pending.getType(), pending.isUseCard()));
            }

            Optional<Account> account = accountService.findOwned(pending.getAccountId(), user.getId());
            if (account.isEmpty()) {
                pendingRepository.delete(pending);
                return "Não encontrei essa conta. Envie o comando de novo.";
            }
            return finishPending(user, pending, account.get(), category.get());
        }

        // etapa ACCOUNT
        List<Account> options = optionsFor(user.getId(), pending.getType(), pending.isUseCard());
        Optional<Account> chosen = accountService.pick(options, text);
        if (chosen.isEmpty()) {
            return "Não entendi. Escolha pelo número ou pelo nome:\n" + accountService.menu(options);
        }
        Category category = pending.getCategoryId() != null
                ? categoryService.findById(pending.getCategoryId()).orElse(null)
                : null;
        return finishPending(user, pending, chosen.get(), category);
    }

    public void discard(PendingTransaction pending) {
        pendingRepository.delete(pending);
    }

    private String finishPending(User user, PendingTransaction pending, Account account, Category category) {
        // apaga antes de gravar: se algo falhar, o usuário recomeça do zero em vez de ficar preso
        pendingRepository.delete(pending);
        return complete(user, pending.getType(), account, category, pending.getAmount(), pending.getDescription());
    }

    private String complete(
            User user, TransactionType type, Account account, Category category,
            BigDecimal amount, String description) {
        transactionService.create(new CreateTransactionRequest(
                user.getId(), account.getId(), type, amount, description,
                category != null ? category.getName() : null, null));
        return confirmation(user, type, account, category, amount, description);
    }

    private List<Account> optionsFor(Long userId, TransactionType type, boolean useCard) {
        return type == TransactionType.EXPENSE && useCard
                ? accountService.cards(userId)
                : accountService.banks(userId);
    }

    private String categoryQuestion(PendingTransaction pending) {
        return "Em qual categoria foi esse gasto de " + TextUtils.formatMoney(pending.getAmount())
                + describe(pending.getDescription()) + "?\n"
                + categoryService.menu()
                + "\n\nResponda com o número ou o nome. (!cancelar para desistir)";
    }

    private String accountQuestion(PendingTransaction pending, List<Account> options) {
        String money = TextUtils.formatMoney(pending.getAmount()) + describe(pending.getDescription());
        String question;
        if (pending.getType() == TransactionType.INCOME) {
            question = "Em qual banco entrou esse ganho de " + money + "?";
        } else if (pending.isUseCard()) {
            question = "Em qual cartão foi esse gasto de " + money + "?";
        } else {
            question = "De qual banco saiu esse gasto de " + money + "?";
        }
        return question + "\n" + accountService.menu(options)
                + "\n\nResponda com o número ou o nome. (!cancelar para desistir)";
    }

    private String confirmation(
            User user, TransactionType type, Account account, Category category,
            BigDecimal amount, String description) {
        String money = TextUtils.formatMoney(amount);
        String detail = describe(description);
        StringBuilder reply = new StringBuilder();

        if (type == TransactionType.EXPENSE) {
            reply.append("Gasto registrado: ").append(money)
                    .append(" em ").append(category != null ? category.getName() : "Sem categoria").append(detail);
            if (account.getType() == AccountType.CREDIT_CARD) {
                reply.append("\nFoi para o cartão ").append(account.getName()).append(" e entra na fatura.");
            } else {
                reply.append("\n").append(balanceLines(user, account));
            }
        } else {
            reply.append("Ganho registrado: ").append(money).append(detail)
                    .append("\n").append(balanceLines(user, account));
        }
        return reply.toString();
    }

    // com mais de um banco, mostra o saldo do banco usado e o geral
    private String balanceLines(User user, Account account) {
        BigDecimal total = balanceService.spendableBalance(user.getId());
        if (accountService.banks(user.getId()).size() > 1) {
            return "Saldo no " + account.getName() + ": "
                    + TextUtils.formatMoney(balanceService.accountBalance(account))
                    + "\nSaldo geral: " + TextUtils.formatMoney(total);
        }
        return "Saldo disponível: " + TextUtils.formatMoney(total);
    }

    private String describe(String description) {
        return description != null ? " (" + description + ")" : "";
    }

    private String usage(TransactionType type) {
        if (type == TransactionType.EXPENSE) {
            return "Não entendi. Use: !gasto valor\nExemplo: !gasto 25,90\nEnvie !ajuda gasto para ver mais.";
        }
        return "Não entendi. Use: !ganho valor\nExemplo: !ganho 1500 salário\nEnvie !ajuda ganho para ver mais.";
    }
}
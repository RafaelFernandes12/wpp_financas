package com.code.wpp_financas.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.code.wpp_financas.dtos.CreateTransactionRequest;
import com.code.wpp_financas.models.Account;
import com.code.wpp_financas.models.AccountType;
import com.code.wpp_financas.models.Category;
import com.code.wpp_financas.models.PendingExpense;
import com.code.wpp_financas.models.TransactionType;
import com.code.wpp_financas.models.User;
import com.code.wpp_financas.repositories.AccountRepository;
import com.code.wpp_financas.repositories.PendingExpenseRepository;
import com.code.wpp_financas.utils.TextUtils;
import com.code.wpp_financas.utils.TransactionInputParser;

@Service
public class TransactionCommandService {

    private static final long PENDING_MINUTES = 10;

    private final AccountRepository accountRepository;
    private final PendingExpenseRepository pendingRepository;
    private final TransactionService transactionService;
    private final CategoryService categoryService;
    private final BalanceService balanceService;

    public TransactionCommandService(
            AccountRepository accountRepository,
            PendingExpenseRepository pendingRepository,
            TransactionService transactionService,
            CategoryService categoryService,
            BalanceService balanceService) {
        this.accountRepository = accountRepository;
        this.pendingRepository = pendingRepository;
        this.transactionService = transactionService;
        this.categoryService = categoryService;
        this.balanceService = balanceService;
    }

    // chamado por !gasto e !receita
    public String register(User user, TransactionType type, String args) {
        TransactionInputParser.Parsed parsed = TransactionInputParser.parse(args);
        if (parsed == null) {
            return usage(type);
        }

        // sem "cartão" no fim: conta corrente; com "cartão": cartão de crédito
        AccountType wanted = parsed.useCard() ? AccountType.CREDIT_CARD : AccountType.CHECKING;
        Account account = accountRepository.findByUserId(user.getId()).stream()
                .filter(a -> a.getType() == wanted)
                .findFirst()
                .orElse(null);

        if (account == null) {
            return parsed.useCard()
                    ? "Você não tem cartão cadastrado. Para lançar na conta corrente, tire a palavra \"cartão\"."
                    : "Não encontrei a sua conta corrente.";
        }

        // receita não tem categoria: registra direto
        if (type == TransactionType.INCOME) {
            transactionService.create(new CreateTransactionRequest(
                    user.getId(), account.getId(), type, parsed.amount(),
                    parsed.description(), null, null));
            return confirmation(user, type, parsed.amount(), parsed.description(), null, parsed.useCard());
        }

        // gasto com #categoria: atalho, não precisa perguntar
        if (parsed.categoryName() != null) {
            Category category = categoryService.findByInput(parsed.categoryName())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Não conheço a categoria \"" + parsed.categoryName() + "\". Categorias:\n"
                                    + categoryService.menu()));
            transactionService.create(new CreateTransactionRequest(
                    user.getId(), account.getId(), type, parsed.amount(),
                    parsed.description(), category.getName(), null));
            return confirmation(user, type, parsed.amount(), parsed.description(),
                    category.getName(), parsed.useCard());
        }

        // gasto sem categoria: guarda e pergunta
        PendingExpense pending = pendingRepository.findByPhone(user.getPhone())
                .orElseGet(PendingExpense::new);
        pending.setPhone(user.getPhone());
        pending.setAccountId(account.getId());
        pending.setAmount(parsed.amount());
        pending.setDescription(parsed.description());
        pending.setUseCard(parsed.useCard());
        pendingRepository.save(pending);

        return "Em qual categoria foi esse gasto de " + TextUtils.formatMoney(parsed.amount())
                + describe(parsed.description()) + "?\n"
                + categoryService.menu()
                + "\n\nResponda com o número ou o nome. (!cancelar para desistir)";
    }

    // gasto esperando categoria; se passou do prazo, descarta e devolve vazio
    public Optional<PendingExpense> findPending(String phone) {
        Optional<PendingExpense> pending = pendingRepository.findByPhone(phone);
        if (pending.isPresent() && pending.get().getUpdatedAt()
                .isBefore(Instant.now().minus(PENDING_MINUTES, ChronoUnit.MINUTES))) {
            pendingRepository.delete(pending.get());
            return Optional.empty();
        }
        return pending;
    }

    // a mensagem do usuário é a resposta da categoria
    public String answerPending(User user, PendingExpense pending, String text) {
        Optional<Category> category = categoryService.findByInput(text);
        if (category.isEmpty()) {
            return "Não entendi a categoria. Escolha pelo número ou pelo nome:\n" + categoryService.menu();
        }

        transactionService.create(new CreateTransactionRequest(
                user.getId(), pending.getAccountId(), TransactionType.EXPENSE, pending.getAmount(),
                pending.getDescription(), category.get().getName(), null));
        pendingRepository.delete(pending);

        return confirmation(user, TransactionType.EXPENSE, pending.getAmount(),
                pending.getDescription(), category.get().getName(), pending.isUseCard());
    }

    public void discard(PendingExpense pending) {
        pendingRepository.delete(pending);
    }

    private String confirmation(
            User user, TransactionType type, BigDecimal amount, String description,
            String categoryName, boolean useCard) {
        String money = TextUtils.formatMoney(amount);
        String detail = describe(description);
        StringBuilder reply = new StringBuilder();

        if (type == TransactionType.EXPENSE) {
            reply.append("Gasto registrado: ").append(money)
                    .append(" em ").append(categoryName).append(detail);
            if (useCard) {
                reply.append("\nFoi para o cartão e entra na sua fatura.");
            } else {
                reply.append("\nSaldo disponível: ")
                        .append(TextUtils.formatMoney(balanceService.spendableBalance(user.getId())));
            }
        } else {
            reply.append("Receita registrada: ").append(money).append(detail)
                    .append("\nSaldo disponível: ")
                    .append(TextUtils.formatMoney(balanceService.spendableBalance(user.getId())));
        }
        return reply.toString();
    }

    private String describe(String description) {
        return description != null ? " (" + description + ")" : "";
    }

    private String usage(TransactionType type) {
        if (type == TransactionType.EXPENSE) {
            return "Não entendi. Use: !gasto valor\nExemplo: !gasto 25,90\nEnvie !ajuda gasto para ver mais.";
        }
        return "Não entendi. Use: !receita valor\nExemplo: !receita 1500\nEnvie !ajuda receita para ver mais.";
    }
}
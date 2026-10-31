package com.code.wpp_financas.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.code.wpp_financas.models.Account;
import com.code.wpp_financas.models.AccountType;
import com.code.wpp_financas.models.ActionKind;
import com.code.wpp_financas.models.ActionStep;
import com.code.wpp_financas.models.PendingAction;
import com.code.wpp_financas.models.Transaction;
import com.code.wpp_financas.models.User;
import com.code.wpp_financas.repositories.PendingActionRepository;
import com.code.wpp_financas.repositories.TransactionRepository;
import com.code.wpp_financas.utils.TextUtils;

@Service
public class ActionCommandService {

    private static final long PENDING_MINUTES = 10;
    private static final String CHOOSE = "\n\nResponda com o número ou o nome. (!cancelar para desistir)";
    private static final String NO_BANK = "Você não tem nenhum banco cadastrado. Use !novobanco para cadastrar um.";
    private static final String NOT_FOUND = "Não encontrei essa conta. Envie o comando de novo.";

    private final PendingActionRepository pendingRepository;
    private final TransactionRepository transactionRepository;
    private final TransferService transferService;
    private final StatementService statementService;
    private final InvoiceService invoiceService;
    private final AccountService accountService;
    private final BalanceService balanceService;

    public ActionCommandService(
            PendingActionRepository pendingRepository,
            TransactionRepository transactionRepository,
            TransferService transferService,
            StatementService statementService,
            InvoiceService invoiceService,
            AccountService accountService,
            BalanceService balanceService) {
        this.pendingRepository = pendingRepository;
        this.transactionRepository = transactionRepository;
        this.transferService = transferService;
        this.statementService = statementService;
        this.invoiceService = invoiceService;
        this.accountService = accountService;
        this.balanceService = balanceService;
    }

    // ---------- comandos ----------

    // !guardar 500
    public String saveToReserve(User user, String args) {
        BigDecimal amount = parseAmount(args);
        if (amount == null) {
            return "Use: !guardar valor\nExemplo: !guardar 500";
        }
        PendingAction pending = newPending(user.getPhone(), ActionKind.SAVE);
        pending.setAmount(amount);
        return advance(user, pending);
    }

    // !resgatar 200
    public String withdrawFromReserve(User user, String args) {
        BigDecimal amount = parseAmount(args);
        if (amount == null) {
            return "Use: !resgatar valor\nExemplo: !resgatar 200";
        }
        PendingAction pending = newPending(user.getPhone(), ActionKind.WITHDRAW);
        pending.setAmount(amount);
        return advance(user, pending);
    }

    // !pagarfatura, !pagarfatura 300, !pagarfatura inter, !pagarfatura 300 inter
    public String payInvoice(User user, String args) {
        List<Account> cards = accountService.cards(user.getId());
        if (cards.isEmpty()) {
            return "Você não tem cartão cadastrado. Use !novocartao para cadastrar um.";
        }
        if (accountService.banks(user.getId()).isEmpty()) {
            return NO_BANK;
        }

        // o primeiro pedaço pode ser o valor; o resto (ou tudo, se não houver valor) é o nome do cartão
        String[] tokens = split(args);
        BigDecimal amount = null;
        String cardName = "";
        if (tokens.length > 0) {
            BigDecimal first = TextUtils.parseMoney(tokens[0]);
            if (first != null) {
                amount = first;
                cardName = String.join(" ", Arrays.copyOfRange(tokens, 1, tokens.length));
            } else {
                cardName = String.join(" ", tokens);
            }
        }
        if (amount != null && amount.signum() <= 0) {
            return "O valor precisa ser maior que zero.";
        }

        PendingAction pending = newPending(user.getPhone(), ActionKind.PAY_INVOICE);
        pending.setAmount(amount);

        if (!cardName.isBlank()) {
            Optional<Account> card = accountService.pick(cards, cardName);
            if (card.isEmpty()) {
                return "Não encontrei o cartão \"" + cardName + "\". Seus cartões:\n" + accountService.menu(cards);
            }
            pending.setCardId(card.get().getId());
        } else if (cards.size() == 1) {
            pending.setCardId(cards.get(0).getId());
        }
        return advance(user, pending);
    }

    // !desfazer: mostra o último lançamento e pede confirmação
    public String undo(User user) {
        List<Transaction> last = transactionRepository.recentByUser(user.getId(), PageRequest.of(0, 1));
        if (last.isEmpty()) {
            return "Você ainda não tem lançamentos para desfazer.";
        }
        Transaction transaction = last.get(0);

        PendingAction pending = newPending(user.getPhone(), ActionKind.UNDO);
        pending.setTargetId(transaction.getId());
        return ask(pending, ActionStep.CONFIRM,
                "Desfazer este lançamento?\n" + describe(transaction)
                        + "\n\nResponda sim ou não. (!cancelar para desistir)");
    }

    // ---------- perguntas pendentes ----------

    // ação esperando resposta; se passou do prazo, descarta e devolve vazio
    public Optional<PendingAction> findPending(String phone) {
        Optional<PendingAction> pending = pendingRepository.findByPhone(phone);
        if (pending.isPresent() && pending.get().getUpdatedAt()
                .isBefore(Instant.now().minus(PENDING_MINUTES, ChronoUnit.MINUTES))) {
            pendingRepository.delete(pending.get());
            return Optional.empty();
        }
        return pending;
    }

    // a mensagem do usuário é a resposta da pergunta pendente
    public String answer(User user, PendingAction pending, String text) {
        if (pending.getStep() == ActionStep.CONFIRM) {
            return answerUndo(user, pending, text);
        }

        boolean askingCard = pending.getStep() == ActionStep.CARD;
        List<Account> options = askingCard
                ? accountService.cards(user.getId())
                : accountService.banks(user.getId());

        Optional<Account> chosen = accountService.pick(options, text);
        if (chosen.isEmpty()) {
            return "Não entendi. Escolha pelo número ou pelo nome:\n"
                    + (askingCard ? cardMenu(options) : accountService.menu(options));
        }

        if (askingCard) {
            pending.setCardId(chosen.get().getId());
        } else {
            pending.setBankId(chosen.get().getId());
        }
        return advance(user, pending);
    }

    public void discard(PendingAction pending) {
        pendingRepository.delete(pending);
    }

    // ---------- fluxos ----------

    private String advance(User user, PendingAction pending) {
        return pending.getKind() == ActionKind.PAY_INVOICE
                ? advancePayInvoice(user, pending)
                : advanceReserve(user, pending);
    }

    // !guardar e !resgatar: só falta saber de qual banco (ou para qual)
    private String advanceReserve(User user, PendingAction pending) {
        List<Account> banks = accountService.banks(user.getId());
        List<Account> reserves = accountService.ofType(user.getId(), AccountType.EMERGENCY_FUND);
        if (banks.isEmpty()) {
            return abort(pending, NO_BANK);
        }
        if (reserves.isEmpty()) {
            return abort(pending, "Você não tem reserva de emergência cadastrada.");
        }
        Account reserve = reserves.get(0);

        boolean saving = pending.getKind() == ActionKind.SAVE;
        String money = TextUtils.formatMoney(pending.getAmount());

        if (pending.getBankId() == null) {
            if (banks.size() > 1) {
                String question = saving
                        ? "De qual banco sai esse valor de " + money + " para a reserva?"
                        : "Em qual banco entra o resgate de " + money + "?";
                return ask(pending, ActionStep.BANK, question + "\n" + accountService.menu(banks) + CHOOSE);
            }
            pending.setBankId(banks.get(0).getId());
        }

        Optional<Account> bank = accountService.findOwned(pending.getBankId(), user.getId());
        if (bank.isEmpty()) {
            return abort(pending, NOT_FOUND);
        }

        pendingRepository.delete(pending);
        return saving
                ? executeSave(user, bank.get(), reserve, pending.getAmount())
                : executeWithdraw(user, bank.get(), reserve, pending.getAmount());
    }

    // !pagarfatura: cartão, valor e banco, nessa ordem; só pergunta o que faltar
    private String advancePayInvoice(User user, PendingAction pending) {
        List<Account> cards = accountService.cards(user.getId());
        List<Account> banks = accountService.banks(user.getId());
        if (banks.isEmpty()) {
            return abort(pending, NO_BANK);
        }

        // 1. qual cartão
        if (pending.getCardId() == null) {
            return ask(pending, ActionStep.CARD, "Qual fatura você quer pagar?\n" + cardMenu(cards) + CHOOSE);
        }
        Optional<Account> cardOpt = accountService.findOwned(pending.getCardId(), user.getId());
        if (cardOpt.isEmpty()) {
            return abort(pending, NOT_FOUND);
        }
        Account card = cardOpt.get();

        // 2. quanto: o valor informado, ou a fatura fechada inteira
        BigDecimal owed = invoiceService.closedInvoiceOwed(card);
        if (owed.signum() <= 0) {
            return abort(pending, "Não há fatura fechada em aberto no cartão " + card.getName()
                    + ". Use !fatura para ver a situação.");
        }
        BigDecimal amount = pending.getAmount() != null ? pending.getAmount() : owed;
        if (amount.compareTo(owed) > 0) {
            return abort(pending, "O valor é maior que a fatura fechada em aberto (" + TextUtils.formatMoney(owed)
                    + "). Envie !pagarfatura sem valor para pagar tudo.");
        }
        pending.setAmount(amount);

        // 3. de qual banco sai
        if (pending.getBankId() == null) {
            if (banks.size() > 1) {
                return ask(pending, ActionStep.BANK,
                        "De qual banco sai o pagamento de " + TextUtils.formatMoney(amount)
                                + " (fatura " + card.getName() + ")?\n" + accountService.menu(banks) + CHOOSE);
            }
            pending.setBankId(banks.get(0).getId());
        }
        Optional<Account> bankOpt = accountService.findOwned(pending.getBankId(), user.getId());
        if (bankOpt.isEmpty()) {
            return abort(pending, NOT_FOUND);
        }
        Account bank = bankOpt.get();

        pendingRepository.delete(pending);
        transferService.transfer(user, bank, card, amount, "Pagamento da fatura " + card.getName());

        BigDecimal remaining = invoiceService.closedInvoiceOwed(card);
        StringBuilder reply = new StringBuilder("Fatura paga: ")
                .append(TextUtils.formatMoney(amount)).append(" do cartão ").append(card.getName())
                .append(" com o ").append(bank.getName()).append(".\n");
        reply.append(remaining.signum() > 0
                ? "Ainda falta pagar " + TextUtils.formatMoney(remaining) + " da fatura fechada."
                : "Fatura fechada quitada.");
        reply.append("\n").append(bankBalanceLines(user, bank));
        warnIfNegative(reply, bank);
        return reply.toString();
    }

    private String answerUndo(User user, PendingAction pending, String text) {
        if (TextUtils.isNo(text)) {
            pendingRepository.delete(pending);
            return "Ok, nada foi apagado.";
        }
        if (!TextUtils.isYes(text)) {
            return "Responda com sim ou não. (!cancelar para desistir)";
        }

        Optional<Transaction> transaction = transactionRepository.detailedById(pending.getTargetId(), user.getId());
        pendingRepository.delete(pending);
        if (transaction.isEmpty()) {
            return "Esse lançamento não existe mais.";
        }

        String described = describe(transaction.get());
        transferService.delete(transaction.get());
        return "Desfeito:\n" + described;
    }

    // ---------- execução ----------

    private String executeSave(User user, Account bank, Account reserve, BigDecimal amount) {
        transferService.transfer(user, bank, reserve, amount, "Guardado na reserva");

        StringBuilder reply = new StringBuilder("Guardado: ")
                .append(TextUtils.formatMoney(amount)).append(" de ").append(bank.getName())
                .append(" para a reserva.\n")
                .append(bankBalanceLines(user, bank))
                .append("\nReserva: ").append(TextUtils.formatMoney(balanceService.accountBalance(reserve)));
        warnIfNegative(reply, bank);
        return reply.toString();
    }

    private String executeWithdraw(User user, Account bank, Account reserve, BigDecimal amount) {
        transferService.transfer(user, reserve, bank, amount, "Resgatado da reserva");

        StringBuilder reply = new StringBuilder("Resgatado: ")
                .append(TextUtils.formatMoney(amount)).append(" da reserva para o ").append(bank.getName())
                .append(".\nReserva: ").append(TextUtils.formatMoney(balanceService.accountBalance(reserve)))
                .append("\n").append(bankBalanceLines(user, bank));
        warnIfNegative(reply, reserve);
        return reply.toString();
    }

    // ---------- utilitários ----------

    private PendingAction newPending(String phone, ActionKind kind) {
        PendingAction pending = pendingRepository.findByPhone(phone).orElseGet(PendingAction::new);
        pending.setPhone(phone);
        pending.setKind(kind);
        pending.setStep(ActionStep.BANK);
        pending.setAmount(null);
        pending.setCardId(null);
        pending.setBankId(null);
        pending.setTargetId(null);
        return pending;
    }

    private String ask(PendingAction pending, ActionStep step, String question) {
        pending.setStep(step);
        pendingRepository.save(pending);
        return question;
    }

    private String abort(PendingAction pending, String message) {
        pendingRepository.delete(pending);
        return message;
    }

    private BigDecimal parseAmount(String args) {
        String[] tokens = split(args);
        if (tokens.length == 0) {
            return null;
        }
        BigDecimal value = TextUtils.parseMoney(tokens[0]);
        return value != null && value.signum() > 0 ? value : null;
    }

    private String[] split(String args) {
        return args == null || args.isBlank() ? new String[0] : args.trim().split("\\s+");
    }

    // "1 - Nubank (fatura fechada: R$ 300,00)"
    private String cardMenu(List<Account> cards) {
        StringBuilder menu = new StringBuilder();
        for (int i = 0; i < cards.size(); i++) {
            Account card = cards.get(i);
            BigDecimal owed = invoiceService.closedInvoiceOwed(card);
            menu.append(i + 1).append(" - ").append(card.getName())
                    .append(owed.signum() > 0
                            ? " (fatura fechada: " + TextUtils.formatMoney(owed) + ")"
                            : " (sem fatura fechada)")
                    .append("\n");
        }
        return menu.toString().trim();
    }

    // o lançamento; se for transferência, as duas pontas
    // o lançamento; transferência: as duas pontas; compra parcelada: a primeira parcela e o resumo
    private String describe(Transaction transaction) {
        if (transaction.getTransferId() != null) {
            StringBuilder text = new StringBuilder();
            for (Transaction leg : transactionRepository.legsOf(transaction.getTransferId())) {
                text.append(statementService.formatLine(leg)).append("\n");
            }
            return text.toString().trim();
        }

        if (transaction.getInstallmentGroupId() != null) {
            List<Transaction> installments =
                    transactionRepository.installmentsOf(transaction.getInstallmentGroupId());
            if (!installments.isEmpty()) {
                BigDecimal total = installments.stream()
                        .map(Transaction::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                return statementService.formatLine(installments.get(0))
                        + "\n+ mais " + (installments.size() - 1) + " parcelas (total da compra: "
                        + TextUtils.formatMoney(total) + ")";
            }
        }

        return statementService.formatLine(transaction);
    }

    private String bankBalanceLines(User user, Account bank) {
        BigDecimal total = balanceService.spendableBalance(user.getId());
        if (accountService.banks(user.getId()).size() > 1) {
            return "Saldo no " + bank.getName() + ": "
                    + TextUtils.formatMoney(balanceService.accountBalance(bank))
                    + "\nSaldo geral: " + TextUtils.formatMoney(total);
        }
        return "Saldo disponível: " + TextUtils.formatMoney(total);
    }

    private void warnIfNegative(StringBuilder reply, Account account) {
        if (balanceService.accountBalance(account).signum() < 0) {
            reply.append("\nAtenção: o saldo de ").append(account.getName()).append(" ficou negativo.");
        }
    }
}
package com.code.wpp_financas.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.code.wpp_financas.models.Account;
import com.code.wpp_financas.models.TransactionType;
import com.code.wpp_financas.models.User;
import com.code.wpp_financas.repositories.TransactionRepository;
import com.code.wpp_financas.utils.BillingCycle;
import com.code.wpp_financas.utils.TextUtils;

@Service
public class InvoiceService {

    private static final ZoneId ZONE = ZoneId.of("America/Fortaleza");
    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd/MM");

    // texto de um cartão e quanto ele tem a pagar (fatura aberta + fechada ainda não vencida)
    private record Summary(String text, BigDecimal toPay) {
    }

    private final AccountService accountService;
    private final TransactionRepository transactionRepository;

    public InvoiceService(AccountService accountService, TransactionRepository transactionRepository) {
        this.accountService = accountService;
        this.transactionRepository = transactionRepository;
    }

    public String invoice(User user, String args) {
        List<Account> cards = accountService.cards(user.getId());
        if (cards.isEmpty()) {
            return "Você não tem cartão cadastrado. Use !novocartao para cadastrar um.";
        }

        LocalDate today = LocalDate.now(ZONE);
        String arg = args == null ? "" : args.trim();

        // !fatura geral: uma linha por cartão e o total
        if (TextUtils.normalize(arg).equals("geral")) {
            StringBuilder text = new StringBuilder("*Faturas (geral)*\n");
            BigDecimal total = BigDecimal.ZERO;
            for (Account card : cards) {
                BigDecimal toPay = summary(card, today).toPay();
                text.append(card.getName()).append(": ").append(TextUtils.formatMoney(toPay)).append("\n");
                total = total.add(toPay);
            }
            text.append("\nTotal: ").append(TextUtils.formatMoney(total))
                    .append("\n(faturas abertas e fechadas ainda não vencidas)");
            return text.toString();
        }

        // !fatura inter: só um cartão
        if (!arg.isEmpty()) {
            Optional<Account> chosen = accountService.pick(cards, arg);
            if (chosen.isEmpty()) {
                return "Não encontrei o cartão \"" + arg + "\". Seus cartões:\n" + accountService.menu(cards);
            }
            return summary(chosen.get(), today).text();
        }

        // !fatura: um cartão só, ou todos com o total no fim
        if (cards.size() == 1) {
            return summary(cards.get(0), today).text();
        }

        StringBuilder text = new StringBuilder();
        BigDecimal total = BigDecimal.ZERO;
        for (Account card : cards) {
            Summary summary = summary(card, today);
            text.append(summary.text()).append("\n\n");
            total = total.add(summary.toPay());
        }
        text.append("*Total das faturas:* ").append(TextUtils.formatMoney(total));
        return text.toString();
    }

    private Summary summary(Account card, LocalDate today) {
        String title = title(card);
        if (card.getClosingDay() == null || card.getDueDay() == null) {
            return new Summary("*" + title + "*\nSem dia de fechamento ou de vencimento. Use !editar fechamento e !editar vencimento.",
                    BigDecimal.ZERO);
        }

        BillingCycle.Statement open =
                BillingCycle.openStatement(card.getClosingDay(), card.getDueDay(), today);
        BillingCycle.Statement previous =
                BillingCycle.previousStatement(open, card.getClosingDay(), card.getDueDay());

        BigDecimal openTotal = expenses(card, open);
        BigDecimal previousTotal = expenses(card, previous);

        // a fatura fechada só aparece enquanto não passou do vencimento
        boolean previousPending = previousTotal.signum() > 0 && !today.isAfter(previous.due());

        StringBuilder reply = new StringBuilder("*" + title + "*\n");

        if (previousPending) {
            reply.append("Fatura fechada: ").append(TextUtils.formatMoney(previousTotal))
                    .append("\nVence em ").append(previous.due().format(DAY_MONTH)).append("\n\n");
        }

        reply.append("Fatura aberta: ").append(TextUtils.formatMoney(openTotal))
                .append("\nFecha em ").append(open.closing().format(DAY_MONTH))
                .append(" e vence em ").append(open.due().format(DAY_MONTH));

        BigDecimal used = openTotal.add(previousPending ? previousTotal : BigDecimal.ZERO);

        if (card.getCreditLimit() != null) {
            reply.append("\n\nLimite disponível: ")
                    .append(TextUtils.formatMoney(card.getCreditLimit().subtract(used)));
        }

        return new Summary(reply.toString(), used);
    }

    private BigDecimal expenses(Account card, BillingCycle.Statement statement) {
        BigDecimal sum = transactionRepository.sumByAccountAndTypeInPeriod(
                card.getId(), TransactionType.EXPENSE, statement.previousClosing(), statement.closing());
        return sum != null ? sum : BigDecimal.ZERO;
    }

    // "Cartão Inter"; se o nome já começa com "cartão", não repete
    private String title(Account card) {
        return TextUtils.normalize(card.getName()).startsWith("cartao")
                ? card.getName()
                : "Cartão " + card.getName();
    }
}
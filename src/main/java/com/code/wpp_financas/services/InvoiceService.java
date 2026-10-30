package com.code.wpp_financas.services;

import com.code.wpp_financas.models.Account;
import com.code.wpp_financas.models.AccountType;
import com.code.wpp_financas.models.TransactionType;
import com.code.wpp_financas.models.User;
import com.code.wpp_financas.repositories.AccountRepository;
import com.code.wpp_financas.repositories.TransactionRepository;
import com.code.wpp_financas.utils.BillingCycle;
import com.code.wpp_financas.utils.TextUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class InvoiceService {

    private static final ZoneId ZONE = ZoneId.of("America/Fortaleza");
    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd-MM");

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public InvoiceService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    public String invoice(User user){
        Account card = accountRepository.findByUserId(user.getId()).stream().filter(account -> account.getType() == AccountType.CREDIT_CARD).findFirst().orElse(null);

        if(card == null){
            return "você não tem cartão cadastrado.";
        }
        if (card.getClosingDay() == null || card.getDueDay() == null){
            return "Seu cartão está sem dia de fechamento ou vencimento.\nUse !editar fechamento e !editar vencimento.";
        }

        LocalDate today = LocalDate.now(ZONE);
        BillingCycle.Statement open = BillingCycle.openStatement(card.getClosingDay(), card.getDueDay(), today);
        BillingCycle.Statement previous = BillingCycle.previusStatement(open, card.getClosingDay(), card.getDueDay());

        BigDecimal openTotal = expenses(card, open);
        BigDecimal previusTotal = expenses(card, previous);

        // a fatura fechada só aparece enquanto não passou do vencimento
        boolean previusPending = previusTotal.signum() > 0 && !today.isAfter(previous.due());

        StringBuilder reply = new StringBuilder("*Cartão de crédito*\n");

        if(previusPending){
            reply.append("Fatura fechada: ").append(TextUtils.formatMoney(previusTotal)).append("\nVence em ").append(previous.due().format(DAY_MONTH)).append("\n\n");
        }

        reply.append("Fatura aberta: ").append(TextUtils.formatMoney(openTotal)).append("\nFecha em ").append(open.closing().format(DAY_MONTH)).append(" e vence em ").append(open.due().format(DAY_MONTH));

        if(card.getCreditLimit() != null){
            BigDecimal used = openTotal.add(previusPending ? previusTotal : BigDecimal.ZERO);
            reply.append("\n\nLimite disponível: ").append(TextUtils.formatMoney(card.getCreditLimit().subtract(used)));
        }

        return reply.toString();
    }

    private BigDecimal expenses(Account card, BillingCycle.Statement statement){
        BigDecimal sum = transactionRepository.sumByAccountAndTypeInPeriod(card.getId(), TransactionType.EXPENSE, statement.previousClosing(), statement.closing());
        return sum != null ? sum : BigDecimal.ZERO;
    }
}

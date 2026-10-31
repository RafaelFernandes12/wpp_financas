package com.code.wpp_financas.services;

import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.code.wpp_financas.models.Account;
import com.code.wpp_financas.models.AccountType;
import com.code.wpp_financas.models.Transaction;
import com.code.wpp_financas.models.TransactionType;
import com.code.wpp_financas.models.User;
import com.code.wpp_financas.repositories.TransactionRepository;
import com.code.wpp_financas.utils.TextUtils;

@Service
public class StatementService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 30;
    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd/MM");

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;

    public StatementService(TransactionRepository transactionRepository, AccountService accountService) {
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
    }

    // !extrato, !extrato 20, !extrato nubank
    public String statement(User user, String args) {
        String arg = args == null ? "" : args.trim();
        int limit = DEFAULT_LIMIT;
        List<Account> filter = List.of();

        if (arg.matches("\\d{1,3}")) {
            limit = Math.min(Math.max(Integer.parseInt(arg), 1), MAX_LIMIT);
        } else if (!arg.isEmpty()) {
            String wanted = TextUtils.normalize(arg);
            // banco e cartão podem ter o mesmo nome: mostra os dois
            filter = accountService.listByUser(user.getId()).stream()
                    .filter(account -> TextUtils.normalize(account.getName()).contains(wanted))
                    .toList();
            if (filter.isEmpty()) {
                return "Não encontrei nenhuma conta com o nome \"" + arg + "\". Use !contas para ver as suas.";
            }
        }

        Pageable page = PageRequest.of(0, limit);
        List<Transaction> transactions = filter.isEmpty()
                ? transactionRepository.recentByUser(user.getId(), page)
                : transactionRepository.recentByAccounts(filter.stream().map(Account::getId).toList(), page);

        if (transactions.isEmpty()) {
            return "Nenhum lançamento encontrado.";
        }

        StringBuilder text = new StringBuilder("*Últimos lançamentos*\n");
        for (Transaction transaction : transactions) {
            text.append(formatLine(transaction)).append("\n");
        }
        return text.toString().trim();
    }

    // "30/10  − R$ 25,90  Alimentação (almoço) · Nubank"
    public String formatLine(Transaction transaction) {
        String sign = transaction.getType() == TransactionType.EXPENSE ? "−" : "+";

        String label;
        if (transaction.getTransferId() != null) {
            label = "Transferência";
        } else if (transaction.getCategory() != null) {
            label = transaction.getCategory().getName();
        } else {
            label = transaction.getType() == TransactionType.INCOME ? "Ganho" : "Sem categoria";
        }

        String description = transaction.getDescription();
        String detail = description != null && !description.isBlank() ? " (" + description + ")" : "";

        Account account = transaction.getAccount();
        String accountLabel = account.getName()
                + (account.getType() == AccountType.CREDIT_CARD ? " (cartão)" : "");

        return transaction.getDate().format(DAY_MONTH) + "  " + sign + " "
                + TextUtils.formatMoney(transaction.getAmount()) + "  " + label + detail + " · " + accountLabel;
    }
}
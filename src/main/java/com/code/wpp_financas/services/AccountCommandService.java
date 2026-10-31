package com.code.wpp_financas.services;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import com.code.wpp_financas.dtos.CreateAccountRequest;
import com.code.wpp_financas.models.Account;
import com.code.wpp_financas.models.AccountType;
import com.code.wpp_financas.models.User;
import com.code.wpp_financas.utils.TextUtils;

@Service
public class AccountCommandService {

    private final AccountService accountService;
    private final BalanceService balanceService;

    public AccountCommandService(AccountService accountService, BalanceService balanceService) {
        this.accountService = accountService;
        this.balanceService = balanceService;
    }

    // !novobanco Nubank 1500,50
    public String newBank(User user, String args) {
        String usage = "Use: !novobanco nome saldo\nExemplo: !novobanco Nubank 1500,50";
        String[] tokens = split(args);
        if (tokens.length < 2) {
            return usage;
        }

        BigDecimal balance = TextUtils.parseMoney(tokens[tokens.length - 1]);
        if (balance == null) {
            return usage;
        }

        String name = String.join(" ", Arrays.copyOfRange(tokens, 0, tokens.length - 1));
        if (accountService.nameTaken(user.getId(), AccountType.CHECKING, name)) {
            return "Você já tem um banco com esse nome.";
        }

        accountService.create(new CreateAccountRequest(
                user.getId(), name, AccountType.CHECKING, balance, null, null, null));
        return "Banco " + name + " cadastrado com saldo de " + TextUtils.formatMoney(balance) + ".";
    }

    // !novocartao Inter 10 17
    public String newCard(User user, String args) {
        String usage = "Use: !novocartao nome fechamento vencimento\nExemplo: !novocartao Inter 10 17";
        String[] tokens = split(args);
        if (tokens.length < 3) {
            return usage;
        }

        Integer closing = TextUtils.parseDay(tokens[tokens.length - 2]);
        Integer due = TextUtils.parseDay(tokens[tokens.length - 1]);
        if (closing == null || due == null) {
            return usage;
        }

        String name = String.join(" ", Arrays.copyOfRange(tokens, 0, tokens.length - 2));
        if (accountService.nameTaken(user.getId(), AccountType.CREDIT_CARD, name)) {
            return "Você já tem um cartão com esse nome.";
        }

        accountService.create(new CreateAccountRequest(
                user.getId(), name, AccountType.CREDIT_CARD, BigDecimal.ZERO, null, closing, due));
        return "Cartão " + name + " cadastrado: fecha dia " + closing + " e vence dia " + due + "."
                + "\nPara definir o limite: !editar limite 3000 " + name;
    }

    // !contas: tudo o que está cadastrado
    public String list(User user) {
        List<Account> banks = accountService.banks(user.getId());
        List<Account> cards = accountService.cards(user.getId());
        List<Account> reserves = accountService.ofType(user.getId(), AccountType.EMERGENCY_FUND);

        StringBuilder text = new StringBuilder("*Bancos*\n");
        if (banks.isEmpty()) {
            text.append("Nenhum. Use !novobanco para cadastrar.\n");
        }
        for (int i = 0; i < banks.size(); i++) {
            Account bank = banks.get(i);
            text.append(i + 1).append(" - ").append(bank.getName()).append(": ")
                    .append(TextUtils.formatMoney(balanceService.accountBalance(bank))).append("\n");
        }

        text.append("\n*Cartões*\n");
        if (cards.isEmpty()) {
            text.append("Nenhum. Use !novocartao para cadastrar.\n");
        }
        for (int i = 0; i < cards.size(); i++) {
            Account card = cards.get(i);
            text.append(i + 1).append(" - ").append(card.getName());
            if (card.getClosingDay() != null && card.getDueDay() != null) {
                text.append(" (fecha dia ").append(card.getClosingDay())
                        .append(", vence dia ").append(card.getDueDay()).append(")");
            }
            text.append("\n");
        }

        if (!reserves.isEmpty()) {
            text.append("\n*Reserva*\n");
            for (Account reserve : reserves) {
                text.append(reserve.getName()).append(": ")
                        .append(TextUtils.formatMoney(balanceService.accountBalance(reserve))).append("\n");
            }
        }
        return text.toString().trim();
    }

    private String[] split(String args) {
        return args == null || args.isBlank() ? new String[0] : args.trim().split("\\s+");
    }
}
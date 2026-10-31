package com.code.wpp_financas.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.code.wpp_financas.models.Account;
import com.code.wpp_financas.models.User;
import com.code.wpp_financas.utils.TextUtils;

@Service
public class BalanceCommandService {

    private final AccountService accountService;
    private final BalanceService balanceService;

    public BalanceCommandService(AccountService accountService, BalanceService balanceService) {
        this.accountService = accountService;
        this.balanceService = balanceService;
    }

    public String balance(User user, String args) {
        List<Account> banks = accountService.banks(user.getId());
        if (banks.isEmpty()) {
            return "Você não tem nenhum banco cadastrado. Use !novobanco para cadastrar um.";
        }

        String arg = args == null ? "" : args.trim();
        BigDecimal total = banks.stream()
                .map(balanceService::accountBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // !saldo geral: só o total
        if (TextUtils.normalize(arg).equals("geral")) {
            return "Saldo geral disponível: " + TextUtils.formatMoney(total);
        }

        // !saldo nubank: só um banco
        if (!arg.isEmpty()) {
            Optional<Account> chosen = accountService.pick(banks, arg);
            if (chosen.isEmpty()) {
                return "Não encontrei o banco \"" + arg + "\". Seus bancos:\n" + accountService.menu(banks);
            }
            return chosen.get().getName() + ": "
                    + TextUtils.formatMoney(balanceService.accountBalance(chosen.get()));
        }

        // !saldo: com um banco só, a resposta curta de sempre
        if (banks.size() == 1) {
            return "Saldo disponível: " + TextUtils.formatMoney(total);
        }

        StringBuilder text = new StringBuilder("*Saldo por banco*\n");
        for (Account bank : banks) {
            text.append(bank.getName()).append(": ")
                    .append(TextUtils.formatMoney(balanceService.accountBalance(bank))).append("\n");
        }
        text.append("\nTotal disponível: ").append(TextUtils.formatMoney(total));
        return text.toString();
    }
}
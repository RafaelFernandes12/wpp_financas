package com.code.wpp_financas.services;

import com.code.wpp_financas.models.Account;
import com.code.wpp_financas.models.AccountType;
import com.code.wpp_financas.models.User;
import com.code.wpp_financas.repositories.AccountRepository;
import com.code.wpp_financas.repositories.UserRepository;
import com.code.wpp_financas.utils.TextUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ProfileCommandService {
    private static final String USAGE = "Use: !editar campo valor\nExemplos:\n!editar nome Silvio Santos\n!editar fechamento 12\n!editar vencimento 19\n!editar limite 3000";
    private static final String NO_CARD = "Você não tem cartão cadastrado.";

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    public ProfileCommandService(UserRepository userRepository, AccountRepository accountRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
    }

    public String edit(User user, String args) {
        String[] parts = args == null ? new String[0] : args.trim().split("\\s+", 2);
        if (parts.length < 2 || parts[0].isEmpty()) {
            return USAGE;
        }

        String field = TextUtils.normalize(parts[0]);
        String value = parts[1].trim();

        return switch (field) {
            case "nome" -> editName(user, value);
            case "fechamento" -> editCardDay(user, value, true);
            case "vencimento" -> editCardDay(user, value, false);
            case "limite" -> editLimit(user, value);
            default -> "Não sei editar \"" + parts[0] + "\". Opções: nome, fechamento, vencimento, limite.";
        };
    }

    private String editName(User user, String value) {
        if (value.length() < 3) {
            return "Não entendi o nome. Envie o nome completo.";
        }
        user.setName(value);
        userRepository.save(user);
        return "Pronto! Seu nome agora é " + value + ".";
    }

    private String editCardDay(User user, String value, boolean closing) {
        Account card = findCard(user);
        if (card == null) {
            return NO_CARD;
        }
        Integer day = TextUtils.parseDay(value);
        if (day == null) {
            return "Envie um número de 1 a 31.";
        }
        if (closing) {
            card.setClosingDay(day);
        } else {
            card.setDueDay(day);
        }
        accountRepository.save(card);
        return closing
                ? "Pronto! A fatura agora fecha no dia " + day + "."
                : "Pronto! A fatura agora vence no dia " + day + ".";
    }

    private String editLimit(User user, String value) {
        Account card = findCard(user);
        if (card == null) {
            return NO_CARD;
        }
        BigDecimal limit = TextUtils.parseMoney(value);
        if (limit == null || limit.signum() <= 0) {
            return "Envie um valor maior que zero, por exemplo 3000.";
        }
        card.setCreditLimit(limit);
        accountRepository.save(card);
        return "Pronto! O limite do cartão agora é " + TextUtils.formatMoney(limit) + ".";
    }

    private Account findCard(User user) {
        return accountRepository.findByUserId(user.getId()).stream()
                .filter(account -> account.getType() == AccountType.CREDIT_CARD)
                .findFirst()
                .orElse(null);
    }
}

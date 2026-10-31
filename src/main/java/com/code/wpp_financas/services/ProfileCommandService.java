package com.code.wpp_financas.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.code.wpp_financas.models.Account;
import com.code.wpp_financas.models.User;
import com.code.wpp_financas.repositories.AccountRepository;
import com.code.wpp_financas.repositories.UserRepository;
import com.code.wpp_financas.utils.TextUtils;

@Service
public class ProfileCommandService {

    private static final String USAGE = """
      Use: !editar campo valor
      Exemplos:
      !editar nome Maria Silva
      !editar fechamento 12 inter
      !editar vencimento 19 inter
      !editar limite 3000 inter
      (o nome do cartão no final só é preciso se você tem mais de um)""";

    // o cartão escolhido, ou a mensagem de erro para responder
    private record CardChoice(Account card, String error) {
    }

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;

    public ProfileCommandService(
            UserRepository userRepository,
            AccountRepository accountRepository,
            AccountService accountService) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.accountService = accountService;
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

    // value = "12" ou "12 inter"
    private String editCardDay(User user, String value, boolean closing) {
        String[] parts = value.split("\\s+", 2);
        Integer day = TextUtils.parseDay(parts[0]);
        if (day == null) {
            return "Envie um número de 1 a 31.";
        }

        CardChoice choice = chooseCard(user, parts.length > 1 ? parts[1] : "", "!editar " + (closing ? "fechamento " : "vencimento ") + day);
        if (choice.error() != null) {
            return choice.error();
        }

        Account card = choice.card();
        if (closing) {
            card.setClosingDay(day);
        } else {
            card.setDueDay(day);
        }
        accountRepository.save(card);
        return closing
                ? "Pronto! A fatura do " + card.getName() + " agora fecha no dia " + day + "."
                : "Pronto! A fatura do " + card.getName() + " agora vence no dia " + day + ".";
    }

    // value = "3000" ou "3000 inter"
    private String editLimit(User user, String value) {
        String[] parts = value.split("\\s+", 2);
        BigDecimal limit = TextUtils.parseMoney(parts[0]);
        if (limit == null || limit.signum() <= 0) {
            return "Envie um valor maior que zero, por exemplo 3000.";
        }

        CardChoice choice = chooseCard(user, parts.length > 1 ? parts[1] : "", "!editar limite " + parts[0]);
        if (choice.error() != null) {
            return choice.error();
        }

        Account card = choice.card();
        card.setCreditLimit(limit);
        accountRepository.save(card);
        return "Pronto! O limite do cartão " + card.getName() + " agora é " + TextUtils.formatMoney(limit) + ".";
    }

    // sem nome: serve se houver um cartão só; com nome: procura entre os cartões
    private CardChoice chooseCard(User user, String nameArg, String commandSoFar) {
        List<Account> cards = accountService.cards(user.getId());
        if (cards.isEmpty()) {
            return new CardChoice(null, "Você não tem cartão cadastrado. Use !novocartao para cadastrar um.");
        }

        if (nameArg.isBlank()) {
            if (cards.size() == 1) {
                return new CardChoice(cards.get(0), null);
            }
            return new CardChoice(null, "Você tem mais de um cartão. Diga qual no final, por exemplo:\n"
                    + commandSoFar + " " + cards.get(0).getName() + "\n\nSeus cartões:\n" + accountService.menu(cards));
        }

        Optional<Account> chosen = accountService.pick(cards, nameArg);
        if (chosen.isEmpty()) {
            return new CardChoice(null, "Não encontrei o cartão \"" + nameArg + "\". Seus cartões:\n"
                    + accountService.menu(cards));
        }
        return new CardChoice(chosen.get(), null);
    }
}
package com.code.wpp_financas.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.code.wpp_financas.dtos.CreateAccountRequest;
import com.code.wpp_financas.models.Account;
import com.code.wpp_financas.models.AccountType;
import com.code.wpp_financas.models.User;
import com.code.wpp_financas.repositories.AccountRepository;
import com.code.wpp_financas.utils.TextUtils;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserService userService;

    public AccountService(AccountRepository accountRepository, UserService userService) {
        this.accountRepository = accountRepository;
        this.userService = userService;
    }

    public Account create(CreateAccountRequest request) {
        if (request.userId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId é obrigatório");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome da conta é obrigatório");
        }
        if (request.type() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo da conta é obrigatório");
        }

        User user = userService.getById(request.userId());

        if (request.type() != AccountType.EMERGENCY_FUND
                && nameTaken(user.getId(), request.type(), request.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Você já tem uma conta com esse nome.");
        }

        Account account = new Account();
        account.setUser(user);
        account.setName(request.name().trim());
        account.setType(request.type());
        account.setInitialBalance(
                request.initialBalance() != null ? request.initialBalance() : BigDecimal.ZERO);

        if (request.type() == AccountType.CREDIT_CARD) {
            validateDay(request.closingDay(), "Dia de fechamento");
            validateDay(request.dueDay(), "Dia de vencimento");
            account.setClosingDay(request.closingDay());
            account.setDueDay(request.dueDay());
            account.setCreditLimit(request.creditLimit());
        }
        // para as outras contas, closingDay, dueDay e creditLimit ficam nulos

        return accountRepository.save(account);
    }

    public List<Account> listByUser(Long userId) {
        userService.getById(userId); // dá 404 se o usuário não existir
        return accountRepository.findByUserId(userId);
    }

    public List<Account> ofType(Long userId, AccountType type) {
        return accountRepository.findByUserIdAndTypeOrderByIdAsc(userId, type);
    }

    public List<Account> banks(Long userId) {
        return ofType(userId, AccountType.CHECKING);
    }

    public List<Account> cards(Long userId) {
        return ofType(userId, AccountType.CREDIT_CARD);
    }

    // só devolve a conta se ela for do usuário
    public Optional<Account> findOwned(Long id, Long userId) {
        return accountRepository.findByIdAndUserId(id, userId);
    }

    // dois bancos (ou dois cartões) não podem ter o mesmo nome; banco e cartão podem
    public boolean nameTaken(Long userId, AccountType type, String name) {
        String wanted = TextUtils.normalize(name);
        return ofType(userId, type).stream()
                .anyMatch(account -> TextUtils.normalize(account.getName()).equals(wanted));
    }

    // escolhe uma conta da lista pelo número ("2") ou pelo nome ("nubank", ou só "nu" se for único)
    public Optional<Account> pick(List<Account> options, String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        String wanted = TextUtils.normalize(input);

        if (wanted.matches("\\d{1,3}")) {
            int index = Integer.parseInt(wanted) - 1;
            return index >= 0 && index < options.size()
                    ? Optional.of(options.get(index))
                    : Optional.empty();
        }

        List<Account> exact = options.stream()
                .filter(account -> TextUtils.normalize(account.getName()).equals(wanted))
                .toList();
        if (exact.size() == 1) {
            return Optional.of(exact.get(0));
        }

        List<Account> partial = options.stream()
                .filter(account -> TextUtils.normalize(account.getName()).contains(wanted))
                .toList();
        return partial.size() == 1 ? Optional.of(partial.get(0)) : Optional.empty();
    }

    // "1 - Nubank\n2 - Banco do Brasil"
    public String menu(List<Account> accounts) {
        StringBuilder menu = new StringBuilder();
        for (int i = 0; i < accounts.size(); i++) {
            menu.append(i + 1).append(" - ").append(accounts.get(i).getName()).append("\n");
        }
        return menu.toString().trim();
    }

    private void validateDay(Integer day, String field) {
        if (day == null || day < 1 || day > 31) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, field + " deve ser um número de 1 a 31");
        }
    }
}
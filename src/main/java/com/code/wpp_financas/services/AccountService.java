package com.code.wpp_financas.services;

import com.code.wpp_financas.dtos.CreateAccountRequest;
import com.code.wpp_financas.models.Account;
import com.code.wpp_financas.models.AccountType;
import com.code.wpp_financas.models.User;
import com.code.wpp_financas.repositories.AccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserService userService;

    public AccountService(AccountRepository accountRepository, UserService userService) {
        this.accountRepository = accountRepository;
        this.userService = userService;
    }

    public Account create(CreateAccountRequest request){
        if(request.userId() == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O userId é obrigatório");
        }
        if(request.name() == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O nome da conta é obrigatório");
        }
        if(request.type() == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O tipo da conta é obrigatório");
        }

        User user = userService.getById(request.userId());

        Account account = new Account();
        account.setUser(user);
        account.setName(request.name().trim());
        account.setType(request.type());
        account.setInitialBalance(request.initialBalance() != null ? request.initialBalance() : BigDecimal.ZERO);

        if(request.type() == AccountType.CREDIT_CARD){
            validateDay(request.closingDay(), "Dia de fechamento");
            validateDay(request.dueDay(), "Dia de vencimento");
            account.setClosingDay(request.closingDay());
            account.setDueDay(request.dueDay());
            account.setCreditLimit(request.creditLimit());
        }
        // para as outras contas, closingDay, dueDay e creditLimit devem ser nulos

        return accountRepository.save(account);
    }

    public List<Account> listByUser(Long userId){
        userService.getById(userId); // verifica se o usuário existe
        return accountRepository.findByUserId(userId);
    }

    private void validateDay(Integer day, String field){
        if(day == null || day < 1 || day > 31){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " deve ser de 1 a 31");
        }
    }
}

package com.code.wpp_financas.services;

import com.code.wpp_financas.models.Account;
import com.code.wpp_financas.models.AccountType;
import com.code.wpp_financas.models.TransactionType;
import com.code.wpp_financas.repositories.AccountRepository;
import com.code.wpp_financas.repositories.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class BalanceService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserService userService;

    public BalanceService(AccountRepository accountRepository, TransactionRepository transactionRepository, UserService userService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.userService = userService;
    }

    // saldo inicial + receitas - gastos de uma conta
    public BigDecimal accountBalance(Account account) {
        BigDecimal income = orZero(
                transactionRepository.sumByAccountAndType(account.getId(), TransactionType.INCOME));
        BigDecimal expense = orZero(
                transactionRepository.sumByAccountAndType(account.getId(), TransactionType.EXPENSE));
        return account.getInitialBalance().add(income).subtract(expense);
    }

    // o que o usuário pode gastar: soma das contas correntes (!saldo)
    public BigDecimal spendableBalance(Long userID){
        return totalByType(userID, AccountType.CHECKING);
    }

    // reserva de emergência (!reserva)
    public BigDecimal emergencyFund(Long userID){
        return totalByType(userID, AccountType.EMERGENCY_FUND);
    }

    private BigDecimal totalByType(Long userID, AccountType type){
        userService.getById(userID); // 404 se o usuário não existir]
        return accountRepository.findByUserId(userID).stream()
                .filter(account -> account.getType() == type)
                .map(this::accountBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

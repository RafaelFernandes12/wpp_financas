package com.code.wpp_financas.dtos;

import com.code.wpp_financas.models.Account;
import com.code.wpp_financas.models.AccountType;

import java.math.BigDecimal;

public record AccountResponse(
        Long id,
        Long userId,
        String name,
        AccountType type,
        BigDecimal initialBalance,
        BigDecimal creditLimit,
        Integer closingDay,
        Integer dueDay){

    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getUser().getId(),
                account.getName(),
                account.getType(),
                account.getInitialBalance(),
                account.getCreditLimit(),
                account.getClosingDay(),
                account.getDueDay());
    }
}

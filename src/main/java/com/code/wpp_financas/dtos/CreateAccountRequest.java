package com.code.wpp_financas.dtos;

import com.code.wpp_financas.models.AccountType;

import java.math.BigDecimal;

public record CreateAccountRequest(
    Long userId,
    String name,
    AccountType type,
    BigDecimal initialBalance,
    BigDecimal creditLimit,
    Integer closingDay,
    Integer dueDay){
}

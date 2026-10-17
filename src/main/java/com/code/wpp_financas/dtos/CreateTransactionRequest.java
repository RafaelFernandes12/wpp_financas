package com.code.wpp_financas.dtos;

import com.code.wpp_financas.models.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTransactionRequest(
        Long userId,
        Long accountId,
        TransactionType type,
        BigDecimal amount,
        String description,
        String categoryName,
        LocalDate date) {
}

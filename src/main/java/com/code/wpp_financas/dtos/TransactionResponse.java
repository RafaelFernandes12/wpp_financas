package com.code.wpp_financas.dtos;

import com.code.wpp_financas.models.Transaction;
import com.code.wpp_financas.models.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponse(
        Long id,
        Long AccountId,
        String categoryName,
        TransactionType type,
        BigDecimal amount,
        String description,
        LocalDate date) {

    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getAccount().getId(),
                t.getCategory() != null ? t.getCategory().getName() : null,
                t.getType(),
                t.getAmount(),
                t.getDescription(),
                t.getDate()
        );
    }
}

package com.code.wpp_financas.repositories;

import com.code.wpp_financas.models.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserIdAndDateBetween(Long userId, LocalDate start, LocalDate end);

    List<Transaction> findByAccountIdAndDateBetween(Long accountId, LocalDate start, LocalDate end);
}

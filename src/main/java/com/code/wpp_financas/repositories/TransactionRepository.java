package com.code.wpp_financas.repositories;

import com.code.wpp_financas.models.Transaction;
import com.code.wpp_financas.models.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserIdAndDateBetween(Long userId, LocalDate start, LocalDate end);

    List<Transaction> findByAccountIdAndDateBetween(Long accountId, LocalDate start, LocalDate end);

    @Query("select sum(t.amount) from Transaction t where t.account.id = :accountId and t.type = :type")
    BigDecimal sumByAccountAndType(@Param("accountId") Long accountId, @Param("type") TransactionType type);
}

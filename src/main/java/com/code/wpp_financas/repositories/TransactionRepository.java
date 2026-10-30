package com.code.wpp_financas.repositories;

import com.code.wpp_financas.dtos.CategoryTotal;
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

    // soma de tudo de um tipo numa conta (usada no saldo)
    @Query("select sum(t.amount) from Transaction t where t.account.id = :accountId and t.type = :type")
    BigDecimal sumByAccountAndType(@Param("accountId") Long accountId, @Param("type") TransactionType type);

    // soma numa conta, depois de "after" (exclusive) até "until" (inclusive): usada na fatura
    @Query("select sum(t.amount) from Transaction t where t.account.id = :accountId and t.type = :type " + "and t.date > :after and t.date <= :until")
    BigDecimal sumByAccountAndTypeInPeriod(
            @Param("accountId") Long accountId,
            @Param("type") TransactionType type,
            @Param("after") LocalDate after,
            @Param("until") LocalDate until);

    // soma de todas as contas do usuário num período (início e fim incluídos)
    @Query("select sum(t.amount) from Transaction t where t.user.id = :userId and t.type = :type" + "and t.date between :start and :end")
    BigDecimal sumByUserAndTypeBetween(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    // total por categoria, do maior para o menor; categoria nula vem com nome nulo
    @Query("select new com.code.wpp_financas.dtos.CategoryTotal(c.name, sum(t.amount)) "
            + "from Transaction t left join t.category c "
            + "where t.user.id = :userId and t.type = :type and t.date between :stard and :end "
            + "group by c.name order by sum(t.amount) desc")
    List<CategoryTotal> totalsByCategory(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

}
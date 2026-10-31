package com.code.wpp_financas.repositories;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.code.wpp_financas.dtos.CategoryTotal;
import com.code.wpp_financas.dtos.MonthTotal;
import com.code.wpp_financas.models.Transaction;
import com.code.wpp_financas.models.TransactionType;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserIdAndDateBetween(Long userId, LocalDate start, LocalDate end);

    List<Transaction> findByAccountIdAndDateBetween(Long accountId, LocalDate start, LocalDate end);

    // soma de tudo de um tipo numa conta (usada no saldo; inclui as transferências)
    @Query("select sum(t.amount) from Transaction t where t.account.id = :accountId and t.type = :type")
    BigDecimal sumByAccountAndType(@Param("accountId") Long accountId, @Param("type") TransactionType type);

    // soma numa conta, depois de "after" (exclusive) até "until" (inclusive): usada na fatura
    @Query("select sum(t.amount) from Transaction t where t.account.id = :accountId and t.type = :type "
            + "and t.date > :after and t.date <= :until")
    BigDecimal sumByAccountAndTypeInPeriod(
            @Param("accountId") Long accountId,
            @Param("type") TransactionType type,
            @Param("after") LocalDate after,
            @Param("until") LocalDate until);

    // soma numa conta, depois de "after" (exclusive): usada nos pagamentos de fatura
    @Query("select sum(t.amount) from Transaction t where t.account.id = :accountId and t.type = :type "
            + "and t.date > :after")
    BigDecimal sumByAccountAndTypeAfter(
            @Param("accountId") Long accountId,
            @Param("type") TransactionType type,
            @Param("after") LocalDate after);

    // soma de todas as contas do usuário num período, sem as transferências (relatório)
    @Query("select sum(t.amount) from Transaction t where t.user.id = :userId and t.type = :type "
            + "and t.transferId is null and t.date between :start and :end")
    BigDecimal sumByUserAndTypeBetween(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    // total por categoria, do maior para o menor, sem as transferências; categoria nula vem com nome nulo
    @Query("select new com.code.wpp_financas.dtos.CategoryTotal(c.name, sum(t.amount)) "
            + "from Transaction t left join t.category c "
            + "where t.user.id = :userId and t.type = :type and t.transferId is null "
            + "and t.date between :start and :end "
            + "group by c.name order by sum(t.amount) desc")
    List<CategoryTotal> totalsByCategory(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    // total por mês, sem as transferências (relatório anual)
    @Query("select new com.code.wpp_financas.dtos.MonthTotal(month(t.date), sum(t.amount)) "
            + "from Transaction t "
            + "where t.user.id = :userId and t.type = :type and t.transferId is null "
            + "and t.date between :start and :end "
            + "group by month(t.date) order by month(t.date)")
    List<MonthTotal> totalsByMonth(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    // quanto foi gasto numa categoria num período, sem as transferências (alerta de meta)
    @Query("select sum(t.amount) from Transaction t where t.user.id = :userId and t.category.id = :categoryId "
            + "and t.type = :type and t.transferId is null and t.date between :start and :end")
    BigDecimal sumByCategoryBetween(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("type") TransactionType type,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    // últimos lançamentos do usuário, do mais novo para o mais antigo (extrato e desfazer)
    @Query("select t from Transaction t join fetch t.account left join fetch t.category "
            + "where t.user.id = :userId order by t.id desc")
    List<Transaction> recentByUser(@Param("userId") Long userId, Pageable pageable);

    // últimos lançamentos de uma ou mais contas
    @Query("select t from Transaction t join fetch t.account left join fetch t.category "
            + "where t.account.id in :accountIds order by t.id desc")
    List<Transaction> recentByAccounts(@Param("accountIds") List<Long> accountIds, Pageable pageable);

    // as duas pontas de uma transferência
    @Query("select t from Transaction t join fetch t.account left join fetch t.category "
            + "where t.transferId = :transferId order by t.id")
    List<Transaction> legsOf(@Param("transferId") String transferId);

    // todas as parcelas de uma compra parcelada, da primeira para a última
    @Query("select t from Transaction t join fetch t.account left join fetch t.category "
            + "where t.installmentGroupId = :groupId order by t.id")
    List<Transaction> installmentsOf(@Param("groupId") String groupId);

    // um lançamento do usuário, já com a conta e a categoria carregadas
    @Query("select t from Transaction t join fetch t.account left join fetch t.category "
            + "where t.id = :id and t.user.id = :userId")
    Optional<Transaction> detailedById(@Param("id") Long id, @Param("userId") Long userId);
}
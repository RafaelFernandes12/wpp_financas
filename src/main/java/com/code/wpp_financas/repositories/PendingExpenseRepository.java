package com.code.wpp_financas.repositories;

import com.code.wpp_financas.models.PendingExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PendingExpenseRepository extends JpaRepository<PendingExpense, Long> {

    Optional<PendingExpense> findByPhone(String phone);
}

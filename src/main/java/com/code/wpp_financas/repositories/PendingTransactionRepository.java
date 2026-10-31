package com.code.wpp_financas.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.code.wpp_financas.models.PendingTransaction;

public interface PendingTransactionRepository extends JpaRepository<PendingTransaction, Long> {

    Optional<PendingTransaction> findByPhone(String phone);
}
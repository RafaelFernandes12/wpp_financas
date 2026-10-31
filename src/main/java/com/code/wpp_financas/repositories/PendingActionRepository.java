package com.code.wpp_financas.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.code.wpp_financas.models.PendingAction;

public interface PendingActionRepository extends JpaRepository<PendingAction, Long> {

    Optional<PendingAction> findByPhone(String phone);
}
package com.code.wpp_financas.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.code.wpp_financas.models.Account;
import com.code.wpp_financas.models.AccountType;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByUserId(Long userId);

    Optional<Account> findByIdAndUserId(Long id, Long userId);

    // sempre na mesma ordem (pelo id), para o número de cada conta nos menus não mudar
    List<Account> findByUserIdAndTypeOrderByIdAsc(Long userId, AccountType type);
}
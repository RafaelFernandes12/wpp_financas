package com.code.wpp_financas.repositories;

import com.code.wpp_financas.models.ConversationState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConversationStateRepository extends JpaRepository<ConversationState, Long> {
    Optional<ConversationState> findByPhone(String phone);
}

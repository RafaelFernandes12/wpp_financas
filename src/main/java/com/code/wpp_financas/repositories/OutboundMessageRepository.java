package com.code.wpp_financas.repositories;

import com.code.wpp_financas.models.OutboundMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboundMessageRepository extends JpaRepository<OutboundMessage, Long> {

    List<OutboundMessage> findTop20BySentAtIsNullOrderByIdAsc();

    boolean existsByDedupeKey(String dedupeKey);
}
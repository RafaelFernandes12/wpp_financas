package com.code.wpp_financas.services;

import com.code.wpp_financas.dtos.OutboundMessageResponse;
import com.code.wpp_financas.models.OutboundMessage;
import com.code.wpp_financas.repositories.OutboundMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboundMessageRepository repository;

    /** Retorna false se já existia uma mensagem com a mesma chave. */
    public boolean enqueue(String phone, String text, String dedupeKey) {
        if (dedupeKey != null && repository.existsByDedupeKey(dedupeKey)) {
            return false;
        }
        OutboundMessage message = new OutboundMessage();
        message.setPhone(phone);
        message.setText(text);
        message.setDedupeKey(dedupeKey);
        repository.save(message);
        return true;
    }

    public List<OutboundMessageResponse> pending() {
        return repository.findTop20BySentAtIsNullOrderByIdAsc().stream()
                .map(m -> new OutboundMessageResponse(m.getId(), m.getPhone(), m.getText()))
                .toList();
    }

    public void markSent(Long id) {
        repository.findById(id).ifPresent(message -> {
            if (message.getSentAt() == null) {
                message.setSentAt(Instant.now());
                repository.save(message);
            }
        });
    }
}
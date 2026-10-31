package com.code.wpp_financas.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "outbound_messages")
@Data
@NoArgsConstructor
public class OutboundMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    // evita enviar duas vezes a mesma coisa (ex.: "monthly-2026-10-3")
    @Column(unique = true)
    private String dedupeKey;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    // null = ainda não enviada
    private Instant sentAt;
}
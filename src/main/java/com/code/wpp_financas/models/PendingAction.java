package com.code.wpp_financas.models;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// ação (guardar, resgatar, pagar fatura, desfazer) esperando uma resposta do usuário
@Entity
@Table(name = "pending_actions")
@Getter
@Setter
@NoArgsConstructor
public class PendingAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActionKind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActionStep step;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    // nulos enquanto ainda não foram escolhidos
    private Long cardId;

    private Long bankId;

    // no !desfazer: o lançamento que será apagado se o usuário confirmar
    private Long targetId;

    @UpdateTimestamp
    private Instant updatedAt;
}
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

// gasto ou ganho já informado, esperando a escolha da categoria e/ou do banco ou cartão
@Entity
@Table(name = "pending_transactions")
@Getter
@Setter
@NoArgsConstructor
public class PendingTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PendingStep step;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    private String description;

    // nulos enquanto ainda não foram escolhidos
    private Long categoryId;

    private Long accountId;

    // o usuário escreveu "cartão" no fim do comando
    @Column(nullable = false)
    private boolean useCard;

    // número de parcelas; nulo significa compra à vista
    private Integer installments;

    @UpdateTimestamp
    private Instant updatedAt;
}
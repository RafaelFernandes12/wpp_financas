package com.code.wpp_financas.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    // pode ser nulo: gasto ainda sem categoria
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    // sempre positivo; quem diz se soma ou subtrai é o "type"
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    private String description;

    // dia do gasto: é ele que define a fatura e o mês do relatório
    @Column(nullable = false)
    private LocalDate date = LocalDate.now();

    // nulo nos lançamentos normais; nas transferências, as duas pontas têm o mesmo valor
    @Column(length = 36)
    private String transferId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
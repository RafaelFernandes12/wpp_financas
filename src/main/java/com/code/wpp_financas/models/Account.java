package com.code.wpp_financas.models;

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

import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType type;

    //Saldo inicial
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal initialBalance = BigDecimal.ZERO;

    //crédito
    @Column(precision = 19, scale = 2)
    private BigDecimal creditLimit;

    //dia da fatura fechar
    @Column
    private Integer closingDay;

    //dia da fatura vencer
    @Column
    private Integer dueDay;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}

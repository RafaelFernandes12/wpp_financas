package com.code.wpp_financas.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

// gasto que já foi informado, mas ainda espera a escolha da categoria
@Entity
@Table(name = "pending_expenses")
@Getter
@Setter
@NoArgsConstructor
public class PendingExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String phone;

    @Column(nullable = false)
    private Long accountID;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    private String description;

    @Column(nullable = false)
    private boolean useCard;

    @UpdateTimestamp
    private Instant updatedAt;
}
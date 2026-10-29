package com.code.wpp_financas.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "conversation_states")
@Getter
@Setter
@NoArgsConstructor
public class ConversationState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OnboardingStep step;

    // respostas coletadas até agora
    private String name;

    @Column(precision = 19, scale = 2)
    private BigDecimal checkingBalance;

    @Column(precision = 19, scale = 2)
    private BigDecimal emergencyFund;

    private Integer closingDay;

    private Integer dueDay;

    @UpdateTimestamp
    private Instant updatedAt;
}

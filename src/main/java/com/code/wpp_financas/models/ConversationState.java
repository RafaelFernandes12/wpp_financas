package com.code.wpp_financas.models;

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

// cadastro guiado em andamento: em que pergunta o telefone está
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

    // o usuário é criado assim que ele informa o nome; os bancos e cartões vão sendo criados a cada resposta
    private Long userId;

    // nome do banco ou cartão que está sendo cadastrado agora
    private String draftName;

    private Integer draftClosingDay;

    @UpdateTimestamp
    private Instant updatedAt;
}
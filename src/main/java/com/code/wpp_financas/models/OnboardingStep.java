package com.code.wpp_financas.models;

public enum OnboardingStep {
    NAME,       // perguntou o nome
    BALANCE,    // perguntou o saldo da conta corrente
    RESERVE,    // perguntou o valor da reserva
    HAS_CARD,   // perguntou se tem cartão
    CLOSING_DAY, // perguntou o dia de fechamento
    DUE_DAY     // perguntou o dia de vencimento
}

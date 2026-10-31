package com.code.wpp_financas.models;

public enum OnboardingStep {
    NAME,          // perguntou o nome
    BANK_NAME,     // perguntou o nome de um banco
    BANK_BALANCE,  // perguntou o saldo desse banco
    MORE_BANKS,    // perguntou se tem mais algum banco
    RESERVE,       // perguntou o valor da reserva
    HAS_CARD,      // perguntou se tem cartão
    CARD_NAME,     // perguntou o nome de um cartão
    CLOSING_DAY,   // perguntou o dia de fechamento
    DUE_DAY,       // perguntou o dia de vencimento
    MORE_CARDS     // perguntou se tem mais algum cartão
}
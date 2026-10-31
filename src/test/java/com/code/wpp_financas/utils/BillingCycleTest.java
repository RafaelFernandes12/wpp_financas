package com.code.wpp_financas.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class BillingCycleTest {

    @Test
    void antesDoFechamento_usaOFechamentoDoMesAtual() {
        var s = BillingCycle.openStatement(10, 17, LocalDate.of(2026, 10, 5));
        assertEquals(LocalDate.of(2026, 9, 10), s.previousClosing());
        assertEquals(LocalDate.of(2026, 10, 10), s.closing());
        assertEquals(LocalDate.of(2026, 10, 17), s.due());
    }

    @Test
    void noDiaDoFechamento_aindaEntraNaFaturaQueFecha() {
        var s = BillingCycle.openStatement(10, 17, LocalDate.of(2026, 10, 10));
        assertEquals(LocalDate.of(2026, 10, 10), s.closing());
    }

    @Test
    void depoisDoFechamento_vaiParaAProximaFatura() {
        var s = BillingCycle.openStatement(10, 17, LocalDate.of(2026, 10, 11));
        assertEquals(LocalDate.of(2026, 10, 10), s.previousClosing());
        assertEquals(LocalDate.of(2026, 11, 10), s.closing());
        assertEquals(LocalDate.of(2026, 11, 17), s.due());
    }

    @Test
    void vencimentoAntesDoFechamento_caiNoMesSeguinte() {
        var s = BillingCycle.openStatement(25, 5, LocalDate.of(2026, 10, 30));
        assertEquals(LocalDate.of(2026, 10, 25), s.previousClosing());
        assertEquals(LocalDate.of(2026, 11, 25), s.closing());
        assertEquals(LocalDate.of(2026, 12, 5), s.due());
    }

    @Test
    void dia31EmFevereiro_viraUltimoDiaDoMes() {
        var s = BillingCycle.openStatement(31, 7, LocalDate.of(2026, 2, 15));
        assertEquals(LocalDate.of(2026, 1, 31), s.previousClosing());
        assertEquals(LocalDate.of(2026, 2, 28), s.closing());
        assertEquals(LocalDate.of(2026, 3, 7), s.due());
    }

    @Test
    void faturaAnterior_eAQueFechouNoCicloAnterior() {
        var open = BillingCycle.openStatement(10, 17, LocalDate.of(2026, 10, 15));
        var previous = BillingCycle.previousStatement(open, 10, 17);
        assertEquals(LocalDate.of(2026, 10, 10), previous.closing());
        assertEquals(LocalDate.of(2026, 9, 10), previous.previousClosing());
        assertEquals(LocalDate.of(2026, 10, 17), previous.due());
    }
}
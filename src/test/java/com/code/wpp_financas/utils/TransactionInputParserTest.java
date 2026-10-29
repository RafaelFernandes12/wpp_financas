package com.code.wpp_financas.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class TransactionInputParserTest {

    @Test
    void somenteValor() {
        var parsed = TransactionInputParser.parse("25,90");
        assertEquals(0, new BigDecimal("25.90").compareTo(parsed.amount()));
        assertNull(parsed.description());
        assertNull(parsed.categoryName());
        assertFalse(parsed.useCard());
    }

    @Test
    void valorEDescricao() {
        var parsed = TransactionInputParser.parse("25,90 almoço");
        assertEquals(0, new BigDecimal("25.90").compareTo(parsed.amount()));
        assertEquals("almoço", parsed.description());
    }

    @Test
    void categoriaEmQualquerPosicao() {
        var parsed = TransactionInputParser.parse("40 #transporte uber para o centro");
        assertEquals("transporte", parsed.categoryName());
        assertEquals("uber para o centro", parsed.description());
    }

    @Test
    void cartaoNoFim() {
        var parsed = TransactionInputParser.parse("120 mercado cartão");
        assertTrue(parsed.useCard());
        assertEquals("mercado", parsed.description());
    }

    @Test
    void cartaoSemDescricao() {
        var parsed = TransactionInputParser.parse("120 cartão");
        assertTrue(parsed.useCard());
        assertNull(parsed.description());
    }

    @Test
    void cartaoNoMeioFazParteDaDescricao() {
        var parsed = TransactionInputParser.parse("50 cartão de presente");
        assertFalse(parsed.useCard());
        assertEquals("cartão de presente", parsed.description());
    }

    @Test
    void entradaInvalida() {
        assertNull(TransactionInputParser.parse("abc almoço"));
        assertNull(TransactionInputParser.parse("0 almoço"));
        assertNull(TransactionInputParser.parse(""));
    }
}
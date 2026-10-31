package com.code.wpp_financas.services;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class MonthlyReportJobTest {

    @Test
    void detectaUltimoDiaDoMes() {
        assertTrue(MonthlyReportJob.isLastDayOfMonth(LocalDate.of(2026, 10, 31)));
        assertTrue(MonthlyReportJob.isLastDayOfMonth(LocalDate.of(2026, 2, 28)));
        assertTrue(MonthlyReportJob.isLastDayOfMonth(LocalDate.of(2028, 2, 29)));
        assertTrue(MonthlyReportJob.isLastDayOfMonth(LocalDate.of(2026, 4, 30)));
    }

    @Test
    void naoEUltimoDia() {
        assertFalse(MonthlyReportJob.isLastDayOfMonth(LocalDate.of(2026, 10, 30)));
        assertFalse(MonthlyReportJob.isLastDayOfMonth(LocalDate.of(2028, 2, 28)));
        assertFalse(MonthlyReportJob.isLastDayOfMonth(LocalDate.of(2026, 1, 1)));
    }
}
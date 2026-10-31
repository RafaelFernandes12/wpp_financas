package com.code.wpp_financas.utils;

import java.time.LocalDate;
import java.time.YearMonth;

public class BillingCycle {

    // compras com data > previousClosing e <= closing pertencem a esta fatura
    public record Statement(LocalDate previousClosing, LocalDate closing, LocalDate due){
    }

    private BillingCycle(){
    }

    // a fatura aberta é a que fecha no próximo fechamento (hoje incluido)
    public static Statement openStatement(int closingDay, int dueDay, LocalDate today){
        YearMonth month = YearMonth.from(today);
        LocalDate closing = dateIn(month, closingDay);
        if(today.isAfter(closing)){
            closing = dateIn(month.plusMonths(1), closingDay);
        }
        return build(closing, closingDay, dueDay);
    }

    // a fatura imediatamente anterior à fatura aberta
    public static Statement previousStatement(Statement open, int closingDay, int dueDay){
        return build(open.previousClosing(), closingDay, dueDay);
    }

    private static Statement build(LocalDate closing, int closingDay, int dueDay){
        YearMonth closingMonth = YearMonth.from(closing);
        LocalDate previusClosing = dateIn(closingMonth.minusMonths(1), closingDay);
        YearMonth dueMonth = dueDay > closingDay ? closingMonth : closingMonth.plusMonths(1);
        return new Statement(previusClosing, closing, dateIn(dueMonth, dueDay));
    }

    // dia 31 em fevereiro vira dia 28(ou 29)
    private static LocalDate dateIn(YearMonth month, int day){
        return month.atDay(Math.min(day, month.lengthOfMonth()));
    }
}

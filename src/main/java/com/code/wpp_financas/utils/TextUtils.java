package com.code.wpp_financas.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.Normalizer;
import java.util.Locale;

public final class TextUtils {

    private TextUtils() {
    }

    // "!Relatório " vira "!relatorio": sem acento, minúsculo, sem espaços nas pontas
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(text.trim(), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    public static boolean isYes(String text) {
        String t = normalize(text);
        return t.equals("sim") || t.equals("s");
    }

    public static boolean isNo(String text) {
        String t = normalize(text);
        return t.equals("nao") || t.equals("n");
    }

    // aceita "1500", "1500,50", "1.500,50", "R$ 1500.50"; devolve null se não entender
    public static BigDecimal parseMoney(String text) {
        if (text == null) {
            return null;
        }
        String s = text.toLowerCase(Locale.ROOT).replace("r$", "").replace(" ", "");
        if (s.isEmpty()) {
            return null;
        }
        if (s.contains(",")) {
            s = s.replace(".", "").replace(",", ".");
        }
        try {
            return new BigDecimal(s).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // dia do mês de 1 a 31; devolve null se inválido
    public static Integer parseDay(String text) {
        try {
            int day = Integer.parseInt(text.trim());
            return day >= 1 && day <= 31 ? day : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // 1974.10 vira "R$ 1.974,10"
    public static String formatMoney(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(Locale.of("pt", "BR")).format(value);
    }
}
package com.code.wpp_financas.utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class TransactionInputParser {

    // "3x", "10x", "2X"
    private static final Pattern INSTALLMENTS = Pattern.compile("(?i)^(\\d{1,2})x$");

    // installments vale 1 para compra à vista
    public record Parsed(
            BigDecimal amount, String description, String categoryName, boolean useCard, int installments) {
    }

    private TransactionInputParser() {
    }

    // devolve null se o texto não começar com um valor válido
    public static Parsed parse(String args) {
        if (args == null || args.isBlank()) {
            return null;
        }

        String[] tokens = args.trim().split("\\s+");

        BigDecimal amount = TextUtils.parseMoney(tokens[0]);
        if (amount == null || amount.signum() <= 0) {
            return null;
        }

        String category = null;
        int installments = 1;
        boolean installmentsFound = false;
        List<String> words = new ArrayList<>();

        for (int i = 1; i < tokens.length; i++) {
            String token = tokens[i];
            var matcher = INSTALLMENTS.matcher(token);

            if (token.startsWith("#") && token.length() > 1) {
                if (category == null) {
                    category = token.substring(1);
                }
            } else if (!installmentsFound && matcher.matches()) {
                installments = Math.max(1, Integer.parseInt(matcher.group(1)));
                installmentsFound = true;
            } else {
                words.add(token);
            }
        }

        // "cartão" só vale como última palavra
        boolean useCard = false;
        if (!words.isEmpty() && TextUtils.normalize(words.get(words.size() - 1)).equals("cartao")) {
            useCard = true;
            words.remove(words.size() - 1);
        }

        String description = words.isEmpty() ? null : String.join(" ", words);
        return new Parsed(amount, description, category, useCard, installments);
    }
}
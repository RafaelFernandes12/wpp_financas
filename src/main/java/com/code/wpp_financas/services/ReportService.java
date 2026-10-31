package com.code.wpp_financas.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.code.wpp_financas.dtos.CategoryTotal;
import com.code.wpp_financas.dtos.MonthTotal;
import com.code.wpp_financas.models.TransactionType;
import com.code.wpp_financas.models.User;
import com.code.wpp_financas.repositories.TransactionRepository;
import com.code.wpp_financas.utils.TextUtils;

@Service
public class ReportService {

    private static final ZoneId ZONE = ZoneId.of("America/Fortaleza");
    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final String ANNUAL_USAGE = "Não entendi o ano. Use !relatorio ano (ano atual) ou !relatorio ano 2025.";

    private final TransactionRepository transactionRepository;
    private final BudgetService budgetService;

    public ReportService(TransactionRepository transactionRepository, BudgetService budgetService) {
        this.transactionRepository = transactionRepository;
        this.budgetService = budgetService;
    }

    // !relatorio [mês] ou !relatorio ano [ano]
    public String report(User user, String args) {
        String arg = args == null ? "" : args.trim();
        String[] parts = arg.split("\\s+", 2);
        if (TextUtils.normalize(parts[0]).equals("ano")) {
            return annualReport(user, parts.length > 1 ? parts[1] : "");
        }
        return monthlyReport(user, arg);
    }

    private String monthlyReport(User user, String args) {
        YearMonth month = parseMonth(args);
        if (month == null) {
            return "Não entendi o mês. Use !relatorio (mês atual), !relatorio 9, !relatorio 9/2026 ou !relatorio ano.";
        }

        List<CategoryTotal> totals = transactionRepository.totalsByCategory(
                user.getId(), TransactionType.EXPENSE, month.atDay(1), month.atEndOfMonth());

        BigDecimal expenses = sumOf(totals);
        BigDecimal income = incomeBetween(user, month.atDay(1), month.atEndOfMonth());

        String title = month.getMonth().getDisplayName(TextStyle.FULL, PT_BR) + "/" + month.getYear();

        if (totals.isEmpty() && income.signum() == 0) {
            return "Nenhuma movimentação em " + title + ".";
        }

        Map<String, BigDecimal> goals = budgetService.limitsByCategoryName(user.getId());
        StringBuilder report = new StringBuilder("*Relatório de " + title + "*\n\n");

        if (!totals.isEmpty()) {
            report.append("*Gastos por categoria*\n");
            for (CategoryTotal item : totals) {
                String name = item.categoryName() != null ? item.categoryName() : "Sem categoria";
                report.append(name).append(": ").append(TextUtils.formatMoney(item.total()))
                        .append(" (").append(percent(item.total(), expenses)).append("%)");
                BigDecimal goal = item.categoryName() != null ? goals.get(item.categoryName()) : null;
                if (goal != null) {
                    report.append(" - meta ").append(TextUtils.formatMoney(goal));
                }
                report.append("\n");
            }
            report.append("\n");
        }

        report.append("Total de gastos: ").append(TextUtils.formatMoney(expenses)).append("\n");
        report.append("Ganhos: ").append(TextUtils.formatMoney(income)).append("\n");
        report.append("Resultado do mês: ").append(TextUtils.formatMoney(income.subtract(expenses)));
        report.append("\n\nOs gastos no cartão entram pela data da compra (compras parceladas, pela data de cada parcela).");

        return report.toString();
    }

    private String annualReport(User user, String yearArg) {
        int year = YearMonth.now(ZONE).getYear();
        String arg = yearArg.trim();
        if (!arg.isEmpty()) {
            try {
                year = Integer.parseInt(arg);
            } catch (NumberFormatException e) {
                return ANNUAL_USAGE;
            }
            if (year < 2000 || year > 2100) {
                return ANNUAL_USAGE;
            }
        }

        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);

        List<CategoryTotal> totals = transactionRepository.totalsByCategory(
                user.getId(), TransactionType.EXPENSE, start, end);
        BigDecimal expenses = sumOf(totals);
        BigDecimal income = incomeBetween(user, start, end);

        if (totals.isEmpty() && income.signum() == 0) {
            return "Nenhuma movimentação em " + year + ".";
        }

        StringBuilder report = new StringBuilder("*Relatório de " + year + "*\n\n");

        if (!totals.isEmpty()) {
            report.append("*Gastos por categoria*\n");
            for (CategoryTotal item : totals) {
                String name = item.categoryName() != null ? item.categoryName() : "Sem categoria";
                report.append(name).append(": ").append(TextUtils.formatMoney(item.total()))
                        .append(" (").append(percent(item.total(), expenses)).append("%)\n");
            }
            report.append("\n");
        }

        Map<Integer, BigDecimal> expenseByMonth = byMonth(user, TransactionType.EXPENSE, start, end);
        Map<Integer, BigDecimal> incomeByMonth = byMonth(user, TransactionType.INCOME, start, end);

        report.append("*Mês a mês*\n");
        for (int m = 1; m <= 12; m++) {
            if (!expenseByMonth.containsKey(m) && !incomeByMonth.containsKey(m)) {
                continue;
            }
            report.append(Month.of(m).getDisplayName(TextStyle.SHORT, PT_BR)).append(": gastos ")
                    .append(TextUtils.formatMoney(expenseByMonth.getOrDefault(m, BigDecimal.ZERO)))
                    .append(" · ganhos ")
                    .append(TextUtils.formatMoney(incomeByMonth.getOrDefault(m, BigDecimal.ZERO)))
                    .append("\n");
        }

        report.append("\nTotal de gastos: ").append(TextUtils.formatMoney(expenses)).append("\n");
        report.append("Ganhos: ").append(TextUtils.formatMoney(income)).append("\n");
        report.append("Resultado do ano: ").append(TextUtils.formatMoney(income.subtract(expenses)));
        report.append("\n\nOs gastos no cartão entram pela data da compra (compras parceladas, pela data de cada parcela).");

        return report.toString();
    }

    private BigDecimal sumOf(List<CategoryTotal> totals) {
        return totals.stream().map(CategoryTotal::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal incomeBetween(User user, LocalDate start, LocalDate end) {
        BigDecimal income = transactionRepository.sumByUserAndTypeBetween(
                user.getId(), TransactionType.INCOME, start, end);
        return income != null ? income : BigDecimal.ZERO;
    }

    private Map<Integer, BigDecimal> byMonth(User user, TransactionType type, LocalDate start, LocalDate end) {
        return transactionRepository.totalsByMonth(user.getId(), type, start, end).stream()
                .collect(Collectors.toMap(MonthTotal::month, MonthTotal::total));
    }

    // "total" nunca é zero aqui: só é chamado quando há gastos
    private BigDecimal percent(BigDecimal part, BigDecimal total) {
        return part.multiply(BigDecimal.valueOf(100)).divide(total, 0, RoundingMode.HALF_UP);
    }

    // vazio = mês atual; "9" = setembro deste ano; "9/2026" = setembro de 2026; inválido = null
    private YearMonth parseMonth(String args) {
        if (args == null || args.isBlank()) {
            return YearMonth.now(ZONE);
        }
        String[] parts = args.trim().split("/");
        if (parts.length > 2) {
            return null;
        }
        try {
            int month = Integer.parseInt(parts[0].trim());
            int year = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : YearMonth.now(ZONE).getYear();
            if (month < 1 || month > 12 || year < 2000 || year > 2100) {
                return null;
            }
            return YearMonth.of(year, month);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
package com.code.wpp_financas.services;

import com.code.wpp_financas.dtos.CategoryTotal;
import com.code.wpp_financas.models.TransactionType;
import com.code.wpp_financas.models.User;
import com.code.wpp_financas.repositories.TransactionRepository;
import com.code.wpp_financas.utils.TextUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

@Service
public class ReportService {

    private static final ZoneId ZONE = ZoneId.of("America/Fortaleza");
    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final TransactionRepository transactionRepository;

    public ReportService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public String monthlyReport(User user, String args) {
        YearMonth month = parseMonth(args);
        if(month == null) {
            return "Não entendi o mês.\nUse !relatorio (mês atual), !relatorio 9 ou !relatorio 9/2026";
        }

        List<CategoryTotal> totals = transactionRepository.totalsByCategory(user.getId(), TransactionType.EXPENSE, month.atDay(1), month.atEndOfMonth());

        BigDecimal expenses = totals.stream().map(CategoryTotal::total).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal income = transactionRepository.sumByUserAndTypeBetween(user.getId(), TransactionType.INCOME, month.atDay(1), month.atEndOfMonth());
        if (income == null) {
            income = BigDecimal.ZERO;
        }

        String title = month.getMonth().getDisplayName(TextStyle.FULL, PT_BR) + "/" + month.getYear();

        if(totals.isEmpty() && income.signum() == 0){
            return "Nenhuma movimentação em " + title + ".";
        }

        StringBuilder report = new StringBuilder("Relatório de " + title + "\n\n");

        if (!totals.isEmpty()) {
            report.append("*Gastos por categoria*\n");
            for (CategoryTotal item : totals) {
                String name = item.categoryName() != null ? item.categoryName() : "Sem categoria";
                BigDecimal percent = item.total().multiply(BigDecimal.valueOf(100)).divide(expenses, 0, RoundingMode.HALF_UP);
                report.append(name).append(": ").append(TextUtils.formatMoney(item.total())).append(" (").append(percent).append("%)\n");
            }
            report.append("\n");
        }

        report.append("Total de gastos: ").append(TextUtils.formatMoney(expenses)).append("\n");
        report.append("Receitas ").append(TextUtils.formatMoney(income)).append("\n");
        report.append("Resultados do mês: ").append(TextUtils.formatMoney(income.subtract(expenses))).append("\n");
        report.append("\n\nOs gastos no cartão entram pela data da compra.");

        return report.toString();
    }

    // vazio = mês atual; "9" = setembro deste ano; "9/2026" = setembro de 2026;  inválido = null;
    private YearMonth parseMonth(String args) {
        if(args == null || args.isBlank()){
            return YearMonth.now(ZONE);
        }
        String[] parts = args.trim().split("/");
        if (parts.length > 2){
            return null;
        }
        try{
            int month = Integer.parseInt(parts[0].trim());
            int year = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : YearMonth.now(ZONE).getYear();
            if(month < 1 || month > 12 || year < 2000 || year > 2030){
                return null;
            }
            return YearMonth.of(year, month);
        } catch (NumberFormatException e){
            return null;
        }
    }
}

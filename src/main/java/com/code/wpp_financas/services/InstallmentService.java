package com.code.wpp_financas.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.code.wpp_financas.models.Account;
import com.code.wpp_financas.models.Category;
import com.code.wpp_financas.models.Transaction;
import com.code.wpp_financas.models.TransactionType;
import com.code.wpp_financas.models.User;
import com.code.wpp_financas.repositories.TransactionRepository;

@Service
public class InstallmentService {

    private static final ZoneId ZONE = ZoneId.of("America/Fortaleza");

    private final TransactionRepository transactionRepository;

    public InstallmentService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    // uma parcela por mês, cada uma com a data do seu mês; todas ligadas pelo mesmo installmentGroupId
    @Transactional
    public void create(
            User user, Account card, Category category, BigDecimal total, int count, String description) {
        String groupId = UUID.randomUUID().toString();
        LocalDate firstDate = LocalDate.now(ZONE);
        List<BigDecimal> amounts = split(total, count);
        String base = description != null && !description.isBlank() ? description : "Compra";

        for (int i = 0; i < count; i++) {
            Transaction installment = new Transaction();
            installment.setUser(user);
            installment.setAccount(card);
            installment.setCategory(category);
            installment.setType(TransactionType.EXPENSE);
            installment.setAmount(amounts.get(i));
            installment.setDescription(base + " " + (i + 1) + "/" + count);
            installment.setDate(firstDate.plusMonths(i));
            installment.setInstallmentGroupId(groupId);
            transactionRepository.save(installment);
        }
    }

    // divide o total em "count" parcelas; a primeira leva os centavos que sobrarem
    static List<BigDecimal> split(BigDecimal total, int count) {
        BigDecimal divisor = BigDecimal.valueOf(count);
        BigDecimal base = total.divide(divisor, 2, RoundingMode.DOWN);
        BigDecimal remainder = total.subtract(base.multiply(divisor));

        List<BigDecimal> amounts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            amounts.add(i == 0 ? base.add(remainder) : base);
        }
        return amounts;
    }
}
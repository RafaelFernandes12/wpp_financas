package com.code.wpp_financas.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.code.wpp_financas.models.Category;
import com.code.wpp_financas.models.CategoryGoal;
import com.code.wpp_financas.models.TransactionType;
import com.code.wpp_financas.models.User;
import com.code.wpp_financas.repositories.CategoryGoalRepository;
import com.code.wpp_financas.repositories.TransactionRepository;
import com.code.wpp_financas.utils.TextUtils;

@Service
public class BudgetService {

    private static final ZoneId ZONE = ZoneId.of("America/Fortaleza");
    // a partir de 80% da meta, o bot começa a avisar
    private static final BigDecimal WARN_RATIO = new BigDecimal("0.80");

    private final CategoryGoalRepository goalRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;

    public BudgetService(
            CategoryGoalRepository goalRepository,
            TransactionRepository transactionRepository,
            CategoryService categoryService) {
        this.goalRepository = goalRepository;
        this.transactionRepository = transactionRepository;
        this.categoryService = categoryService;
    }

    // !meta alimentação 300 | !meta alimentação 0 (remove) | !meta (lista)
    public String setGoal(User user, String args) {
        String usage = "Use: !meta categoria valor\nExemplos:\n!meta alimentação 300\n"
                + "!meta alimentação 0 (remove a meta)\n!metas (mostra as suas metas)";

        String[] tokens = args == null || args.isBlank() ? new String[0] : args.trim().split("\\s+");
        if (tokens.length == 0) {
            return list(user);
        }
        if (tokens.length < 2) {
            return usage;
        }

        BigDecimal limit = TextUtils.parseMoney(tokens[tokens.length - 1]);
        if (limit == null || limit.signum() < 0) {
            return usage;
        }

        String categoryInput = String.join(" ", Arrays.copyOfRange(tokens, 0, tokens.length - 1));
        Optional<Category> category = categoryService.findByInput(categoryInput);
        if (category.isEmpty()) {
            return "Não conheço a categoria \"" + categoryInput + "\". Categorias:\n" + categoryService.menu();
        }

        String name = category.get().getName();
        Optional<CategoryGoal> existing =
                goalRepository.findByUserIdAndCategoryId(user.getId(), category.get().getId());

        // valor 0 remove a meta
        if (limit.signum() == 0) {
            if (existing.isEmpty()) {
                return "Você não tinha meta para " + name + ".";
            }
            goalRepository.delete(existing.get());
            return "Meta de " + name + " removida.";
        }

        CategoryGoal goal = existing.orElseGet(CategoryGoal::new);
        goal.setUser(user);
        goal.setCategory(category.get());
        goal.setMonthlyLimit(limit);
        goalRepository.save(goal);

        return "Meta de " + name + ": " + TextUtils.formatMoney(limit) + " por mês.\n"
                + "Neste mês você já gastou " + TextUtils.formatMoney(spentThisMonth(user, category.get()))
                + " nessa categoria.";
    }

    // !metas: cada meta com o quanto já foi gasto neste mês
    public String list(User user) {
        List<CategoryGoal> goals = goalRepository.findAllByUser(user.getId());
        if (goals.isEmpty()) {
            return "Você ainda não definiu metas. Use !meta categoria valor, por exemplo: !meta alimentação 300";
        }

        StringBuilder text = new StringBuilder("*Metas do mês*\n");
        for (CategoryGoal goal : goals) {
            BigDecimal limit = goal.getMonthlyLimit();
            BigDecimal spent = spentThisMonth(user, goal.getCategory());
            BigDecimal percent = spent.multiply(BigDecimal.valueOf(100)).divide(limit, 0, RoundingMode.HALF_UP);

            text.append(goal.getCategory().getName()).append(": ")
                    .append(TextUtils.formatMoney(spent)).append(" de ").append(TextUtils.formatMoney(limit))
                    .append(" (").append(percent).append("%)");

            int comparison = spent.compareTo(limit);
            if (comparison > 0) {
                text.append(" - estourada em ").append(TextUtils.formatMoney(spent.subtract(limit)));
            } else if (comparison == 0) {
                text.append(" - atingida");
            } else {
                text.append(" - faltam ").append(TextUtils.formatMoney(limit.subtract(spent)));
            }
            text.append("\n");
        }
        return text.toString().trim();
    }

    // chamado depois de um gasto: devolve o aviso, se a categoria tem meta e já está perto ou acima
    public Optional<String> alertFor(User user, Category category) {
        Optional<CategoryGoal> goal = goalRepository.findByUserIdAndCategoryId(user.getId(), category.getId());
        if (goal.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal limit = goal.get().getMonthlyLimit();
        BigDecimal spent = spentThisMonth(user, category);
        String name = category.getName();
        int comparison = spent.compareTo(limit);

        if (comparison > 0) {
            return Optional.of("LIMITE ESTOURADO! Neste mês você já gastou " + TextUtils.formatMoney(spent)
                    + " em " + name + ", " + TextUtils.formatMoney(spent.subtract(limit))
                    + " acima da sua meta de " + TextUtils.formatMoney(limit) + ".");
        }
        if (comparison == 0) {
            return Optional.of("META ATINGIDA! Você bateu o limite mensal de " + name
                    + " (" + TextUtils.formatMoney(limit) + ").");
        }
        if (spent.compareTo(limit.multiply(WARN_RATIO)) >= 0) {
            return Optional.of("CUIDADO! Faltam " + TextUtils.formatMoney(limit.subtract(spent))
                    + " para bater a sua meta mensal de " + name + " (" + TextUtils.formatMoney(limit) + ").");
        }
        return Optional.empty();
    }

    // nome da categoria -> meta mensal (usado no relatório)
    public Map<String, BigDecimal> limitsByCategoryName(Long userId) {
        return goalRepository.findAllByUser(userId).stream()
                .collect(Collectors.toMap(goal -> goal.getCategory().getName(), CategoryGoal::getMonthlyLimit));
    }

    private BigDecimal spentThisMonth(User user, Category category) {
        YearMonth month = YearMonth.now(ZONE);
        BigDecimal sum = transactionRepository.sumByCategoryBetween(
                user.getId(), category.getId(), TransactionType.EXPENSE, month.atDay(1), month.atEndOfMonth());
        return sum != null ? sum : BigDecimal.ZERO;
    }
}
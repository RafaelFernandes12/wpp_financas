package com.code.wpp_financas.services;

import com.code.wpp_financas.models.User;
import com.code.wpp_financas.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonthlyReportJob {

    private static final ZoneId ZONE = ZoneId.of("America/Fortaleza");

    private final UserRepository userRepository;
    private final ReportService reportService;
    private final OutboxService outboxService;

    // todo dia às 20h; só age se hoje for o último dia do mês
    @Scheduled(cron = "0 0 20 * * *", zone = "America/Fortaleza")
    public void run() {
        LocalDate today = LocalDate.now(ZONE);
        if (isLastDayOfMonth(today)) {
            int sent = sendReports();
            log.info("Relatório mensal enfileirado para {} usuário(s)", sent);
        }
    }

    /** Enfileira o relatório do mês atual de todos os usuários. Retorna quantos entraram na fila. */
    public int sendReports() {
        YearMonth month = YearMonth.now(ZONE);
        String label = month.getMonth().getDisplayName(TextStyle.FULL, Locale.of("pt", "BR"))
                + "/" + month.getYear();

        int queued = 0;
        for (User user : userRepository.findAll()) {
            if (user.getPhone() == null || user.getPhone().isBlank()) {
                continue;
            }
            try {
                String key = "monthly-" + month + "-" + user.getId();
                String body = reportService.report(user, "");
                String text = "📅 Fechamos " + label + "! Aqui está o seu relatório:\n\n" + body;
                if (outboxService.enqueue(user.getPhone(), text, key)) {
                    queued++;
                }
            } catch (Exception e) {
                log.error("Falha ao gerar relatório do usuário {}", user.getId(), e);
            }
        }
        return queued;
    }

    static boolean isLastDayOfMonth(LocalDate date) {
        return date.getDayOfMonth() == date.lengthOfMonth();
    }
}
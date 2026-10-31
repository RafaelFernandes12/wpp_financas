package com.code.wpp_financas.controllers;

import com.code.wpp_financas.dtos.OutboundMessageResponse;
import com.code.wpp_financas.services.MonthlyReportJob;
import com.code.wpp_financas.services.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/outbox")
@RequiredArgsConstructor
public class OutboxController {

    private final OutboxService outboxService;
    private final MonthlyReportJob monthlyReportJob;

    @GetMapping("/pending")
    public List<OutboundMessageResponse> pending() {
        return outboxService.pending();
    }

    @PostMapping("/{id}/sent")
    public ResponseEntity<Void> sent(@PathVariable Long id) {
        outboxService.markSent(id);
        return ResponseEntity.noContent().build();
    }

    // SÓ PARA TESTE: dispara o relatório mensal agora
    @PostMapping("/monthly-reports/run")
    public Map<String, Integer> runMonthlyReports() {
        return Map.of("queued", monthlyReportJob.sendReports());
    }
}
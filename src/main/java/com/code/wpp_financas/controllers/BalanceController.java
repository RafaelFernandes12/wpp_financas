package com.code.wpp_financas.controllers;

import com.code.wpp_financas.dtos.BalanceResponse;
import com.code.wpp_financas.services.BalanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/balance")
public class BalanceController {

    private final BalanceService balanceService;

    public BalanceController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @GetMapping
    public BalanceResponse spendable(@RequestParam Long userId){
        return new BalanceResponse(balanceService.spendableBalance(userId));
    }

    @GetMapping("/emergency-fund")
    public BalanceResponse emergencyFund(@RequestParam Long userId){
        return new BalanceResponse(balanceService.emergencyFund(userId));
    }
}

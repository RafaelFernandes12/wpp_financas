package com.code.wpp_financas.controllers;

import com.code.wpp_financas.dtos.CreateTransactionRequest;
import com.code.wpp_financas.dtos.TransactionResponse;
import com.code.wpp_financas.services.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@RequestBody CreateTransactionRequest request) {
        return TransactionResponse.from(transactionService.create(request));
    }
}

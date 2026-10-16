package com.code.wpp_financas.controllers;

import com.code.wpp_financas.dtos.AccountResponse;
import com.code.wpp_financas.dtos.CreateAccountRequest;
import com.code.wpp_financas.services.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@RequestBody CreateAccountRequest request){
        return AccountResponse.from(accountService.create(request));
    }

    @GetMapping
    public List<AccountResponse> listByUser(@RequestParam Long userId){
        return accountService.listByUser(userId).stream()
                .map(AccountResponse::from)
                .toList();
    }

}

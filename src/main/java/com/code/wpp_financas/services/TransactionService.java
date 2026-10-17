package com.code.wpp_financas.services;

import com.code.wpp_financas.dtos.CreateTransactionRequest;
import com.code.wpp_financas.models.Account;
import com.code.wpp_financas.models.AccountType;
import com.code.wpp_financas.models.Category;
import com.code.wpp_financas.models.Transaction;
import com.code.wpp_financas.models.TransactionType;
import com.code.wpp_financas.models.User;
import com.code.wpp_financas.repositories.AccountRepository;
import com.code.wpp_financas.repositories.CategoryRepository;
import com.code.wpp_financas.repositories.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final UserService userService;

    public TransactionService(TransactionRepository transactionRepository, AccountRepository accountRepository, CategoryRepository categoryRepository, UserService userService) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.userService = userService;
    }

    public Transaction create(CreateTransactionRequest request) {
        if (request.userId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId é obrigatórioo");
        }
        if (request.accountId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "accountId é obrigatório");
        }
        if (request.type() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type é obrigatório");
        }
        if (request.amount() == null || request.amount().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "valor deve ser maior que zero");
        }

        User user = userService.getById(request.userId());

        Account account = accountRepository.findByIdAndUserId(request.accountId(), user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada para o usuário"));

        if(account.getType() == AccountType.CREDIT_CARD && request.type() == TransactionType.INCOME){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cartão de crédito não recebe receita");
        }

        Category category = null;
        if (request.categoryName() != null && !request.categoryName().isBlank()) {
            category = categoryRepository.findByNameIgnoreCase(request.categoryName().trim())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categoria não encontrada" + request.categoryName()));
        }

        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setType(request.type());
        transaction.setAmount(request.amount());
        transaction.setDescription(request.description());
        transaction.setDate(request.date() != null ? request.date() : LocalDate.now());

        return transactionRepository.save(transaction);
    }
}

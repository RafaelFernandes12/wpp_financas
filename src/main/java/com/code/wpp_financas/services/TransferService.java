package com.code.wpp_financas.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.code.wpp_financas.models.Account;
import com.code.wpp_financas.models.Transaction;
import com.code.wpp_financas.models.TransactionType;
import com.code.wpp_financas.models.User;
import com.code.wpp_financas.repositories.TransactionRepository;

@Service
public class TransferService {

    private static final ZoneId ZONE = ZoneId.of("America/Fortaleza");

    private final TransactionRepository transactionRepository;

    public TransferService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    // dinheiro que sai de "from" e entra em "to": duas linhas ligadas pelo mesmo transferId
    @Transactional
    public void transfer(User user, Account from, Account to, BigDecimal amount, String description) {
        String transferId = UUID.randomUUID().toString();
        LocalDate today = LocalDate.now(ZONE);
        transactionRepository.save(leg(user, from, TransactionType.EXPENSE, amount, description, transferId, today));
        transactionRepository.save(leg(user, to, TransactionType.INCOME, amount, description, transferId, today));
    }

    // apaga o lançamento; transferência apaga as duas pontas, compra parcelada apaga todas as parcelas
    @Transactional
    public void delete(Transaction transaction) {
        if (transaction.getTransferId() != null) {
            transactionRepository.deleteAll(transactionRepository.legsOf(transaction.getTransferId()));
            return;
        }
        if (transaction.getInstallmentGroupId() != null) {
            transactionRepository.deleteAll(
                    transactionRepository.installmentsOf(transaction.getInstallmentGroupId()));
            return;
        }
        transactionRepository.delete(transaction);
    }

    private Transaction leg(
            User user, Account account, TransactionType type, BigDecimal amount,
            String description, String transferId, LocalDate date) {
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setAccount(account);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setTransferId(transferId);
        transaction.setDate(date);
        return transaction;
    }
}
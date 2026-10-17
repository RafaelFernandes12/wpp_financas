package com.code.wpp_financas.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.code.wpp_financas.models.Account;
import com.code.wpp_financas.models.AccountType;
import com.code.wpp_financas.models.TransactionType;
import com.code.wpp_financas.repositories.AccountRepository;
import com.code.wpp_financas.repositories.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private BalanceService balanceService;

    private Account account(Long id, AccountType type, String initialBalance) {
        Account account = new Account();
        account.setId(id);
        account.setType(type);
        account.setInitialBalance(new BigDecimal(initialBalance));
        return account;
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "esperado " + expected + " mas foi " + actual);
    }

    @Test
    void accountBalance_somaReceitasESubtraiGastos() {
        Account checking = account(1L, AccountType.CHECKING, "1000.00");
        when(transactionRepository.sumByAccountAndType(1L, TransactionType.INCOME))
                .thenReturn(new BigDecimal("500.00"));
        when(transactionRepository.sumByAccountAndType(1L, TransactionType.EXPENSE))
                .thenReturn(new BigDecimal("200.00"));

        assertMoney("1300.00", balanceService.accountBalance(checking));
    }

    @Test
    void accountBalance_semTransacoes_devolveSaldoInicial() {
        Account checking = account(1L, AccountType.CHECKING, "1000.00");
        when(transactionRepository.sumByAccountAndType(1L, TransactionType.INCOME)).thenReturn(null);
        when(transactionRepository.sumByAccountAndType(1L, TransactionType.EXPENSE)).thenReturn(null);

        assertMoney("1000.00", balanceService.accountBalance(checking));
    }

    @Test
    void spendableBalance_ignoraReservaECartao() {
        Account checking = account(1L, AccountType.CHECKING, "1000.00");
        Account reserve = account(2L, AccountType.EMERGENCY_FUND, "5000.00");
        Account card = account(3L, AccountType.CREDIT_CARD, "0.00");
        when(accountRepository.findByUserId(10L)).thenReturn(List.of(checking, reserve, card));
        when(transactionRepository.sumByAccountAndType(1L, TransactionType.INCOME))
                .thenReturn(new BigDecimal("200.00"));
        when(transactionRepository.sumByAccountAndType(1L, TransactionType.EXPENSE))
                .thenReturn(new BigDecimal("100.00"));

        assertMoney("1100.00", balanceService.spendableBalance(10L));
    }

    @Test
    void emergencyFund_somaSoAContaDeReserva() {
        Account checking = account(1L, AccountType.CHECKING, "1000.00");
        Account reserve = account(2L, AccountType.EMERGENCY_FUND, "5000.00");
        when(accountRepository.findByUserId(10L)).thenReturn(List.of(checking, reserve));
        when(transactionRepository.sumByAccountAndType(2L, TransactionType.INCOME)).thenReturn(null);
        when(transactionRepository.sumByAccountAndType(2L, TransactionType.EXPENSE)).thenReturn(null);

        assertMoney("5000.00", balanceService.emergencyFund(10L));
    }
}
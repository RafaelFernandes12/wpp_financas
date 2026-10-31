package com.code.wpp_financas.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.code.wpp_financas.models.Account;
import com.code.wpp_financas.models.Transaction;
import com.code.wpp_financas.models.TransactionType;
import com.code.wpp_financas.models.User;
import com.code.wpp_financas.repositories.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransferService transferService;

    @Test
    void transfer_criaDuasPontasLigadasPeloMesmoId() {
        User user = new User();
        Account from = new Account();
        Account to = new Account();

        transferService.transfer(user, from, to, new BigDecimal("500.00"), "teste");

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(captor.capture());
        List<Transaction> saved = captor.getAllValues();

        Transaction out = saved.get(0);
        Transaction in = saved.get(1);

        assertEquals(TransactionType.EXPENSE, out.getType());
        assertEquals(from, out.getAccount());
        assertEquals(TransactionType.INCOME, in.getType());
        assertEquals(to, in.getAccount());
        assertEquals(0, new BigDecimal("500.00").compareTo(out.getAmount()));
        assertEquals(0, new BigDecimal("500.00").compareTo(in.getAmount()));
        assertNotNull(out.getTransferId());
        assertEquals(out.getTransferId(), in.getTransferId());
    }

    @Test
    void delete_deTransferencia_apagaAsDuasPontas() {
        Transaction first = new Transaction();
        first.setTransferId("abc");
        Transaction second = new Transaction();
        second.setTransferId("abc");
        when(transactionRepository.legsOf("abc")).thenReturn(List.of(first, second));

        transferService.delete(first);

        verify(transactionRepository).deleteAll(List.of(first, second));
    }

    @Test
    void delete_deLancamentoComum_apagaSoEle() {
        Transaction normal = new Transaction();

        transferService.delete(normal);

        verify(transactionRepository).delete(normal);
        verifyNoMoreInteractions(transactionRepository);
    }
}
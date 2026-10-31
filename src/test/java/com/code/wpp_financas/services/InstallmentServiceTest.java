package com.code.wpp_financas.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.code.wpp_financas.models.Account;
import com.code.wpp_financas.models.Category;
import com.code.wpp_financas.models.Transaction;
import com.code.wpp_financas.models.TransactionType;
import com.code.wpp_financas.models.User;
import com.code.wpp_financas.repositories.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class InstallmentServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private InstallmentService installmentService;

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "esperado " + expected + " mas foi " + actual);
    }

    @Test
    void split_divisaoExata() {
        List<BigDecimal> amounts = InstallmentService.split(new BigDecimal("600.00"), 3);

        assertEquals(3, amounts.size());
        amounts.forEach(amount -> assertMoney("200.00", amount));
    }

    @Test
    void split_primeiraParcelaLevaOsCentavosQueSobram() {
        List<BigDecimal> amounts = InstallmentService.split(new BigDecimal("100.00"), 3);

        assertMoney("33.34", amounts.get(0));
        assertMoney("33.33", amounts.get(1));
        assertMoney("33.33", amounts.get(2));
        assertMoney("100.00", amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Test
    void create_geraUmaParcelaPorMes_ligadasPeloMesmoGrupo() {
        User user = new User();
        Account card = new Account();
        Category category = new Category("Alimentação");

        installmentService.create(user, card, category, new BigDecimal("600.00"), 3, "almoço");

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(3)).save(captor.capture());
        List<Transaction> saved = captor.getAllValues();

        assertEquals("almoço 1/3", saved.get(0).getDescription());
        assertEquals("almoço 2/3", saved.get(1).getDescription());
        assertEquals("almoço 3/3", saved.get(2).getDescription());

        assertEquals(saved.get(0).getDate().plusMonths(1), saved.get(1).getDate());
        assertEquals(saved.get(0).getDate().plusMonths(2), saved.get(2).getDate());

        assertNotNull(saved.get(0).getInstallmentGroupId());
        assertEquals(saved.get(0).getInstallmentGroupId(), saved.get(1).getInstallmentGroupId());
        assertEquals(saved.get(0).getInstallmentGroupId(), saved.get(2).getInstallmentGroupId());

        saved.forEach(installment -> {
            assertEquals(TransactionType.EXPENSE, installment.getType());
            assertEquals(card, installment.getAccount());
            assertEquals(category, installment.getCategory());
        });
    }

    @Test
    void create_semDescricao_usaCompraComoNome() {
        installmentService.create(new User(), new Account(), null, new BigDecimal("90.00"), 2, null);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(captor.capture());

        assertEquals("Compra 1/2", captor.getAllValues().get(0).getDescription());
        assertEquals("Compra 2/2", captor.getAllValues().get(1).getDescription());
    }
}
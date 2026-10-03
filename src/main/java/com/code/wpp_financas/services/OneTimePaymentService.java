package com.code.wpp_financas.services;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.code.wpp_financas.models.Payment;
import com.code.wpp_financas.repositories.PaymentRepository;

@Service
public class OneTimePaymentService {

  private final PaymentRepository paymentRepository;

  public OneTimePaymentService(PaymentRepository paymentRepository) {
    this.paymentRepository = paymentRepository;
  }

  public void oneTimePayment() {
    Payment payment = new Payment();
    payment.setValue(new BigDecimal("11.0"));
    paymentRepository.save(payment);

  }

}

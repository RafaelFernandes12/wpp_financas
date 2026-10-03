package com.code.wpp_financas.repositories;

import com.code.wpp_financas.models.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

}

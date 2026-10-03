package com.code.wpp_financas.controllers;

import org.springframework.stereotype.Controller;

import com.code.wpp_financas.services.OneTimePaymentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payment")
public class OneTimePaymentController {

  private final OneTimePaymentService oneTimePaymentService;

  public OneTimePaymentController(OneTimePaymentService oneTimePaymentService) {
    this.oneTimePaymentService = oneTimePaymentService;
  }

  @GetMapping("/once")
  public void oneTimePayment() {
    oneTimePaymentService.oneTimePayment();

  }

}

package com.code.wpp_financas.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
public class Payment {
  @Id
  @GeneratedValue()
  Long id;

  @Column
  BigDecimal value;

  @Column
  Date created_at = new Date();

  @Column
  Date updated_at = new Date();
}

package ru.kakatya.conveyor.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PaymentScheduleElementDto {
   private Integer number;
   private LocalDate date;
   private BigDecimal totalPayment;
   private BigDecimal interestPayment;
   private BigDecimal debtPayment;
   private BigDecimal remainingDebt;
}

package com.unibank.bankingSystem.dto;

import com.unibank.bankingSystem.model.Frequency;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
public class RecurringPaymentResponse {
    private Long id;
    private String fromAccountNumber;
    private String toAccountNumber;
    private BigDecimal amount;
    private String description;
    private Frequency frequency;
    private LocalDate nextRunDate;
    private boolean active;
}

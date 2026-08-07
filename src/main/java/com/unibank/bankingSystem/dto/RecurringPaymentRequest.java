package com.unibank.bankingSystem.dto;

import com.unibank.bankingSystem.model.Frequency;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class RecurringPaymentRequest {
    private Long fromAccountId;
    private String toAccountNumber;
    private BigDecimal amount;
    private String description;
    private Frequency frequency;
    private LocalDate startDate;
}

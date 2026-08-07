package com.unibank.bankingSystem.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
public class EmailTransferRequest {
    private Long fromAccountId;
    private String recipientEmail;
    private BigDecimal amount;
    private String description;
}

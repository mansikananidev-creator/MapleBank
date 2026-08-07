package com.unibank.bankingSystem.dto;

import com.unibank.bankingSystem.model.TransType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class ComplianceFlagResponse {
    private Long id;
    private Long transactionId;
    private String accountNumber;
    private String accountOwnerName;
    private TransType transactionType;
    private BigDecimal amount;
    private String reason;
    private boolean reviewed;
    private String reviewedByUsername;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}

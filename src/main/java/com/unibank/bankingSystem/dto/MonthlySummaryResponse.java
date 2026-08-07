package com.unibank.bankingSystem.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
public class MonthlySummaryResponse {
    private String month;
    private BigDecimal income;
    private BigDecimal expense;
}

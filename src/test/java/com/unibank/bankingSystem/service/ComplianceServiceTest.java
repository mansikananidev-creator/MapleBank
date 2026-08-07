package com.unibank.bankingSystem.service;

import com.unibank.bankingSystem.model.ComplianceFlag;
import com.unibank.bankingSystem.model.Transaction;
import com.unibank.bankingSystem.repository.ComplianceFlagRepository;
import com.unibank.bankingSystem.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

// Covers the FINTRAC-style large-transaction flagging: anything at or above the
// $10,000 CAD threshold must be flagged, and nothing below it should be.
@ExtendWith(MockitoExtension.class)
class ComplianceServiceTest {

    @Mock
    private ComplianceFlagRepository complianceFlagRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ComplianceService complianceService;

    @Test
    void flagIfLarge_savesFlag_whenAmountEqualsThreshold() {
        Transaction transaction = new Transaction();
        transaction.setAmount(ComplianceService.LARGE_TRANSACTION_THRESHOLD);

        complianceService.flagIfLarge(transaction);

        verify(complianceFlagRepository, times(1)).save(any(ComplianceFlag.class));
    }

    @Test
    void flagIfLarge_savesFlag_whenAmountAboveThreshold() {
        Transaction transaction = new Transaction();
        transaction.setAmount(new BigDecimal("15000.00"));

        complianceService.flagIfLarge(transaction);

        verify(complianceFlagRepository, times(1)).save(any(ComplianceFlag.class));
    }

    @Test
    void flagIfLarge_doesNothing_whenBelowThreshold() {
        Transaction transaction = new Transaction();
        transaction.setAmount(new BigDecimal("9999.99"));

        complianceService.flagIfLarge(transaction);

        verify(complianceFlagRepository, never()).save(any());
    }
}

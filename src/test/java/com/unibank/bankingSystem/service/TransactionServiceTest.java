package com.unibank.bankingSystem.service;

import com.unibank.bankingSystem.dto.TransactionResponse;
import com.unibank.bankingSystem.exception.BadRequestException;
import com.unibank.bankingSystem.exception.InsufficientFundsException;
import com.unibank.bankingSystem.model.Account;
import com.unibank.bankingSystem.model.AccountStatus;
import com.unibank.bankingSystem.model.TransType;
import com.unibank.bankingSystem.repository.AccountRepository;
import com.unibank.bankingSystem.repository.TransactionRepository;
import com.unibank.bankingSystem.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

// executeTransfer() is the shared core used by both account-number transfers and
// email (Interac-style) transfers, plus the recurring payment scheduler - these
// tests exercise it directly since it's the single place all the money-movement
// rules live.
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private ComplianceService complianceService;

    @InjectMocks
    private TransactionService transactionService;

    private Account newAccount(long id, String amount, AccountStatus status) {
        Account account = new Account();
        account.setId(id);
        account.setBalance(new BigDecimal(amount));
        account.setStatus(status);
        return account;
    }

    @Test
    void executeTransfer_throwsWhenFromAndToAreTheSameAccount() {
        Account account = newAccount(1L, "500.00", AccountStatus.ACTIVE);

        assertThatThrownBy(() -> transactionService.executeTransfer(account, account, new BigDecimal("10"), "test"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("can not be the same");
    }

    @Test
    void executeTransfer_throwsWhenInsufficientFunds() {
        Account from = newAccount(1L, "50.00", AccountStatus.ACTIVE);
        Account to = newAccount(2L, "0.00", AccountStatus.ACTIVE);

        assertThatThrownBy(() -> transactionService.executeTransfer(from, to, new BigDecimal("100.00"), "test"))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void executeTransfer_throwsWhenDestinationAccountIsNotActive() {
        Account from = newAccount(1L, "500.00", AccountStatus.ACTIVE);
        Account to = newAccount(2L, "0.00", AccountStatus.FROZEN);

        assertThatThrownBy(() -> transactionService.executeTransfer(from, to, new BigDecimal("10.00"), "test"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("frozen/closed");
    }

    @Test
    void executeTransfer_throwsWhenSourceAccountIsNotActive() {
        Account from = newAccount(1L, "500.00", AccountStatus.CLOSED);
        Account to = newAccount(2L, "0.00", AccountStatus.ACTIVE);

        assertThatThrownBy(() -> transactionService.executeTransfer(from, to, new BigDecimal("10.00"), "test"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("frozen/closed");
    }

    @Test
    void executeTransfer_movesMoneyAndRecordsBothLegs() {
        Account from = newAccount(1L, "500.00", AccountStatus.ACTIVE);
        Account to = newAccount(2L, "100.00", AccountStatus.ACTIVE);

        TransactionResponse response = transactionService.executeTransfer(from, to, new BigDecimal("200.00"), "rent");

        assertThat(from.getBalance()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(to.getBalance()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(response.getType()).isEqualTo(TransType.TRANSFER_OUT);
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("200.00"));

        verify(transactionRepository, times(2)).save(any());
        verify(complianceService, times(1)).flagIfLarge(any());
    }
}

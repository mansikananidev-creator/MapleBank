package com.unibank.bankingSystem.service;

import com.unibank.bankingSystem.dto.LoanRepaymentRequest;
import com.unibank.bankingSystem.dto.LoanRequest;
import com.unibank.bankingSystem.dto.LoanResponse;
import com.unibank.bankingSystem.exception.UnauthorizedException;
import com.unibank.bankingSystem.model.Account;
import com.unibank.bankingSystem.model.AccountStatus;
import com.unibank.bankingSystem.model.Loan;
import com.unibank.bankingSystem.model.LoanStatus;
import com.unibank.bankingSystem.model.User;
import com.unibank.bankingSystem.repository.AccountRepository;
import com.unibank.bankingSystem.repository.LoanRepository;
import com.unibank.bankingSystem.repository.TransactionRepository;
import com.unibank.bankingSystem.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

// Covers the ownership-check bug we fixed in repayLoan (a logged-in user used to be
// able to repay ANY loan by guessing its ID) so it can't silently regress.
@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private LoanRepository loanRepository;
    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private LoanService loanService;

    private MockedStatic<SecurityContextHolder> securityContextHolderMock;

    private User borrower;
    private User someoneElse;

    @BeforeEach
    void setUp() {
        borrower = new User();
        borrower.setId(1L);
        borrower.setEmail("borrower@example.com");

        someoneElse = new User();
        someoneElse.setId(2L);
        someoneElse.setEmail("someoneelse@example.com");
    }

    @AfterEach
    void tearDown() {
        if (securityContextHolderMock != null) {
            securityContextHolderMock.close();
        }
    }

    private void mockLoggedInUser(User user) {
        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn(user.getEmail());

        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        securityContextHolderMock = Mockito.mockStatic(SecurityContextHolder.class);
        securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    @Test
    void repayLoan_rejectsUserWhoDoesNotOwnTheLoan() {
        mockLoggedInUser(someoneElse);

        Loan loan = new Loan();
        loan.setId(10L);
        loan.setBorrower(borrower); // the loan belongs to someone else
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setRemainingAmount(new BigDecimal("100.00"));

        when(loanRepository.findById(10L)).thenReturn(Optional.of(loan));

        LoanRepaymentRequest request = new LoanRepaymentRequest();
        request.setAmount(new BigDecimal("50.00"));

        assertThatThrownBy(() -> loanService.repayLoan(10L, request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("do not own this loan");
    }

    @Test
    void repayLoan_paysDownBalanceAndMarksPaidOffWhenFullyRepaid() {
        mockLoggedInUser(borrower);

        Account account = new Account();
        account.setId(5L);
        account.setBalance(new BigDecimal("500.00"));
        account.setStatus(AccountStatus.ACTIVE);

        Loan loan = new Loan();
        loan.setId(10L);
        loan.setBorrower(borrower);
        loan.setAccount(account);
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setRemainingAmount(new BigDecimal("100.00"));

        when(loanRepository.findById(10L)).thenReturn(Optional.of(loan));

        LoanRepaymentRequest request = new LoanRepaymentRequest();
        request.setAmount(new BigDecimal("100.00"));

        LoanResponse response = loanService.repayLoan(10L, request);

        assertThat(response.getStatus()).isEqualTo(LoanStatus.PAID_OFF);
        assertThat(response.getRemainingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("400.00"));
    }

    @Test
    void repayLoan_partialPaymentKeepsLoanActive() {
        mockLoggedInUser(borrower);

        Account account = new Account();
        account.setId(5L);
        account.setBalance(new BigDecimal("500.00"));
        account.setStatus(AccountStatus.ACTIVE);

        Loan loan = new Loan();
        loan.setId(10L);
        loan.setBorrower(borrower);
        loan.setAccount(account);
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setRemainingAmount(new BigDecimal("100.00"));

        when(loanRepository.findById(10L)).thenReturn(Optional.of(loan));

        LoanRepaymentRequest request = new LoanRepaymentRequest();
        request.setAmount(new BigDecimal("40.00"));

        LoanResponse response = loanService.repayLoan(10L, request);

        assertThat(response.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(response.getRemainingBalance()).isEqualByComparingTo(new BigDecimal("60.00"));
    }

    @Test
    void applyForLoan_rejectsUserWhoDoesNotOwnTheAccount() {
        mockLoggedInUser(someoneElse);

        Account account = new Account();
        account.setId(5L);
        account.setOwner(borrower); // account belongs to someone else

        when(accountRepository.findById(5L)).thenReturn(Optional.of(account));

        LoanRequest request = new LoanRequest();
        request.setAccountId(5L);
        request.setAmount(new BigDecimal("1000"));
        request.setTermMonths(12);
        request.setPurpose("Test loan");

        assertThatThrownBy(() -> loanService.applyForLoan(request))
                .isInstanceOf(UnauthorizedException.class);
    }
}

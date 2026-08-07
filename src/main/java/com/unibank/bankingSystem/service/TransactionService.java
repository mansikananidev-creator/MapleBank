package com.unibank.bankingSystem.service;

import com.unibank.bankingSystem.dto.EmailTransferRequest;
import com.unibank.bankingSystem.dto.MonthlySummaryResponse;
import com.unibank.bankingSystem.dto.TransactionRequest;
import com.unibank.bankingSystem.dto.TransactionResponse;
import com.unibank.bankingSystem.dto.TransferRequest;
import com.unibank.bankingSystem.exception.BadRequestException;
import com.unibank.bankingSystem.exception.InsufficientFundsException;
import com.unibank.bankingSystem.exception.ResourceNotFoundException;
import com.unibank.bankingSystem.exception.UnauthorizedException;
import com.unibank.bankingSystem.model.*;
import com.unibank.bankingSystem.repository.AccountRepository;
import com.unibank.bankingSystem.repository.TransactionRepository;
import com.unibank.bankingSystem.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final ComplianceService complianceService;

    public TransactionResponse deposit(TransactionRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new ResourceNotFoundException("User not found")
        );

        Account account = accountRepository.findById(request.getAccountId()).orElseThrow(
                () -> new ResourceNotFoundException("Account not found")
        );
        if (!account.getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedException("You do not own this account");
        }

        account.setBalance(account.getBalance().add(request.getAmount()));

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setAmount(request.getAmount());
        transaction.setBalanceAfter(account.getBalance());
        transaction.setType(TransType.DEPOSIT);
        transaction.setDescription(request.getDescription());

        accountRepository.save(account);
        transactionRepository.save(transaction);
        complianceService.flagIfLarge(transaction);

        return new TransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getBalanceAfter(),
                transaction.getDescription(),
                transaction.getCreatedAt()
        );
    }

    public TransactionResponse withdraw(TransactionRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new ResourceNotFoundException("User not found")
        );

        Account account = accountRepository.findById(request.getAccountId()).orElseThrow(
                () -> new ResourceNotFoundException("Account not found")
        );
        if (!account.getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedException("You do not own this account");
        }

        if(account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }

        account.setBalance(account.getBalance().subtract(request.getAmount()));

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setAmount(request.getAmount());
        transaction.setBalanceAfter(account.getBalance());
        transaction.setType(TransType.WITHDRAWAL);
        transaction.setDescription(request.getDescription());

        accountRepository.save(account);
        transactionRepository.save(transaction);
        complianceService.flagIfLarge(transaction);

        return new TransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getBalanceAfter(),
                transaction.getDescription(),
                transaction.getCreatedAt()
        );
    }


    @Transactional
    public TransactionResponse transfer(TransferRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new ResourceNotFoundException("User not found")
        );

        Account fromAccount = accountRepository.findById(request.getFromAccountId()).orElseThrow(
                () -> new ResourceNotFoundException("Account not found")
        );
        if (!fromAccount.getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedException("You do not own this account");
        }

        Account toAccount = accountRepository.findByAccountNumber(request.getToAccountNumber()).orElseThrow(
                () -> new ResourceNotFoundException("Account not found")
        );

        return executeTransfer(fromAccount, toAccount, request.getAmount(), request.getDescription());
    }

    // Interac-style e-Transfer: instead of requiring the recipient's raw account number,
    // the sender only needs to know the recipient's email. We look up the recipient's
    // UniBank user by email and deposit into their first active account - this mirrors
    // how real Canadian e-Transfers route to a "default" deposit account.
    @Transactional
    public TransactionResponse transferByEmail(EmailTransferRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new ResourceNotFoundException("User not found")
        );

        Account fromAccount = accountRepository.findById(request.getFromAccountId()).orElseThrow(
                () -> new ResourceNotFoundException("Account not found")
        );
        if (!fromAccount.getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedException("You do not own this account");
        }

        if (request.getRecipientEmail() == null || request.getRecipientEmail().equalsIgnoreCase(user.getEmail())) {
            throw new BadRequestException("You can not e-Transfer money to yourself");
        }

        User recipient = userRepository.findByEmail(request.getRecipientEmail()).orElseThrow(
                () -> new ResourceNotFoundException("No Maple Bank user is registered with that email")
        );

        Account toAccount = accountRepository.findByOwner(recipient).stream()
                .filter(a -> a.getStatus() == AccountStatus.ACTIVE)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Recipient has no active account to receive this e-Transfer"));

        String description = "Interac e-Transfer" +
                (request.getDescription() != null && !request.getDescription().isBlank() ? ": " + request.getDescription() : "");

        return executeTransfer(fromAccount, toAccount, request.getAmount(), description);
    }

    // Package-private on purpose: reused internally by RecurringPaymentService for
    // background-scheduled transfers, which have no logged-in user / security context.
    // Not exposed by any controller directly.
    TransactionResponse executeTransfer(Account fromAccount, Account toAccount, BigDecimal amount, String description) {
        if (fromAccount.getId().equals(toAccount.getId())) {
            throw new BadRequestException("The from-account can not be the same as the to-account");
        }

        if (fromAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new BadRequestException("You can not transfer from a frozen/closed account");
        }

        if (toAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new BadRequestException("You can not make a transfer to a frozen/closed account");
        }

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        Transaction outTransaction = new Transaction();
        outTransaction.setAccount(fromAccount);
        outTransaction.setAmount(amount);
        outTransaction.setBalanceAfter(fromAccount.getBalance());
        outTransaction.setType(TransType.TRANSFER_OUT);
        outTransaction.setDescription(description);

        Transaction inTransaction = new Transaction();
        inTransaction.setAccount(toAccount);
        inTransaction.setAmount(amount);
        inTransaction.setBalanceAfter(toAccount.getBalance());
        inTransaction.setType(TransType.TRANSFER_IN);
        inTransaction.setDescription(description);

        accountRepository.save(fromAccount);
        transactionRepository.save(outTransaction);
        complianceService.flagIfLarge(outTransaction);

        accountRepository.save(toAccount);
        transactionRepository.save(inTransaction);

        return new TransactionResponse(
                outTransaction.getId(),
                outTransaction.getType(),
                outTransaction.getAmount(),
                outTransaction.getBalanceAfter(),
                outTransaction.getDescription(),
                outTransaction.getCreatedAt()
        );
    }

    public Page<TransactionResponse> getTransactionHistory(Long accountId, Pageable pageable) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new ResourceNotFoundException("User not found")
        );

        Account account = accountRepository.findById(accountId).orElseThrow(
                () -> new ResourceNotFoundException("Account not found")
        );
        if (!account.getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedException("You do not own this account");
        }
        Page<Transaction> transactions = transactionRepository.findByAccountOrderByCreatedAtDesc(account, pageable);

        return transactions.map(transaction -> new TransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getBalanceAfter(),
                transaction.getDescription(),
                transaction.getCreatedAt()
        ));
    }

    public List<TransactionResponse> getRecentTransactions() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new ResourceNotFoundException("User not found")
        );
        Pageable pageable = PageRequest.of(0,10);
        Page<Transaction> transactions = transactionRepository.findByAccountOwnerOrderByCreatedAtDesc(user, pageable);

        return transactions.map(transaction -> new TransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getBalanceAfter(),
                transaction.getDescription(),
                transaction.getCreatedAt()
        )).getContent();
    }

    // Powers the frontend spending dashboard: income vs. expense totals per calendar
    // month, for the last 6 months, across all of the current user's accounts.
    public List<MonthlySummaryResponse> getMonthlySummary() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new ResourceNotFoundException("User not found")
        );

        LocalDateTime since = LocalDateTime.now().minusMonths(5).withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);

        List<Transaction> transactions = transactionRepository.findByAccountOwnerAndCreatedAtAfter(user, since);

        Map<String, BigDecimal[]> byMonth = new TreeMap<>();
        DateTimeFormatter monthFormat = DateTimeFormatter.ofPattern("yyyy-MM");

        for (Transaction tx : transactions) {
            String month = tx.getCreatedAt().format(monthFormat);
            BigDecimal[] totals = byMonth.computeIfAbsent(month, m -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});

            boolean inbound = tx.getType() == TransType.DEPOSIT
                    || tx.getType() == TransType.TRANSFER_IN
                    || tx.getType() == TransType.LOAN_DISBURSEMENT;

            if (inbound) {
                totals[0] = totals[0].add(tx.getAmount());
            } else {
                totals[1] = totals[1].add(tx.getAmount());
            }
        }

        return byMonth.entrySet().stream()
                .map(e -> new MonthlySummaryResponse(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();
    }
}

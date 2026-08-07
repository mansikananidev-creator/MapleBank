package com.unibank.bankingSystem.service;

import com.unibank.bankingSystem.dto.RecurringPaymentRequest;
import com.unibank.bankingSystem.dto.RecurringPaymentResponse;
import com.unibank.bankingSystem.exception.ResourceNotFoundException;
import com.unibank.bankingSystem.exception.UnauthorizedException;
import com.unibank.bankingSystem.model.Account;
import com.unibank.bankingSystem.model.Frequency;
import com.unibank.bankingSystem.model.RecurringPayment;
import com.unibank.bankingSystem.model.User;
import com.unibank.bankingSystem.repository.AccountRepository;
import com.unibank.bankingSystem.repository.RecurringPaymentRepository;
import com.unibank.bankingSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringPaymentService {

    private final RecurringPaymentRepository recurringPaymentRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionService transactionService;

    public RecurringPaymentResponse createRecurringPayment(RecurringPaymentRequest request) {
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

        if (!accountRepository.existsByAccountNumber(request.getToAccountNumber())) {
            throw new ResourceNotFoundException("Recipient account not found");
        }

        RecurringPayment payment = new RecurringPayment();
        payment.setFromAccount(fromAccount);
        payment.setToAccountNumber(request.getToAccountNumber());
        payment.setAmount(request.getAmount());
        payment.setDescription(request.getDescription());
        payment.setFrequency(request.getFrequency());
        payment.setNextRunDate(request.getStartDate() != null ? request.getStartDate() : LocalDate.now());
        payment.setActive(true);

        recurringPaymentRepository.save(payment);

        return toResponse(payment);
    }

    public List<RecurringPaymentResponse> getUserRecurringPayments() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new ResourceNotFoundException("User not found")
        );

        return recurringPaymentRepository.findByFromAccount_Owner(user).stream()
                .map(this::toResponse)
                .toList();
    }

    public void cancelRecurringPayment(Long id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new ResourceNotFoundException("User not found")
        );

        RecurringPayment payment = recurringPaymentRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Recurring payment not found")
        );

        if (!payment.getFromAccount().getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedException("You do not own this recurring payment");
        }

        payment.setActive(false);
        recurringPaymentRepository.save(payment);
    }

    // Runs once a day. A real system would use a proper job scheduler (Quartz, or an
    // external cron hitting a protected endpoint) with retries/alerting, but @Scheduled
    // demonstrates the same core idea: background work with no HTTP request or logged-in
    // user behind it, which is why it calls TransactionService.executeTransfer() directly
    // instead of going through the normal (security-context-dependent) transfer() method.
    @Scheduled(cron = "0 0 1 * * *")
    public void executeDuePayments() {
        List<RecurringPayment> duePayments =
                recurringPaymentRepository.findByActiveTrueAndNextRunDateLessThanEqual(LocalDate.now());

        for (RecurringPayment payment : duePayments) {
            try {
                Account fromAccount = payment.getFromAccount();
                Account toAccount = accountRepository.findByAccountNumber(payment.getToAccountNumber()).orElse(null);

                if (toAccount == null) {
                    log.warn("Deactivating recurring payment {}: destination account no longer exists", payment.getId());
                    payment.setActive(false);
                } else {
                    String description = "Recurring payment" +
                            (payment.getDescription() != null && !payment.getDescription().isBlank() ? ": " + payment.getDescription() : "");
                    transactionService.executeTransfer(fromAccount, toAccount, payment.getAmount(), description);
                    payment.setNextRunDate(nextRunDate(payment));
                }
            } catch (Exception ex) {
                log.warn("Recurring payment {} failed, will retry next run: {}", payment.getId(), ex.getMessage());
                // nextRunDate is left unchanged so it's picked up again on the next scheduler run
            }
            recurringPaymentRepository.save(payment);
        }
    }

    private LocalDate nextRunDate(RecurringPayment payment) {
        return payment.getFrequency() == Frequency.WEEKLY
                ? payment.getNextRunDate().plusWeeks(1)
                : payment.getNextRunDate().plusMonths(1);
    }

    private RecurringPaymentResponse toResponse(RecurringPayment payment) {
        return new RecurringPaymentResponse(
                payment.getId(),
                payment.getFromAccount().getAccountNumber(),
                payment.getToAccountNumber(),
                payment.getAmount(),
                payment.getDescription(),
                payment.getFrequency(),
                payment.getNextRunDate(),
                payment.isActive()
        );
    }
}

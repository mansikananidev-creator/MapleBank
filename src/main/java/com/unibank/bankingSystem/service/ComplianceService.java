package com.unibank.bankingSystem.service;

import com.unibank.bankingSystem.dto.ComplianceFlagResponse;
import com.unibank.bankingSystem.exception.BadRequestException;
import com.unibank.bankingSystem.exception.ResourceNotFoundException;
import com.unibank.bankingSystem.model.ComplianceFlag;
import com.unibank.bankingSystem.model.Transaction;
import com.unibank.bankingSystem.model.User;
import com.unibank.bankingSystem.repository.ComplianceFlagRepository;
import com.unibank.bankingSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ComplianceService {

    // FINTRAC (Canada's Financial Transactions and Reports Analysis Centre) requires
    // reporting of transactions of CAD $10,000 or more. We mirror that threshold here
    // to auto-flag large transactions for admin review, similar to a real bank's
    // Large Transaction Report (LTR) process.
    public static final BigDecimal LARGE_TRANSACTION_THRESHOLD = new BigDecimal("10000");

    private final ComplianceFlagRepository complianceFlagRepository;
    private final UserRepository userRepository;

    public void flagIfLarge(Transaction transaction) {
        if (transaction.getAmount().compareTo(LARGE_TRANSACTION_THRESHOLD) >= 0) {
            ComplianceFlag flag = new ComplianceFlag();
            flag.setTransaction(transaction);
            flag.setReason("Transaction amount of $" + transaction.getAmount() +
                    " CAD meets or exceeds the $10,000 large transaction reporting threshold.");
            complianceFlagRepository.save(flag);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<ComplianceFlagResponse> getFlags() {
        return complianceFlagRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public ComplianceFlagResponse reviewFlag(Long flagId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User reviewer = userRepository.findByEmail(email).orElseThrow(
                () -> new ResourceNotFoundException("User not found")
        );

        ComplianceFlag flag = complianceFlagRepository.findById(flagId).orElseThrow(
                () -> new ResourceNotFoundException("Compliance flag not found")
        );

        if (flag.isReviewed()) {
            throw new BadRequestException("This flag has already been reviewed");
        }

        flag.setReviewed(true);
        flag.setReviewedBy(reviewer);
        flag.setReviewedAt(LocalDateTime.now());
        complianceFlagRepository.save(flag);

        return toResponse(flag);
    }

    private ComplianceFlagResponse toResponse(ComplianceFlag flag) {
        Transaction tx = flag.getTransaction();
        return new ComplianceFlagResponse(
                flag.getId(),
                tx.getId(),
                tx.getAccount().getAccountNumber(),
                tx.getAccount().getOwner().getFullName(),
                tx.getType(),
                tx.getAmount(),
                flag.getReason(),
                flag.isReviewed(),
                flag.getReviewedBy() != null ? flag.getReviewedBy().getUsername() : null,
                flag.getReviewedAt(),
                flag.getCreatedAt()
        );
    }
}

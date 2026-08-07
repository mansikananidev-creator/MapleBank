package com.unibank.bankingSystem.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "recurring_payments")
public class RecurringPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "from_account_id", nullable = false)
    private Account fromAccount;

    @Column(nullable = false)
    private String toAccountNumber;

    @Column(nullable = false)
    private BigDecimal amount;

    private String description;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Frequency frequency;

    @Column(nullable = false)
    private LocalDate nextRunDate;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

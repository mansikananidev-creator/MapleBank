package com.unibank.bankingSystem.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "compliance_flags")
public class ComplianceFlag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "transaction_id", nullable = false, unique = true)
    private Transaction transaction;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private boolean reviewed = false;

    @ManyToOne
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    private LocalDateTime reviewedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

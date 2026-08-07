package com.unibank.bankingSystem.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false)
    private int failedLoginAttempts = 0;

    private LocalDateTime lockedUntil;

    // KYC-style personal info. All nullable so existing accounts (created before this
    // feature existed) aren't broken - a real bank would require these at account
    // opening, but here they're filled in later via the profile page.
    private String phoneNumber;

    private LocalDate dateOfBirth;

    private String addressLine1;

    private String addressLine2;

    private String city;

    private String province;

    private String postalCode;

    @OneToMany(mappedBy = "owner")
    private List<Account> accounts;
}

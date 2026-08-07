package com.unibank.bankingSystem.dto;

import com.unibank.bankingSystem.model.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private Role role;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String province;
    private String postalCode;
}

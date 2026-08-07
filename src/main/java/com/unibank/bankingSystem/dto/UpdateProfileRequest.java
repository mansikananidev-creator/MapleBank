package com.unibank.bankingSystem.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateProfileRequest {
    private String fullName;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String province;
    private String postalCode;
}

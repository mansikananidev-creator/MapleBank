package com.unibank.bankingSystem.service;

import com.unibank.bankingSystem.dto.ChangePasswordRequest;
import com.unibank.bankingSystem.dto.UpdateProfileRequest;
import com.unibank.bankingSystem.dto.UserProfileResponse;
import com.unibank.bankingSystem.exception.BadRequestException;
import com.unibank.bankingSystem.exception.ResourceNotFoundException;
import com.unibank.bankingSystem.model.User;
import com.unibank.bankingSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserService {

    // Standard Canadian postal code format, e.g. "K1A 0B1" (the space/hyphen is optional).
    private static final Pattern CANADIAN_POSTAL_CODE =
            Pattern.compile("^[A-Za-z]\\d[A-Za-z][ -]?\\d[A-Za-z]\\d$");

    private static final int MINIMUM_AGE_YEARS = 18;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProfileResponse getProfile() {
        return toResponse(getCurrentUser());
    }

    public UserProfileResponse updateProfile(UpdateProfileRequest request) {
        User user = getCurrentUser();

        if (request.getDateOfBirth() != null) {
            if (request.getDateOfBirth().isAfter(LocalDate.now())) {
                throw new BadRequestException("Date of birth can not be in the future");
            }
            if (Period.between(request.getDateOfBirth(), LocalDate.now()).getYears() < MINIMUM_AGE_YEARS) {
                throw new BadRequestException("You must be at least " + MINIMUM_AGE_YEARS + " years old");
            }
        }

        if (request.getPostalCode() != null && !request.getPostalCode().isBlank()
                && !CANADIAN_POSTAL_CODE.matcher(request.getPostalCode().trim()).matches()) {
            throw new BadRequestException("Postal code must be a valid Canadian postal code, e.g. K1A 0B1");
        }

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        user.setPhoneNumber(request.getPhoneNumber());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setAddressLine1(request.getAddressLine1());
        user.setAddressLine2(request.getAddressLine2());
        user.setCity(request.getCity());
        user.setProvince(request.getProvince());
        user.setPostalCode(request.getPostalCode());

        userRepository.save(user);

        return toResponse(user);
    }

    public void changePassword(ChangePasswordRequest request) {
        User user = getCurrentUser();

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        if (request.getNewPassword() == null || request.getNewPassword().length() < 8) {
            throw new BadRequestException("New password must be at least 8 characters");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private UserProfileResponse toResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getPhoneNumber(),
                user.getDateOfBirth(),
                user.getAddressLine1(),
                user.getAddressLine2(),
                user.getCity(),
                user.getProvince(),
                user.getPostalCode()
        );
    }
}

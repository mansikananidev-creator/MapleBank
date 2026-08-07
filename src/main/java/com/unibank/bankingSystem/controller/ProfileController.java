package com.unibank.bankingSystem.controller;

import com.unibank.bankingSystem.dto.ChangePasswordRequest;
import com.unibank.bankingSystem.dto.UpdateProfileRequest;
import com.unibank.bankingSystem.dto.UserProfileResponse;
import com.unibank.bankingSystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    @GetMapping
    public UserProfileResponse getProfile() {
        return userService.getProfile();
    }

    @PutMapping
    public UserProfileResponse updateProfile(@RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(request);
    }

    @PutMapping("/change-password")
    public void changePassword(@RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
    }
}

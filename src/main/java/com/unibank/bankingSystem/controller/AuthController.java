package com.unibank.bankingSystem.controller;

import com.unibank.bankingSystem.dto.AuthResponse;
import com.unibank.bankingSystem.dto.ForgotPasswordRequest;
import com.unibank.bankingSystem.dto.LoginRequest;
import com.unibank.bankingSystem.dto.RegisterRequest;
import com.unibank.bankingSystem.dto.ResetPasswordRequest;
import com.unibank.bankingSystem.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/forgot-password")
    public void forgotPassword(@RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public void resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
    }
}

package com.artSight.backend.controller;

import com.artSight.backend.dto.AuthDtos.*;
import com.artSight.backend.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // --- Sign up ---

    @PostMapping("/signup/send-otp")
    public Map<String, String> signupSendOtp(@RequestBody EmailRequest req) {
        authService.sendSignupOtp(req.email());
        return Map.of("status", "success", "message", "OTP sent");
    }

    @PostMapping("/signup/verify-otp")
    public Map<String, String> signupVerifyOtp(@RequestBody VerifyOtpRequest req) {
        String verificationToken = authService.verifySignupOtp(req.email(), req.otp());
        return Map.of("status", "success", "verificationToken", verificationToken);
    }

    @PostMapping("/signup/set-password")
    public Map<String, String> signupSetPassword(@RequestBody SetPasswordRequest req) {
        String token = authService.completeSignup(req.verificationToken(), req.password());
        return Map.of("status", "success", "token", token);
    }

    // --- Login ---

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody LoginRequest req) {
        String token = authService.loginWithPassword(req.email(), req.password());
        return Map.of("status", "success", "token", token);
    }

    @PostMapping("/login/send-otp")
    public Map<String, String> loginSendOtp(@RequestBody EmailRequest req) {
        authService.sendLoginOtp(req.email());
        return Map.of("status", "success", "message", "OTP sent");
    }

    @PostMapping("/login/verify-otp")
    public Map<String, String> loginVerifyOtp(@RequestBody VerifyOtpRequest req) {
        String token = authService.loginWithOtp(req.email(), req.otp());
        return Map.of("status", "success", "token", token);
    }
}
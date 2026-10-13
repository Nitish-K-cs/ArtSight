package com.artSight.backend.dto;

public class AuthDtos {
    public record EmailRequest(String email) {
    }

    public record VerifyOtpRequest(String email, String otp) {
    }

    public record SetPasswordRequest(String verificationToken, String password) {
    }

    public record LoginRequest(String email, String password) {
    }
}
package com.artSight.backend.controller;


import com.artSight.backend.service.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.artSight.backend.dto.OtpRequest;
import com.artSight.backend.dto.OtpVerifyRequest;


@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private OtpService otpService;

    @PostMapping("/send-otp")
    public String sendOtp(@RequestBody OtpRequest request) {
        String otp = otpService.generateOtp(request.getEmail());
        // TODO: integrate email/SMS provider — for now, log it so you can test
        System.out.println("OTP for " + request.getEmail() + ": " + otp);
        return "OTP sent";
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestBody OtpVerifyRequest request) {
        boolean valid = otpService.verifyOtp(request.getEmail(), request.getOtp());
        if (valid) {
            return "OTP verified"; // JWT issuance plugs in right here next step
        }
        return "Invalid or expired OTP";
    }
}
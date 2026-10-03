package com.artSight.backend.controller;

import com.artSight.backend.dto.OtpRequest;
import com.artSight.backend.dto.OtpVerifyRequest;
import com.artSight.backend.entity.User;
import com.artSight.backend.repository.UserRepository;
import com.artSight.backend.service.JwtService;
import com.artSight.backend.service.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private OtpService otpService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/send-otp")
    public String sendOtp(@RequestBody OtpRequest request) {
        String otp = otpService.generateOtp(request.getEmail());
        System.out.println("OTP for " + request.getEmail() + ": " + otp);
        return "OTP sent";
    }

    @PostMapping("/verify-otp")
    public Map<String, String> verifyOtp(@RequestBody OtpVerifyRequest request) {
        boolean valid = otpService.verifyOtp(request.getEmail(), request.getOtp());

        Map<String, String> response = new HashMap<>();

        if (!valid) {
            response.put("status", "error");
            response.put("message", "Invalid or expired OTP");
            return response;
        }

        // Create user if first-time login, otherwise fetch existing
        User user = userRepository.findByEmail(request.getEmail())
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(request.getEmail());
                    return userRepository.save(newUser);
                });

        String token = jwtService.generateToken(user.getEmail());

        response.put("status", "success");
        response.put("token", token);
        return response;
    }
}
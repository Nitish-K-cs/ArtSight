package com.artSight.backend.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
public class OtpService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final Duration OTP_EXPIRY = Duration.ofMinutes(5);
    private static final String OTP_PREFIX = "otp:";

    public String generateOtp(String email) {
        String otp = String.valueOf(new SecureRandom().nextInt(900000) + 100000); // 6-digit
        redisTemplate.opsForValue().set(OTP_PREFIX + email, otp, OTP_EXPIRY);
        return otp;
    }

    public boolean verifyOtp(String email, String otp) {
        String storedOtp = redisTemplate.opsForValue().get(OTP_PREFIX + email);
        if (storedOtp != null && storedOtp.equals(otp)) {
            redisTemplate.delete(OTP_PREFIX + email); // one-time use
            return true;
        }
        return false;
    }
}
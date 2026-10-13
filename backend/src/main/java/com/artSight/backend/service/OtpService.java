package com.artSight.backend.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;

@Service
public class OtpService {

    private static final Duration OTP_EXPIRY = Duration.ofMinutes(5);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final int MAX_ATTEMPTS = 5;

    private final RedisTemplate<String, String> redisTemplate;
    private final SecureRandom random = new SecureRandom();

    public OtpService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private static String key(String kind, String purpose, String email) {
        return "otp:" + kind + ":" + purpose + ":" + email;
    }

    public String generateOtp(String email, String purpose) {
        Boolean allowed = redisTemplate.opsForValue()
                .setIfAbsent(key("cooldown", purpose, email), "1", RESEND_COOLDOWN);
        if (!Boolean.TRUE.equals(allowed)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Please wait a minute before requesting another code");
        }

        String otp = String.valueOf(random.nextInt(900000) + 100000);
        redisTemplate.opsForValue().set(key("code", purpose, email), otp, OTP_EXPIRY);
        redisTemplate.delete(key("attempts", purpose, email));
        return otp;
    }

    public boolean verifyOtp(String email, String purpose, String otp) {
        if (otp == null)
            return false;

        String codeKey = key("code", purpose, email);
        String attemptsKey = key("attempts", purpose, email);

        String stored = redisTemplate.opsForValue().get(codeKey);
        if (stored == null)
            return false;

        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(attemptsKey, OTP_EXPIRY);
        }
        if (attempts != null && attempts > MAX_ATTEMPTS) {
            redisTemplate.delete(codeKey);
            redisTemplate.delete(attemptsKey);
            return false;
        }

        boolean match = MessageDigest.isEqual(
                stored.getBytes(StandardCharsets.UTF_8),
                otp.trim().getBytes(StandardCharsets.UTF_8));

        if (match) {
            redisTemplate.delete(codeKey);
            redisTemplate.delete(attemptsKey);
        }
        return match;
    }
}
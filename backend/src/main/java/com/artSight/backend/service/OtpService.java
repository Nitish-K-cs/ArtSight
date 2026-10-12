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

    private static final String OTP_PREFIX = "otp:";
    private static final String COOLDOWN_PREFIX = "otp:cooldown:";
    private static final String ATTEMPTS_PREFIX = "otp:attempts:";

    private final RedisTemplate<String, String> redisTemplate;
    private final SecureRandom random = new SecureRandom();

    public OtpService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String generateOtp(String rawEmail) {
        String email = normalize(rawEmail);

        // Atomic: only succeeds if no cooldown key exists yet
        Boolean allowed = redisTemplate.opsForValue()
                .setIfAbsent(COOLDOWN_PREFIX + email, "1", RESEND_COOLDOWN);
        if (!Boolean.TRUE.equals(allowed)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Please wait before requesting another code");
        }

        String otp = String.valueOf(random.nextInt(900000) + 100000);
        redisTemplate.opsForValue().set(OTP_PREFIX + email, otp, OTP_EXPIRY);
        redisTemplate.delete(ATTEMPTS_PREFIX + email); // fresh code, fresh attempt counter
        return otp;
    }

    public boolean verifyOtp(String rawEmail, String otp) {
        if (otp == null)
            return false;
        String email = normalize(rawEmail);

        String stored = redisTemplate.opsForValue().get(OTP_PREFIX + email);
        if (stored == null)
            return false;

        String attemptsKey = ATTEMPTS_PREFIX + email;
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(attemptsKey, OTP_EXPIRY);
        }
        if (attempts != null && attempts > MAX_ATTEMPTS) {
            redisTemplate.delete(OTP_PREFIX + email);
            redisTemplate.delete(attemptsKey);
            return false;
        }

        boolean match = MessageDigest.isEqual(
                stored.getBytes(StandardCharsets.UTF_8),
                otp.trim().getBytes(StandardCharsets.UTF_8));

        if (match) {
            redisTemplate.delete(OTP_PREFIX + email);
            redisTemplate.delete(attemptsKey);
        }
        return match;
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
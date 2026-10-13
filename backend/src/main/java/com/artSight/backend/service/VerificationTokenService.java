package com.artSight.backend.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

@Service
public class VerificationTokenService {

    private static final Duration TTL = Duration.ofMinutes(10);
    private static final String PREFIX = "verified:";

    private final RedisTemplate<String, String> redisTemplate;
    private final SecureRandom random = new SecureRandom();

    public VerificationTokenService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String issue(String email, String purpose) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        redisTemplate.opsForValue().set(PREFIX + purpose + ":" + token, email, TTL);
        return token;
    }

    /**
     * Single use: returns the verified email and deletes the token, or null if
     * invalid or expired.
     */
    public String consume(String token, String purpose) {
        if (token == null || token.isBlank())
            return null;
        return redisTemplate.opsForValue().getAndDelete(PREFIX + purpose + ":" + token);
    }
}
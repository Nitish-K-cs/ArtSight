package com.artSight.backend.service;

import com.artSight.backend.entity.User;
import com.artSight.backend.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final String SIGNUP = "signup";
    private static final String LOGIN = "login";

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_BYTES = 72; // BCrypt ignores anything beyond this

    private static final int MAX_FAILED_LOGINS = 5;
    private static final Duration LOCKOUT = Duration.ofMinutes(15);
    private static final String FAIL_PREFIX = "login:fail:";

    private final OtpService otpService;
    private final EmailService emailService;
    private final VerificationTokenService verificationTokens;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;
    private final String dummyHash; // keeps response time similar for unknown emails

    public AuthService(OtpService otpService,
            EmailService emailService,
            VerificationTokenService verificationTokens,
            UserRepository userRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            RedisTemplate<String, String> redisTemplate) {
        this.otpService = otpService;
        this.emailService = emailService;
        this.verificationTokens = verificationTokens;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    // ---------- Sign up ----------

    public void sendSignupOtp(String rawEmail) {
        String email = normalize(rawEmail);
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An account with this email already exists. Please log in.");
        }
        String otp = otpService.generateOtp(email, SIGNUP);
        emailService.sendOtpEmail(email, otp);
    }

    public String verifySignupOtp(String rawEmail, String otp) {
        String email = normalize(rawEmail);
        if (!otpService.verifyOtp(email, SIGNUP, otp)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired code");
        }
        return verificationTokens.issue(email, SIGNUP);
    }

    public String completeSignup(String verificationToken, String password) {
        validatePassword(password); // before consuming the token, so a weak password doesn't burn it

        String email = verificationTokens.consume(verificationToken, SIGNUP);
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Verification expired. Please start sign-up again.");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An account with this email already exists. Please log in.");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An account with this email already exists. Please log in.");
        }
        return jwtService.generateToken(email);
    }

    // ---------- Login with password ----------

    public String loginWithPassword(String rawEmail, String password) {
        String email = normalize(rawEmail);
        ensureNotLocked(email);

        User user = userRepository.findByEmail(email).orElse(null);
        boolean hasPassword = user != null && user.getPasswordHash() != null;
        String hash = hasPassword ? user.getPasswordHash() : dummyHash;

        boolean validInput = password != null
                && password.getBytes(StandardCharsets.UTF_8).length <= MAX_PASSWORD_BYTES;
        boolean matches = validInput && passwordEncoder.matches(password, hash);

        if (!hasPassword || !matches) {
            recordFailure(email);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        redisTemplate.delete(FAIL_PREFIX + email);
        return jwtService.generateToken(email);
    }

    // ---------- Login with OTP ----------

    public void sendLoginOtp(String rawEmail) {
        String email = normalize(rawEmail);
        if (userRepository.findByEmail(email).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No account found for this email. Please sign up first.");
        }
        String otp = otpService.generateOtp(email, LOGIN);
        emailService.sendOtpEmail(email, otp);
    }

    public String loginWithOtp(String rawEmail, String otp) {
        String email = normalize(rawEmail);
        if (!otpService.verifyOtp(email, LOGIN, otp)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired code");
        }
        if (userRepository.findByEmail(email).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No account found for this email. Please sign up first.");
        }
        return jwtService.generateToken(email);
    }

    // ---------- Helpers ----------

    private String normalize(String rawEmail) {
        if (rawEmail == null || rawEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        String email = rawEmail.trim().toLowerCase();
        if (!EMAIL.matcher(email).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid email address");
        }
        return email;
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is too long");
        }
    }

    private void ensureNotLocked(String email) {
        String failures = redisTemplate.opsForValue().get(FAIL_PREFIX + email);
        if (failures != null && Integer.parseInt(failures) >= MAX_FAILED_LOGINS) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed attempts. Try again in 15 minutes, or log in with an email code.");
        }
    }

    private void recordFailure(String email) {
        Long count = redisTemplate.opsForValue().increment(FAIL_PREFIX + email);
        if (count != null && count == 1) {
            redisTemplate.expire(FAIL_PREFIX + email, LOCKOUT);
        }
    }
}
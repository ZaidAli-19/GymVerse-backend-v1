package com.company.GymVerse.otp.serviceImpl;

import com.company.GymVerse.exceptions.otp.OtpExpiredException;
import com.company.GymVerse.exceptions.otp.OtpInvalidException;
import com.company.GymVerse.exceptions.otp.OtpRateLimitException;
import com.company.GymVerse.messaging.kafka.events.OtpEmailEvent;
import com.company.GymVerse.otp.config.OtpConfigProperties;
import com.company.GymVerse.otp.service.OtpService;
import com.company.GymVerse.otp.util.OtpPurpose;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Enterprise implementation of {@link OtpService} backed by Redis for distributed caching,
 * resend cooldown enforcement, brute-force attempt limits, and Kafka email dispatch.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OtpServiceImpl implements OtpService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OtpConfigProperties config;

    @Value("${gymverse.kafka.topics.otp-email:otp-email-topic}")
    private String otpEmailTopic;

    @Override
    public void sendOtp(String email, OtpPurpose purpose, String recipientName) {
        validateEmailAndPurpose(email, purpose);
        String normalizedEmail = email.trim().toLowerCase();

        // 1. Check Resend Cooldown
        String cooldownKey = buildCooldownKey(purpose, normalizedEmail);
        Boolean hasCooldown = redisTemplate.hasKey(cooldownKey);
        if (Boolean.TRUE.equals(hasCooldown)) {
            Long remainingSeconds = redisTemplate.getExpire(cooldownKey, TimeUnit.SECONDS);
            long waitTime = (remainingSeconds != null && remainingSeconds > 0)
                    ? remainingSeconds
                    : config.getResendCooldown().toSeconds();
            log.warn("Rate limit exceeded for email: [{}], purpose: [{}]. Remaining cooldown: {}s",
                    normalizedEmail, purpose, waitTime);
            throw new OtpRateLimitException("Please wait " + waitTime + " seconds before requesting a new OTP");
        }

        // 2. Generate secure numeric OTP
        String otp = generateNumericOtp(config.getLength());

        // 3. Store OTP code, cooldown, and reset attempt counter in Redis
        String codeKey = buildCodeKey(purpose, normalizedEmail);
        String attemptsKey = buildAttemptsKey(purpose, normalizedEmail);

        redisTemplate.opsForValue().set(codeKey, otp, config.getTtl());
        redisTemplate.opsForValue().set(cooldownKey, "1", config.getResendCooldown());
        redisTemplate.opsForValue().set(attemptsKey, "0", config.getTtl());

        log.info("Stored OTP for email: [{}], purpose: [{}] with TTL: {}s",
                normalizedEmail, purpose, config.getTtl().toSeconds());

        // 4. Publish Kafka event for asynchronous email delivery
        OtpEmailEvent event = OtpEmailEvent.builder()
                .email(normalizedEmail)
                .otp(otp)
                .purpose(purpose.name())
                .recipientName(recipientName)
                .timestamp(Instant.now())
                .build();

        kafkaTemplate.send(otpEmailTopic, event).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish OtpEmailEvent to Kafka topic [{}] for recipient: [{}]: {}",
                        otpEmailTopic, normalizedEmail, ex.getMessage(), ex);
            } else {
                log.info("Successfully published OtpEmailEvent to Kafka topic [{}] for recipient: [{}]",
                        otpEmailTopic, normalizedEmail);
            }
        });
    }

    @Override
    public void resendOtp(String email, OtpPurpose purpose, String recipientName) {
        sendOtp(email, purpose, recipientName);
    }

    @Override
    public boolean verifyOtp(String email, String otp, OtpPurpose purpose) {
        validateEmailAndPurpose(email, purpose);
        String normalizedEmail = email.trim().toLowerCase();

        String codeKey = buildCodeKey(purpose, normalizedEmail);
        String storedOtp = redisTemplate.opsForValue().get(codeKey);

        if (storedOtp == null) {
            log.warn("OTP verification failed: no active OTP found for email: [{}], purpose: [{}]",
                    normalizedEmail, purpose);
            throw new OtpExpiredException("OTP has expired or was not requested");
        }

        String attemptsKey = buildAttemptsKey(purpose, normalizedEmail);
        Long currentAttempts = redisTemplate.opsForValue().increment(attemptsKey);
        int attempts = (currentAttempts != null) ? currentAttempts.intValue() : 1;

        if (attempts > config.getMaxAttempts()) {
            redisTemplate.delete(codeKey);
            redisTemplate.delete(attemptsKey);
            log.warn("Maximum OTP verification attempts ({}) exceeded for email: [{}]",
                    config.getMaxAttempts(), normalizedEmail);
            throw new OtpInvalidException("Maximum verification attempts exceeded. Please request a new OTP");
        }

        if (otp == null || !storedOtp.equals(otp.trim())) {
            int remaining = Math.max(0, config.getMaxAttempts() - attempts);
            if (remaining == 0) {
                redisTemplate.delete(codeKey);
                redisTemplate.delete(attemptsKey);
                log.warn("Maximum OTP verification attempts reached on final incorrect attempt for email: [{}]",
                        normalizedEmail);
                throw new OtpInvalidException("Maximum verification attempts exceeded. Please request a new OTP");
            }
            log.warn("Invalid OTP entered for email: [{}]. Remaining attempts: {}", normalizedEmail, remaining);
            throw new OtpInvalidException("Invalid OTP code. " + remaining + " attempts remaining");
        }

        // OTP matches: clean up code and attempts, set verified token state
        redisTemplate.delete(codeKey);
        redisTemplate.delete(attemptsKey);

        String verifiedKey = buildVerifiedKey(purpose, normalizedEmail);
        redisTemplate.opsForValue().set(verifiedKey, "true", config.getVerifiedTtl());

        log.info("OTP verified successfully for email: [{}], purpose: [{}]", normalizedEmail, purpose);
        return true;
    }

    @Override
    public boolean isOtpVerified(String email, OtpPurpose purpose) {
        if (email == null || email.isBlank() || purpose == null) {
            return false;
        }
        String normalizedEmail = email.trim().toLowerCase();
        String verifiedKey = buildVerifiedKey(purpose, normalizedEmail);
        String val = redisTemplate.opsForValue().get(verifiedKey);
        return "true".equalsIgnoreCase(val);
    }

    @Override
    public void consumeVerifiedOtp(String email, OtpPurpose purpose) {
        if (email == null || email.isBlank() || purpose == null) {
            return;
        }
        String normalizedEmail = email.trim().toLowerCase();
        String verifiedKey = buildVerifiedKey(purpose, normalizedEmail);
        redisTemplate.delete(verifiedKey);
        log.info("Consumed verified OTP state for email: [{}], purpose: [{}]", normalizedEmail, purpose);
    }

    private void validateEmailAndPurpose(String email, OtpPurpose purpose) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email must not be null or blank");
        }
        if (purpose == null) {
            throw new IllegalArgumentException("OtpPurpose must not be null");
        }
    }

    private String generateNumericOtp(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(SECURE_RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    private String buildCodeKey(OtpPurpose purpose, String email) {
        return config.getPrefixCode() + purpose.name() + ":" + email;
    }

    private String buildCooldownKey(OtpPurpose purpose, String email) {
        return config.getPrefixCooldown() + purpose.name() + ":" + email;
    }

    private String buildAttemptsKey(OtpPurpose purpose, String email) {
        return config.getPrefixAttempts() + purpose.name() + ":" + email;
    }

    private String buildVerifiedKey(OtpPurpose purpose, String email) {
        return config.getPrefixVerified() + purpose.name() + ":" + email;
    }
}

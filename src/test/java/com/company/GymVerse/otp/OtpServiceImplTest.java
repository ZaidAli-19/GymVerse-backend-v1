package com.company.GymVerse.otp;

import com.company.GymVerse.exceptions.otp.OtpExpiredException;
import com.company.GymVerse.exceptions.otp.OtpInvalidException;
import com.company.GymVerse.exceptions.otp.OtpRateLimitException;
import com.company.GymVerse.messaging.kafka.events.OtpEmailEvent;
import com.company.GymVerse.otp.config.OtpConfigProperties;
import com.company.GymVerse.otp.serviceImpl.OtpServiceImpl;
import com.company.GymVerse.otp.util.OtpPurpose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceImplTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Spy
    private OtpConfigProperties config = OtpConfigProperties.builder()
            .length(6)
            .ttl(Duration.ofMinutes(10))
            .resendCooldown(Duration.ofSeconds(60))
            .maxAttempts(5)
            .verifiedTtl(Duration.ofMinutes(15))
            .prefixCode("gymverse:otp:code:")
            .prefixCooldown("gymverse:otp:cooldown:")
            .prefixAttempts("gymverse:otp:attempts:")
            .prefixVerified("gymverse:otp:verified:")
            .build();

    @InjectMocks
    private OtpServiceImpl otpService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(otpService, "otpEmailTopic", "otp-email-topic");
    }

    @Test
    @DisplayName("sendOtp generates secure OTP, persists in Redis, and publishes Kafka event")
    void sendOtp_WhenNoCooldown_SavesToRedisAndPublishesKafka() {
        when(redisTemplate.hasKey("gymverse:otp:cooldown:SIGNUP:user@example.com")).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(kafkaTemplate.send(eq("otp-email-topic"), any(OtpEmailEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        otpService.sendOtp("User@Example.com", OtpPurpose.SIGNUP, "Alex");

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq("gymverse:otp:code:SIGNUP:user@example.com"), otpCaptor.capture(), eq(Duration.ofMinutes(10)));
        verify(valueOperations).set("gymverse:otp:cooldown:SIGNUP:user@example.com", "1", Duration.ofSeconds(60));
        verify(valueOperations).set("gymverse:otp:attempts:SIGNUP:user@example.com", "0", Duration.ofMinutes(10));

        String generatedOtp = otpCaptor.getValue();
        assertThat(generatedOtp).hasSize(6).containsOnlyDigits();

        ArgumentCaptor<OtpEmailEvent> eventCaptor = ArgumentCaptor.forClass(OtpEmailEvent.class);
        verify(kafkaTemplate).send(eq("otp-email-topic"), eventCaptor.capture());
        OtpEmailEvent event = eventCaptor.getValue();
        assertThat(event.getEmail()).isEqualTo("user@example.com");
        assertThat(event.getOtp()).isEqualTo(generatedOtp);
        assertThat(event.getPurpose()).isEqualTo("SIGNUP");
        assertThat(event.getRecipientName()).isEqualTo("Alex");
    }

    @Test
    @DisplayName("sendOtp throws OtpRateLimitException when cooldown key exists")
    void sendOtp_WhenCooldownActive_ThrowsOtpRateLimitException() {
        when(redisTemplate.hasKey("gymverse:otp:cooldown:LOGIN:user@example.com")).thenReturn(true);
        when(redisTemplate.getExpire("gymverse:otp:cooldown:LOGIN:user@example.com", TimeUnit.SECONDS)).thenReturn(42L);

        OtpRateLimitException ex = assertThrows(OtpRateLimitException.class, () ->
                otpService.sendOtp("user@example.com", OtpPurpose.LOGIN, null)
        );

        assertThat(ex.getMessage()).contains("42 seconds");
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("resendOtp delegates to sendOtp and applies cooldown logic")
    void resendOtp_DelegatesToSendOtp() {
        when(redisTemplate.hasKey("gymverse:otp:cooldown:FORGOT_PASSWORD:user@example.com")).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(kafkaTemplate.send(eq("otp-email-topic"), any(OtpEmailEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        otpService.resendOtp("user@example.com", OtpPurpose.FORGOT_PASSWORD, "Sarah");

        verify(valueOperations).set(eq("gymverse:otp:code:FORGOT_PASSWORD:user@example.com"), anyString(), eq(Duration.ofMinutes(10)));
        verify(kafkaTemplate).send(eq("otp-email-topic"), any(OtpEmailEvent.class));
    }

    @Test
    @DisplayName("verifyOtp throws OtpExpiredException when no stored OTP exists")
    void verifyOtp_WhenNoStoredOtp_ThrowsOtpExpiredException() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("gymverse:otp:code:SIGNUP:user@example.com")).thenReturn(null);

        assertThrows(OtpExpiredException.class, () ->
                otpService.verifyOtp("user@example.com", "123456", OtpPurpose.SIGNUP)
        );
    }

    @Test
    @DisplayName("verifyOtp with correct code evicts code and attempts, and sets verified key")
    void verifyOtp_WhenOtpMatches_SetsVerifiedToken() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("gymverse:otp:code:SIGNUP:user@example.com")).thenReturn("482910");
        when(valueOperations.increment("gymverse:otp:attempts:SIGNUP:user@example.com")).thenReturn(1L);

        boolean result = otpService.verifyOtp("user@example.com", "482910", OtpPurpose.SIGNUP);

        assertThat(result).isTrue();
        verify(redisTemplate).delete("gymverse:otp:code:SIGNUP:user@example.com");
        verify(redisTemplate).delete("gymverse:otp:attempts:SIGNUP:user@example.com");
        verify(valueOperations).set("gymverse:otp:verified:SIGNUP:user@example.com", "true", Duration.ofMinutes(15));
    }

    @Test
    @DisplayName("verifyOtp with wrong code increments attempt counter and throws OtpInvalidException")
    void verifyOtp_WhenOtpMismatched_IncrementsAttemptsAndThrows() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("gymverse:otp:code:SIGNUP:user@example.com")).thenReturn("482910");
        when(valueOperations.increment("gymverse:otp:attempts:SIGNUP:user@example.com")).thenReturn(2L);

        OtpInvalidException ex = assertThrows(OtpInvalidException.class, () ->
                otpService.verifyOtp("user@example.com", "000000", OtpPurpose.SIGNUP)
        );

        assertThat(ex.getMessage()).contains("3 attempts remaining");
        verify(redisTemplate, never()).delete("gymverse:otp:code:SIGNUP:user@example.com");
    }

    @Test
    @DisplayName("verifyOtp evicts OTP when max attempts reached on 5th failed attempt")
    void verifyOtp_When5thAttemptFails_EvictsOtpAndLocksOut() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("gymverse:otp:code:SIGNUP:user@example.com")).thenReturn("482910");
        when(valueOperations.increment("gymverse:otp:attempts:SIGNUP:user@example.com")).thenReturn(5L);

        OtpInvalidException ex = assertThrows(OtpInvalidException.class, () ->
                otpService.verifyOtp("user@example.com", "000000", OtpPurpose.SIGNUP)
        );

        assertThat(ex.getMessage()).contains("Maximum verification attempts exceeded");
        verify(redisTemplate).delete("gymverse:otp:code:SIGNUP:user@example.com");
        verify(redisTemplate).delete("gymverse:otp:attempts:SIGNUP:user@example.com");
    }

    @Test
    @DisplayName("verifyOtp evicts OTP when attempts counter already exceeds max threshold")
    void verifyOtp_WhenAttemptsAlreadyExceeded_EvictsOtp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("gymverse:otp:code:SIGNUP:user@example.com")).thenReturn("482910");
        when(valueOperations.increment("gymverse:otp:attempts:SIGNUP:user@example.com")).thenReturn(6L);

        OtpInvalidException ex = assertThrows(OtpInvalidException.class, () ->
                otpService.verifyOtp("user@example.com", "482910", OtpPurpose.SIGNUP)
        );

        assertThat(ex.getMessage()).contains("Maximum verification attempts exceeded");
        verify(redisTemplate).delete("gymverse:otp:code:SIGNUP:user@example.com");
    }

    @Test
    @DisplayName("isOtpVerified returns true when verified token is present in Redis")
    void isOtpVerified_WhenPresent_ReturnsTrue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("gymverse:otp:verified:SIGNUP:user@example.com")).thenReturn("true");

        boolean verified = otpService.isOtpVerified("user@example.com", OtpPurpose.SIGNUP);

        assertThat(verified).isTrue();
    }

    @Test
    @DisplayName("isOtpVerified returns false when verified token is missing or not true")
    void isOtpVerified_WhenMissing_ReturnsFalse() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("gymverse:otp:verified:SIGNUP:user@example.com")).thenReturn(null);

        boolean verified = otpService.isOtpVerified("user@example.com", OtpPurpose.SIGNUP);

        assertThat(verified).isFalse();
    }

    @Test
    @DisplayName("consumeVerifiedOtp deletes the verified key from Redis")
    void consumeVerifiedOtp_DeletesKey() {
        otpService.consumeVerifiedOtp("user@example.com", OtpPurpose.SIGNUP);

        verify(redisTemplate).delete("gymverse:otp:verified:SIGNUP:user@example.com");
    }

    @Test
    @DisplayName("sendOtp and verifyOtp reject null or blank email")
    void methods_RejectNullOrBlankEmail() {
        assertThrows(IllegalArgumentException.class, () ->
                otpService.sendOtp("", OtpPurpose.SIGNUP, null)
        );

        assertThrows(IllegalArgumentException.class, () ->
                otpService.verifyOtp(null, "123456", OtpPurpose.SIGNUP)
        );
    }
}

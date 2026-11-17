package com.company.GymVerse.messaging;

import com.company.GymVerse.email.service.EmailService;
import com.company.GymVerse.messaging.kafka.events.OtpEmailEvent;
import com.company.GymVerse.messaging.kafka.listeners.OtpEmailListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpEmailListenerIntegrationTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private OtpEmailListener otpEmailListener;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("consumeOtpEmailEvent successfully delegates to EmailService with exact payload")
    void consumeOtpEmailEvent_DelegatesToEmailService() {
        OtpEmailEvent event = OtpEmailEvent.builder()
                .email("member@example.com")
                .otp("581290")
                .purpose("SIGNUP")
                .recipientName("John Doe")
                .timestamp(Instant.now())
                .build();

        otpEmailListener.consumeOtpEmailEvent(event);

        verify(emailService, times(1)).sendOtpEmail(
                "member@example.com",
                "581290",
                "SIGNUP",
                "John Doe"
        );
    }

    @Test
    @DisplayName("consumeOtpEmailEvent propagates exception when EmailService throws")
    void consumeOtpEmailEvent_WhenEmailServiceThrows_PropagatesException() {
        OtpEmailEvent event = OtpEmailEvent.builder()
                .email("fail@example.com")
                .otp("112233")
                .purpose("LOGIN")
                .recipientName("Jane")
                .build();

        doThrow(new IllegalStateException("SMTP failed"))
                .when(emailService).sendOtpEmail(anyString(), anyString(), anyString(), anyString());

        assertThrows(IllegalStateException.class, () ->
                otpEmailListener.consumeOtpEmailEvent(event)
        );

        verify(emailService, times(1)).sendOtpEmail(
                "fail@example.com",
                "112233",
                "LOGIN",
                "Jane"
        );
    }

    @Test
    @DisplayName("OtpEmailEvent serializes and deserializes accurately with Jackson")
    void otpEmailEvent_JsonSerializationCycle_PreservesAllFields() throws Exception {
        Instant now = Instant.parse("2026-11-17T12:00:00Z");
        OtpEmailEvent original = OtpEmailEvent.builder()
                .email("serialize@example.com")
                .otp("654321")
                .purpose("FORGOT_PASSWORD")
                .recipientName("Alice Smith")
                .timestamp(now)
                .build();

        String json = objectMapper.writeValueAsString(original);
        OtpEmailEvent deserialized = objectMapper.readValue(json, OtpEmailEvent.class);

        assertThat(deserialized.getEmail()).isEqualTo("serialize@example.com");
        assertThat(deserialized.getOtp()).isEqualTo("654321");
        assertThat(deserialized.getPurpose()).isEqualTo("FORGOT_PASSWORD");
        assertThat(deserialized.getRecipientName()).isEqualTo("Alice Smith");
        assertThat(deserialized.getTimestamp()).isEqualTo(now);
    }

    @Test
    @DisplayName("OtpEmailEvent toString masks OTP to prevent logging sensitive verification codes")
    void otpEmailEvent_ToString_MasksOtp() {
        OtpEmailEvent event = OtpEmailEvent.builder()
                .email("secure@example.com")
                .otp("889900")
                .purpose("SIGNUP")
                .recipientName("Secret Agent")
                .build();

        String stringRepresentation = event.toString();

        assertThat(stringRepresentation).doesNotContain("889900");
        assertThat(stringRepresentation).contains("[PROTECTED]");
        assertThat(stringRepresentation).contains("secure@example.com");
        assertThat(stringRepresentation).contains("SIGNUP");
    }
}

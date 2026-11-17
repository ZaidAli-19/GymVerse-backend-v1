package com.company.GymVerse.messaging.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * Event published to Kafka when an OTP verification email needs to be sent to a user.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpEmailEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String email;
    private String otp;
    private String purpose;
    private String recipientName;

    @Builder.Default
    private Instant timestamp = Instant.now();

    @Override
    public String toString() {
        return "OtpEmailEvent{" +
                "email='" + email + '\'' +
                ", otp='[PROTECTED]'" +
                ", purpose='" + purpose + '\'' +
                ", recipientName='" + recipientName + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}

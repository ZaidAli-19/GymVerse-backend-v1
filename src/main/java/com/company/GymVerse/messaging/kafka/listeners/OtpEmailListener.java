package com.company.GymVerse.messaging.kafka.listeners;

import com.company.GymVerse.email.service.EmailService;
import com.company.GymVerse.messaging.kafka.events.OtpEmailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka listener consuming {@link OtpEmailEvent} messages to asynchronously
 * dispatch verification emails via {@link EmailService}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OtpEmailListener {

    private final EmailService emailService;

    @KafkaListener(
            topics = "${gymverse.kafka.topics.otp-email:otp-email-topic}",
            groupId = "${gymverse.kafka.consumer.email-group:email-service-group}"
    )
    public void consumeOtpEmailEvent(OtpEmailEvent event) {
        log.info("Received OtpEmailEvent for recipient: [{}], purpose: [{}]", event.getEmail(), event.getPurpose());
        try {
            emailService.sendOtpEmail(
                    event.getEmail(),
                    event.getOtp(),
                    event.getPurpose(),
                    event.getRecipientName()
            );
            log.info("Successfully processed OTP email dispatch for recipient: [{}]", event.getEmail());
        } catch (Exception e) {
            log.error("Error processing OtpEmailEvent for recipient: [{}]: {}", event.getEmail(), e.getMessage(), e);
            throw e;
        }
    }
}

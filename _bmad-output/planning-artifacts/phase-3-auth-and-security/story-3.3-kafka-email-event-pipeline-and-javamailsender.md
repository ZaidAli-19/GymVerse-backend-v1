---
id: "story-3.3"
epic_id: "epic-phase-3-security-auth"
title: "Kafka Asynchronous Email Pipeline & JavaMailSender Integration"
status: "planning"
type: "asynchronous-messaging"
created_at: "2026-11-17"
dependencies: []
files_to_modify:
  - "src/main/resources/application.yaml"
  - "src/test/resources/application-test.yaml"
files_to_create:
  - "src/main/java/com/company/GymVerse/messaging/kafka/events/OtpEmailEvent.java"
  - "src/main/java/com/company/GymVerse/messaging/kafka/listeners/OtpEmailListener.java"
  - "src/main/java/com/company/GymVerse/email/service/EmailService.java"
  - "src/main/java/com/company/GymVerse/email/serviceImpl/EmailServiceImpl.java"
  - "src/test/java/com/company/GymVerse/email/EmailServiceImplTest.java"
  - "src/test/java/com/company/GymVerse/messaging/OtpEmailListenerIntegrationTest.java"
---

# Story 3.3: Kafka Asynchronous Email Pipeline & JavaMailSender Integration

## ⚠️ MANDATORY DIRECTIVES FOR IMPLEMENTING AGENTS
> 1. Strictly adhere to [`RULES.md`](file:///D:/projects/GymVerse-Backend-v2/RULES.md) and [`PROMPT.md`](file:///D:/projects/GymVerse-Backend-v2/PROMPT.md).
> 2. **NEVER** run `git commit` or `git push` autonomously.
> 3. **DO NOT** create manual `@Bean ProducerFactory` / `@Bean KafkaTemplate` configuration classes. Leverage Spring Boot's built-in Kafka auto-configuration driven via `application.yaml`.
> 4. Keep email sending decoupled: Operation services publish `OtpEmailEvent` to Kafka; `OtpEmailListener` consumes the event and calls `EmailService`.
> 5. Maintain task logging in `antigravity/story-3.3-kafka-email-event-pipeline-and-javamailsender.md`.

---

## 1. User Story

**As a** system architect,  
**I want** an asynchronous Kafka messaging pipeline that consumes OTP email events and delivers styled HTML verification emails via `JavaMailSender`,  
**So that** time-consuming SMTP socket connections are removed from the HTTP request-response cycle without requiring manual Kafka producer/listener boilerplate code.

---

## 2. Technical Context & Requirements

1. **Spring Boot Kafka Configuration (`application.yaml`)**:
   - Verify `spring.kafka` auto-configuration is active:
     ```yaml
     spring:
       kafka:
         bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
         producer:
           key-serializer: org.apache.kafka.common.serialization.StringSerializer
           value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
         consumer:
           group-id: gymverse-group
           auto-offset-reset: earliest
           key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
           value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
           properties:
             spring.json.trusted.packages: '*'
             spring.json.use.type.headers: true
     ```
   - No custom `@Configuration` bean for `KafkaTemplate` needed; Spring Boot provides `KafkaTemplate<String, Object>`.

2. **Event Payload Model (`com.company.GymVerse.messaging.kafka.events.OtpEmailEvent`)**:
   - Strongly-typed DTO:
     - `String email`
     - `String otp`
     - `String purpose` (e.g. `SIGNUP`, `LOGIN`, `FORGOT_PASSWORD`)
     - `String recipientName`
     - `Instant timestamp`
   - Default constructor, all-args constructor, getters/setters/builder.

3. **Email Service (`com.company.GymVerse.email.service.EmailService` & `EmailServiceImpl`)**:
   - Injects `JavaMailSender` and `@Value("${spring.mail.username}") private String fromEmail`.
   - `void sendOtpEmail(String toEmail, String otp, String purpose, String recipientName)`:
     - Uses `MimeMessageHelper(mimeMessage, true, "UTF-8")`.
     - Sets subject dynamically based on purpose:
       - `SIGNUP`: "GymVerse — Email Verification Code"
       - `LOGIN`: "GymVerse — Login Verification Code"
       - `FORGOT_PASSWORD`: "GymVerse — Password Reset Code"
     - Formats responsive HTML email template:
       - GymVerse branding (dark mode theme: `#0d0d0d`, silver/orange accents).
       - Prominent 6-digit verification code rendered inside distinct boxed table cells.
       - Clear instructions with 10-minute validity notice and security warning ("Do not share this code").

4. **Kafka Listener (`com.company.GymVerse.messaging.kafka.listeners.OtpEmailListener`)**:
   - Injects `EmailService`.
   - Annotated with `@KafkaListener(topics = "${gymverse.kafka.topics.otp-email:otp-email-topic}", groupId = "email-service-group")`.
   - Receives `OtpEmailEvent`, logs receipt safely (obfuscating OTP in production logs), and delegates to `emailService.sendOtpEmail(...)`.

---

## 3. Verification & Acceptance Criteria

1. **Email Service Tests (`EmailServiceImplTest.java`)**:
   - Mock `JavaMailSender` and verify `send(MimeMessage)` is invoked with valid recipient, subject, and HTML content containing individual OTP digits.
   - Verify proper error handling if `JavaMailSender` throws `MailException`.
2. **Listener & Messaging Tests (`OtpEmailListenerIntegrationTest.java`)**:
   - Publish `OtpEmailEvent` to `otp-email-topic` via `KafkaTemplate`.
   - Verify `@KafkaListener` consumes the event and triggers `emailService.sendOtpEmail(...)` with exact payload parameters.

package com.company.GymVerse.email;

import com.company.GymVerse.email.serviceImpl.EmailServiceImpl;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "security@gymverse.com");
    }

    @Test
    @DisplayName("sendOtpEmail with SIGNUP purpose creates valid HTML email with digit boxes and greeting")
    void sendOtpEmail_WhenSignup_SendsExpectedMimeMessage() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendOtpEmail("user@example.com", "482910", "SIGNUP", "Alex Walker");

        verify(mailSender, times(1)).send(mimeMessage);
        assertThat(mimeMessage.getAllRecipients()).hasSize(1);
        assertThat(mimeMessage.getAllRecipients()[0].toString()).isEqualTo("user@example.com");
        assertThat(mimeMessage.getSubject()).isEqualTo("GymVerse — Email Verification Code");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mimeMessage.writeTo(out);
        String rawContent = out.toString();

        assertThat(rawContent).contains("Alex Walker");
        assertThat(rawContent).contains("482910");
        assertThat(rawContent).contains("Email Verification");
        assertThat(rawContent).contains("Expires in 10 minutes");
    }

    @Test
    @DisplayName("sendOtpEmail with LOGIN purpose sets Login Verification subject")
    void sendOtpEmail_WhenLogin_SetsLoginSubject() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendOtpEmail("user@example.com", "123456", "LOGIN", null);

        verify(mailSender, times(1)).send(mimeMessage);
        assertThat(mimeMessage.getSubject()).isEqualTo("GymVerse — Login Verification Code");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mimeMessage.writeTo(out);
        String rawContent = out.toString();

        assertThat(rawContent).contains("123456");
        assertThat(rawContent).contains("Login Authentication");
    }

    @Test
    @DisplayName("sendOtpEmail with FORGOT_PASSWORD purpose sets Password Reset subject")
    void sendOtpEmail_WhenForgotPassword_SetsPasswordResetSubject() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendOtpEmail("user@example.com", "998877", "FORGOT_PASSWORD", "Sarah Connor");

        verify(mailSender, times(1)).send(mimeMessage);
        assertThat(mimeMessage.getSubject()).isEqualTo("GymVerse — Password Reset Code");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mimeMessage.writeTo(out);
        String rawContent = out.toString();

        assertThat(rawContent).contains("Sarah Connor");
        assertThat(rawContent).contains("998877");
        assertThat(rawContent).contains("Password Reset");
    }

    @Test
    @DisplayName("sendOtpEmail with unknown purpose falls back to default Verification subject")
    void sendOtpEmail_WhenUnknownPurpose_SetsDefaultSubject() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendOtpEmail("user@example.com", "654321", "CUSTOM", null);

        verify(mailSender, times(1)).send(mimeMessage);
        assertThat(mimeMessage.getSubject()).isEqualTo("GymVerse — Verification Code");
    }

    @Test
    @DisplayName("sendOtpEmail throws IllegalStateException when JavaMailSender fails")
    void sendOtpEmail_WhenMailSenderFails_ThrowsIllegalStateException() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("SMTP server connection refused"))
                .when(mailSender).send(any(MimeMessage.class));

        assertThrows(IllegalStateException.class, () ->
                emailService.sendOtpEmail("user@example.com", "123456", "SIGNUP", "Tester")
        );
    }
}

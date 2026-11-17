package com.company.GymVerse.email.serviceImpl;

import com.company.GymVerse.email.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

/**
 * Production implementation of {@link EmailService} delivering HTML verification emails
 * via Spring's {@link JavaMailSender}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Override
    public void sendOtpEmail(String toEmail, String otp, String purpose, String recipientName) {
        log.info("Preparing to send OTP email to recipient: [{}] for purpose: [{}]", toEmail, purpose);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            String sender = (fromEmail != null && !fromEmail.isBlank()) ? fromEmail : "no-reply@gymverse.com";
            helper.setFrom(sender, "GymVerse Security");
            helper.setTo(toEmail);

            String subject = resolveSubject(purpose);
            helper.setSubject(subject);

            String htmlContent = buildOtpTemplate(otp, purpose, recipientName);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("Successfully sent OTP email to recipient: [{}]", toEmail);

        } catch (MessagingException | UnsupportedEncodingException | MailException e) {
            log.error("Failed to send OTP email to [{}]: {}", toEmail, e.getMessage(), e);
            throw new IllegalStateException("Failed to send OTP verification email", e);
        }
    }

    private String resolveSubject(String purpose) {
        if (purpose == null) {
            return "GymVerse — Verification Code";
        }
        return switch (purpose.trim().toUpperCase()) {
            case "SIGNUP", "VERIFY" -> "GymVerse — Email Verification Code";
            case "LOGIN" -> "GymVerse — Login Verification Code";
            case "FORGOT_PASSWORD", "RESET_PASSWORD" -> "GymVerse — Password Reset Code";
            default -> "GymVerse — Verification Code";
        };
    }

    private String resolveSubtitle(String purpose) {
        if (purpose == null) {
            return "Identity Verification";
        }
        return switch (purpose.trim().toUpperCase()) {
            case "SIGNUP", "VERIFY" -> "Email Verification";
            case "LOGIN" -> "Login Authentication";
            case "FORGOT_PASSWORD", "RESET_PASSWORD" -> "Password Reset";
            default -> "Identity Verification";
        };
    }

    private String resolveBodyDescription(String purpose) {
        if (purpose == null) {
            return "A one-time verification code has been dispatched to this address for your GymVerse account.";
        }
        return switch (purpose.trim().toUpperCase()) {
            case "SIGNUP", "VERIFY" -> "Welcome to <strong style=\"color:#e5e7eb;\">GymVerse</strong>. A one-time verification code has been dispatched to this address. Use the sequence below to complete your registration and activate your account.";
            case "LOGIN" -> "A login attempt was initiated for your <strong style=\"color:#e5e7eb;\">GymVerse</strong> account. Use the one-time code below to authenticate your session.";
            case "FORGOT_PASSWORD", "RESET_PASSWORD" -> "A password reset request was received for your <strong style=\"color:#e5e7eb;\">GymVerse</strong> account. Use the one-time code below to securely reset your credentials.";
            default -> "A one-time verification code has been dispatched to this address for your GymVerse account.";
        };
    }

    private String buildDigitBoxes(String otp) {
        if (otp == null) {
            return "";
        }
        String[] digits = otp.split("");
        StringBuilder digitBoxes = new StringBuilder();
        for (String d : digits) {
            digitBoxes.append(
                    "<td style=\"width:44px;height:52px;" +
                            "background:#0d0d0d;" +
                            "border:1px solid #2a2a2a;" +
                            "text-align:center;vertical-align:middle;" +
                            "font-family:'Courier New',monospace;" +
                            "font-size:24px;font-weight:bold;" +
                            "color:#ffffff;padding:0;\">" + d + "</td>" +
                            "<td style=\"width:8px;\"></td>"
            );
        }
        return digitBoxes.toString();
    }

    private String buildGreeting(String recipientName) {
        if (recipientName != null && !recipientName.isBlank()) {
            return "<p style=\"margin:0 0 16px;font-size:15px;color:#e5e7eb;font-weight:600;\">Hello "
                    + escapeHtml(recipientName) + ",</p>";
        }
        return "";
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String buildOtpTemplate(String otp, String purpose, String recipientName) {
        String digitBoxes = buildDigitBoxes(otp);
        String greeting = buildGreeting(recipientName);
        String subtitle = resolveSubtitle(purpose);
        String description = resolveBodyDescription(purpose);
        String safeOtp = otp != null ? otp : "";

        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width,initial-scale=1.0">
            <title>GymVerse Verification</title>
            <style type="text/css">
                @media only screen and (max-width: 600px) {
                    .em-inner { width: 100% !important; }
                    .em-pad { padding: 24px 20px !important; }
                    .em-header-pad { padding: 24px 20px 20px !important; }
                    .em-footer-pad { padding: 20px !important; }
                }
            </style>
        </head>
        <body style="margin:0;padding:0;background:#e5e7eb;font-family:Arial,sans-serif;">

        <table width="100%" cellpadding="0" cellspacing="0" border="0"
               style="background:#e5e7eb;padding:32px 16px;">
        <tr><td align="center">

        <table class="em-inner" cellpadding="0" cellspacing="0" border="0"
               style="width:100%;max-width:560px;background:#0d0d0d;border:1px solid #2a2a2a;">

            <!-- Top accent bar -->
            <tr><td style="height:3px;background:#374151;font-size:0;">&nbsp;</td></tr>

            <!-- Header -->
            <tr><td class="em-header-pad"
                    style="padding:32px 40px 28px;border-bottom:1px solid #1f1f1f;">
                <table cellpadding="0" cellspacing="0" border="0">
                <tr>
                    <td style="padding-right:10px;">
                        <table cellpadding="0" cellspacing="0" border="0">
                        <tr><td style="width:28px;height:28px;border:2px solid #ffffff;
                                       text-align:center;vertical-align:middle;">
                            <div style="width:12px;height:12px;background:#ffffff;margin:auto;"></div>
                        </td></tr>
                        </table>
                    </td>
                    <td style="font-size:18px;font-weight:700;color:#ffffff;
                               letter-spacing:4px;text-transform:uppercase;
                               font-family:Arial,sans-serif;">
                        GYMVERSE
                    </td>
                </tr>
                </table>
                <p style="margin:20px 0 4px;font-size:10px;color:#4b5563;
                          letter-spacing:3px;text-transform:uppercase;">
                    System Transmission
                </p>
                <p style="margin:0;font-size:10px;color:#374151;
                          letter-spacing:2px;text-transform:uppercase;">
                    Fitness Business Operating System — Secure Channel
                </p>
            </td></tr>

            <!-- Body -->
            <tr><td class="em-pad" style="padding:40px 40px 32px;">

                <p style="margin:0 0 4px;font-size:10px;color:#4b5563;
                          letter-spacing:3px;text-transform:uppercase;">
                    Protocol
                </p>
                <h1 style="margin:0 0 6px;font-size:28px;font-weight:700;
                           color:#ffffff;letter-spacing:2px;text-transform:uppercase;">
                    {{SUBTITLE}}
                </h1>
                <p style="margin:0 0 28px;font-size:10px;color:#4b5563;
                          letter-spacing:2px;text-transform:uppercase;">
                    Transmission sent. Enter the 6-digit code.
                </p>

                {{GREETING}}

                <p style="margin:0 0 32px;font-size:14px;color:#9ca3af;
                          line-height:1.8;letter-spacing:0.5px;">
                    {{DESCRIPTION}}
                </p>

                <!-- OTP Block -->
                <table width="100%" cellpadding="0" cellspacing="0" border="0"
                       style="background:#111111;border:1px solid #2a2a2a;
                              border-left:3px solid #ffffff;margin-bottom:28px;">
                <tr><td style="padding:28px 28px 16px;">
                    <p style="margin:0 0 14px;font-size:10px;color:#4b5563;
                              letter-spacing:3px;text-transform:uppercase;">
                        Authorization Code
                    </p>
                    <table cellpadding="0" cellspacing="0" border="0"
                           style="border-collapse:separate;border-spacing:0;">
                    <tr>{{DIGITS}}</tr>
                    </table>
                    <p style="margin:16px 0 0;font-size:11px;color:#4b5563;
                              letter-spacing:2px;text-transform:uppercase;">
                        <span style="display:inline-block;width:6px;height:6px;
                                     background:#16a34a;border-radius:50%;
                                     vertical-align:middle;margin-right:8px;"></span>
                        Active &nbsp;·&nbsp; Expires in 10 minutes
                    </p>
                </td></tr>

                <!-- Plain text copy line -->
                <tr><td style="padding:0 28px 28px;border-top:1px solid #1f1f1f;">
                    <p style="margin:16px 0 4px;font-size:10px;color:#4b5563;
                              letter-spacing:2px;text-transform:uppercase;">
                        Or copy your code
                    </p>
                    <p style="margin:0;font-family:'Courier New',monospace;
                              font-size:22px;font-weight:bold;color:#ffffff;
                              letter-spacing:8px;">
                        {{OTP}}
                    </p>
                </td></tr>
                </table>

                <!-- Security Warning -->
                <table width="100%" cellpadding="0" cellspacing="0" border="0"
                       style="background:#111111;border:1px solid #1f1f1f;
                              border-left:3px solid #374151;">
                <tr><td style="padding:14px 18px;">
                    <p style="margin:0;font-size:12px;color:#6b7280;
                              line-height:1.7;letter-spacing:1px;">
                        If you did not initiate this request,
                        <strong style="color:#9ca3af;">disregard this transmission.</strong>
                        Your account remains secure.
                        Do not share this code with anyone.
                    </p>
                </td></tr>
                </table>

            </td></tr>

            <!-- Footer -->
            <tr><td class="em-footer-pad"
                    style="padding:24px 40px;border-top:1px solid #1a1a1a;background:#080808;">
                <table width="100%" cellpadding="0" cellspacing="0" border="0">
                <tr>
                    <td>
                        <p style="margin:0 0 4px;font-size:11px;color:#374151;
                                  letter-spacing:2px;text-transform:uppercase;">
                            © 2026 GymVerse
                        </p>
                        <p style="margin:0;font-size:10px;color:#1f2937;
                                  letter-spacing:1px;text-transform:uppercase;">
                            All rights reserved.
                        </p>
                    </td>
                    <td align="right">
                        <p style="margin:0;font-size:10px;color:#1f2937;
                                  letter-spacing:1px;text-transform:uppercase;">
                            Automated message<br>Do not reply
                        </p>
                    </td>
                </tr>
                </table>
            </td></tr>

        </table>

        </td></tr>
        </table>

        </body>
        </html>
        """
                .replace("{{SUBTITLE}}", subtitle)
                .replace("{{GREETING}}", greeting)
                .replace("{{DESCRIPTION}}", description)
                .replace("{{DIGITS}}", digitBoxes)
                .replace("{{OTP}}", safeOtp);
    }
}

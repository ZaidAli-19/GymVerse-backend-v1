package com.company.GymVerse.otp.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Externalized configuration properties for the Redis-backed OTP engine.
 */
@Component
@ConfigurationProperties(prefix = "gymverse.otp")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpConfigProperties {

    @Builder.Default
    private int length = 6;

    @Builder.Default
    private Duration ttl = Duration.ofMinutes(10);

    @Builder.Default
    private Duration resendCooldown = Duration.ofSeconds(60);

    @Builder.Default
    private int maxAttempts = 5;

    @Builder.Default
    private Duration verifiedTtl = Duration.ofMinutes(15);

    @Builder.Default
    private String prefixCode = "gymverse:otp:code:";

    @Builder.Default
    private String prefixCooldown = "gymverse:otp:cooldown:";

    @Builder.Default
    private String prefixAttempts = "gymverse:otp:attempts:";

    @Builder.Default
    private String prefixVerified = "gymverse:otp:verified:";
}

package com.compute.rental.modules.auth.support;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        Duration emailCodeTtl,
        Integer emailCodeLength,
        Integer emailCodeRateLimitPerMinute,
        String emailFrom,
        String emailSubject
) {

    public int codeLength() {
        return emailCodeLength == null ? 6 : emailCodeLength;
    }

    public int rateLimitPerMinute() {
        return emailCodeRateLimitPerMinute == null ? 5 : emailCodeRateLimitPerMinute;
    }

    public Duration codeTtl() {
        return emailCodeTtl == null ? Duration.ofMinutes(5) : emailCodeTtl;
    }

    public String from() {
        return emailFrom;
    }

    public String subject() {
        return emailSubject == null || emailSubject.isBlank()
                ? "Compute Rental login verification code"
                : emailSubject;
    }
}

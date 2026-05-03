package com.compute.rental.modules.order.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.api-token")
public record ApiTokenProperties(
        String encryptionSecret,
        String baseUrl
) {
}

package com.compute.rental.config;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ProductionConfigValidator implements SmartInitializingSingleton {

    private static final int MIN_SECRET_BYTES = 32;

    private final Environment environment;

    public ProductionConfigValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterSingletonsInstantiated() {
        validate();
    }

    void validate() {
        if (!productionEnforced()) {
            return;
        }

        var errors = new ArrayList<String>();
        var allowLocalInfrastructure = booleanProperty("app.production.allow-local-infrastructure", false);

        validateSecret(errors, "app.jwt.secret", "JWT secret");
        validateSecret(errors, "app.api-token.encryption-secret", "API token encryption secret");

        validateRequired(errors, "spring.datasource.url", "Datasource URL");
        validateRequired(errors, "spring.datasource.username", "Datasource username");
        validateRequired(errors, "spring.datasource.password", "Datasource password");
        validateLocalValue(errors, "spring.datasource.url", "Datasource URL", allowLocalInfrastructure);

        validateRequired(errors, "spring.data.redis.host", "Redis host");
        validateLocalValue(errors, "spring.data.redis.host", "Redis host", allowLocalInfrastructure);
        if (!booleanProperty("app.production.allow-empty-redis-password", false)) {
            validateRequired(errors, "spring.data.redis.password", "Redis password");
        }

        validateRequired(errors, "spring.rabbitmq.host", "RabbitMQ host");
        validateRequired(errors, "spring.rabbitmq.username", "RabbitMQ username");
        validateRequired(errors, "spring.rabbitmq.password", "RabbitMQ password");
        validateLocalValue(errors, "spring.rabbitmq.host", "RabbitMQ host", allowLocalInfrastructure);
        validateRabbitGuestUser(errors);

        validateRequired(errors, "spring.mail.username", "Mail username");
        validateRequired(errors, "spring.mail.password", "Mail password");
        validateWebSocketOrigins(errors);
        validateApiDocs(errors);

        if (!errors.isEmpty()) {
            throw new IllegalStateException("Unsafe production configuration: " + String.join("; ", errors));
        }
    }

    private boolean productionEnforced() {
        if (booleanProperty("app.production.enforce", false)) {
            return true;
        }
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile));
    }

    private void validateSecret(ArrayList<String> errors, String key, String label) {
        var value = property(key);
        if (!StringUtils.hasText(value)) {
            errors.add(label + " must be configured");
            return;
        }
        if (value.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            errors.add(label + " must be at least " + MIN_SECRET_BYTES + " bytes");
        }
        var normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.contains("dev-only") || normalized.contains("change-me") || normalized.contains("example")) {
            errors.add(label + " must not use development placeholder values");
        }
    }

    private void validateRequired(ArrayList<String> errors, String key, String label) {
        if (!StringUtils.hasText(property(key))) {
            errors.add(label + " must be configured");
        }
    }

    private void validateLocalValue(ArrayList<String> errors, String key, String label, boolean allowed) {
        if (allowed) {
            return;
        }
        var value = property(key);
        if (!StringUtils.hasText(value)) {
            return;
        }
        var normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.contains("localhost")
                || normalized.contains("127.0.0.1")
                || normalized.contains("[::1]")
                || normalized.contains("host.docker.internal")) {
            errors.add(label + " must not point to local infrastructure in production");
        }
    }

    private void validateRabbitGuestUser(ArrayList<String> errors) {
        if (booleanProperty("app.production.allow-rabbit-guest-user", false)) {
            return;
        }
        if ("guest".equals(property("spring.rabbitmq.username")) || "guest".equals(property("spring.rabbitmq.password"))) {
            errors.add("RabbitMQ guest credentials must not be used in production");
        }
    }

    private void validateWebSocketOrigins(ArrayList<String> errors) {
        var origins = property("app.websocket.allowed-origins");
        if (!StringUtils.hasText(origins)) {
            errors.add("WebSocket allowed origins must be configured");
            return;
        }
        if (origins.contains("*") && !booleanProperty("app.production.allow-wildcard-websocket-origins", false)) {
            errors.add("WebSocket wildcard origins must not be used in production");
        }
    }

    private void validateApiDocs(ArrayList<String> errors) {
        if (booleanProperty("app.production.allow-api-docs", false)) {
            return;
        }
        if (booleanProperty("knife4j.enable", true)
                || booleanProperty("springdoc.api-docs.enabled", true)
                || booleanProperty("springdoc.swagger-ui.enabled", true)) {
            errors.add("API docs must be disabled in production");
        }
    }

    private boolean booleanProperty(String key, boolean defaultValue) {
        return environment.getProperty(key, Boolean.class, defaultValue);
    }

    private String property(String key) {
        var value = environment.getProperty(key);
        return value == null ? null : value.trim();
    }
}

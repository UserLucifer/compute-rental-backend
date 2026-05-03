package com.compute.rental.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionConfigValidatorTest {

    @Test
    void shouldSkipValidationWhenProductionIsNotEnforced() {
        var environment = new MockEnvironment()
                .withProperty("app.jwt.secret", "dev-only-change-me-32-bytes-minimum-secret");

        assertThatCode(() -> new ProductionConfigValidator(environment).validate())
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectDevelopmentSecretsAndLocalInfrastructureWhenProductionIsEnforced() {
        var environment = new MockEnvironment()
                .withProperty("app.production.enforce", "true")
                .withProperty("app.jwt.secret", "dev-only-change-me-32-bytes-minimum-secret")
                .withProperty("app.api-token.encryption-secret", "dev-only-token-secret-change-me")
                .withProperty("spring.datasource.url", "jdbc:mysql://localhost:3306/compute_rental")
                .withProperty("spring.datasource.username", "compute")
                .withProperty("spring.datasource.password", "")
                .withProperty("spring.data.redis.host", "localhost")
                .withProperty("spring.data.redis.password", "")
                .withProperty("spring.rabbitmq.host", "localhost")
                .withProperty("spring.rabbitmq.username", "guest")
                .withProperty("spring.rabbitmq.password", "guest")
                .withProperty("spring.mail.username", "")
                .withProperty("spring.mail.password", "")
                .withProperty("app.websocket.allowed-origins", "*")
                .withProperty("knife4j.enable", "true")
                .withProperty("springdoc.api-docs.enabled", "true")
                .withProperty("springdoc.swagger-ui.enabled", "true");

        assertThatThrownBy(() -> new ProductionConfigValidator(environment).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unsafe production configuration")
                .hasMessageContaining("JWT secret")
                .hasMessageContaining("Datasource password")
                .hasMessageContaining("RabbitMQ guest credentials")
                .hasMessageContaining("API docs must be disabled");
    }

    @Test
    void shouldAcceptCompleteProductionConfiguration() {
        var environment = new MockEnvironment()
                .withProperty("app.production.enforce", "true")
                .withProperty("app.jwt.secret", "prod-jwt-secret-32-bytes-minimum-value")
                .withProperty("app.api-token.encryption-secret", "prod-api-token-secret-32-bytes-value")
                .withProperty("spring.datasource.url", "jdbc:mysql://mysql.internal:3306/compute_rental")
                .withProperty("spring.datasource.username", "compute_prod")
                .withProperty("spring.datasource.password", "db-prod-password")
                .withProperty("spring.data.redis.host", "redis.internal")
                .withProperty("spring.data.redis.password", "redis-prod-password")
                .withProperty("spring.rabbitmq.host", "rabbitmq.internal")
                .withProperty("spring.rabbitmq.username", "compute_prod")
                .withProperty("spring.rabbitmq.password", "rabbit-prod-password")
                .withProperty("spring.mail.username", "mail-user")
                .withProperty("spring.mail.password", "mail-password")
                .withProperty("app.websocket.allowed-origins", "https://admin.example.com,https://app.example.com")
                .withProperty("knife4j.enable", "false")
                .withProperty("springdoc.api-docs.enabled", "false")
                .withProperty("springdoc.swagger-ui.enabled", "false");

        assertThatCode(() -> new ProductionConfigValidator(environment).validate())
                .doesNotThrowAnyException();
    }
}

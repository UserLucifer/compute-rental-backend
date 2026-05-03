package com.compute.rental.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.rabbitmq.client.ConnectionFactory;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.mail.MessagingException;
import java.io.File;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.util.StringUtils;

class InfrastructureConnectionIT {

    private static final Duration CONNECT_TIMEOUT = infrastructureTestTimeout();

    @Test
    void redisShouldRespondToPing() {
        var properties = applicationProperties();
        var host = property(properties, "REDIS_HOST", "spring.data.redis.host", "localhost");
        var port = integerProperty(properties, "REDIS_PORT", "spring.data.redis.port", "6379");
        var database = integerProperty(properties, "REDIS_DATABASE", "spring.data.redis.database", "0");
        var password = property(properties, "REDIS_PASSWORD", "spring.data.redis.password", "");

        LettuceConnectionFactory connectionFactory = null;
        try {
            var redisConfig = new RedisStandaloneConfiguration(host, port);
            redisConfig.setDatabase(database);
            if (StringUtils.hasText(password)) {
                redisConfig.setPassword(RedisPassword.of(password));
            }
            var clientConfig = LettuceClientConfiguration.builder()
                    .commandTimeout(CONNECT_TIMEOUT)
                    .shutdownTimeout(Duration.ZERO)
                    .build();
            connectionFactory = new LettuceConnectionFactory(redisConfig, clientConfig);
            connectionFactory.setValidateConnection(true);
            connectionFactory.afterPropertiesSet();

            var redisTemplate = new StringRedisTemplate(connectionFactory);
            redisTemplate.afterPropertiesSet();

            assertThat(redisTemplate.execute((RedisCallback<String>) connection -> connection.ping()))
                    .isEqualTo("PONG");
        } finally {
            if (connectionFactory != null) {
                connectionFactory.destroy();
            }
        }
    }

    @Test
    void mysqlShouldExecuteSelectOne() throws SQLException {
        var properties = applicationProperties();
        var jdbcUrl = requiredProperty(properties, "DB_URL", "spring.datasource.url");
        var username = requiredProperty(properties, "DB_USERNAME", "spring.datasource.username");
        var password = requiredProperty(properties, "DB_PASSWORD", "spring.datasource.password");

        try (var dataSource = new HikariDataSource()) {
            dataSource.setJdbcUrl(jdbcUrl);
            dataSource.setUsername(username);
            dataSource.setPassword(password);
            dataSource.setConnectionTimeout(CONNECT_TIMEOUT.toMillis());
            dataSource.setValidationTimeout(CONNECT_TIMEOUT.toMillis());
            dataSource.setMaximumPoolSize(1);
            dataSource.setMinimumIdle(0);
            dataSource.setPoolName("ComputeRentalInfrastructureIT");

            try (var connection = dataSource.getConnection();
                 var statement = connection.createStatement();
                 var resultSet = statement.executeQuery("SELECT 1")) {
                assertThat(connection.isValid((int) CONNECT_TIMEOUT.toSeconds())).isTrue();
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt(1)).isEqualTo(1);
            }
        }
    }

    @Test
    void rabbitMqShouldOpenConnection() throws Exception {
        var properties = applicationProperties();
        var factory = new ConnectionFactory();
        factory.setHost(property(properties, "RABBITMQ_HOST", "spring.rabbitmq.host", "localhost"));
        factory.setPort(integerProperty(properties, "RABBITMQ_PORT", "spring.rabbitmq.port", "5672"));
        factory.setUsername(requiredProperty(properties, "RABBITMQ_USERNAME", "spring.rabbitmq.username"));
        factory.setPassword(requiredProperty(properties, "RABBITMQ_PASSWORD", "spring.rabbitmq.password"));
        factory.setVirtualHost(property(properties, "RABBITMQ_VIRTUAL_HOST", "spring.rabbitmq.virtual-host", "/"));
        factory.setConnectionTimeout((int) CONNECT_TIMEOUT.toMillis());
        factory.setHandshakeTimeout((int) CONNECT_TIMEOUT.toMillis());
        factory.setRequestedHeartbeat((int) CONNECT_TIMEOUT.toSeconds());

        try (var connection = factory.newConnection("compute-rental-infrastructure-it")) {
            assertThat(connection.isOpen()).isTrue();
        } catch (TimeoutException ex) {
            throw new IllegalStateException("RabbitMQ connection timed out", ex);
        }
    }

    @Test
    void mailShouldAuthenticateSmtpConnection() throws MessagingException {
        var properties = applicationProperties();
        var mailSender = new JavaMailSenderImpl();
        mailSender.setHost(property(properties, "MAIL_HOST", "spring.mail.host", "smtp.qq.com"));
        mailSender.setPort(integerProperty(properties, "MAIL_PORT", "spring.mail.port", "465"));
        mailSender.setUsername(requiredProperty(properties, "MAIL_USERNAME", "spring.mail.username"));
        mailSender.setPassword(requiredProperty(properties, "MAIL_PASSWORD", "spring.mail.password"));
        mailSender.setProtocol(property(properties, "MAIL_PROTOCOL", "spring.mail.protocol", "smtp"));
        mailSender.setDefaultEncoding("UTF-8");

        var javaMailProperties = mailSender.getJavaMailProperties();
        javaMailProperties.put("mail.smtp.auth", property(
                properties, "MAIL_SMTP_AUTH", "spring.mail.properties.mail.smtp.auth", "true"));
        javaMailProperties.put("mail.smtp.ssl.enable", property(
                properties, "MAIL_SMTP_SSL_ENABLE", "spring.mail.properties.mail.smtp.ssl.enable", "true"));
        javaMailProperties.put("mail.smtp.starttls.enable", property(
                properties, "MAIL_SMTP_STARTTLS_ENABLE", "spring.mail.properties.mail.smtp.starttls.enable", "false"));
        javaMailProperties.put("mail.smtp.connectiontimeout", String.valueOf(CONNECT_TIMEOUT.toMillis()));
        javaMailProperties.put("mail.smtp.timeout", String.valueOf(CONNECT_TIMEOUT.toMillis()));
        javaMailProperties.put("mail.smtp.writetimeout", String.valueOf(CONNECT_TIMEOUT.toMillis()));

        mailSender.testConnection();
    }

    private Properties applicationProperties() {
        var properties = new Properties();
        properties.putAll(loadYaml(new ClassPathResource("application.yml")));
        var localConfig = new File("application-local.yml");
        if (localConfig.exists()) {
            properties.putAll(loadYaml(new FileSystemResource(localConfig)));
        }
        return properties;
    }

    private Properties loadYaml(Resource resource) {
        var factory = new YamlPropertiesFactoryBean();
        factory.setResources(resource);
        var properties = factory.getObject();
        return properties == null ? new Properties() : properties;
    }

    private int integerProperty(Properties properties, String envName, String propertyName, String defaultValue) {
        var value = property(properties, envName, propertyName, defaultValue);
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Missing infrastructure test configuration: " + propertyName);
        }
        return Integer.parseInt(value);
    }

    private String requiredProperty(Properties properties, String envName, String propertyName) {
        var value = property(properties, envName, propertyName, "");
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Missing infrastructure test configuration: " + propertyName);
        }
        return value;
    }

    private String property(Properties properties, String envName, String propertyName, String defaultValue) {
        var envValue = System.getenv(envName);
        if (StringUtils.hasText(envValue)) {
            return envValue.trim();
        }
        var systemEnvValue = System.getProperty(envName);
        if (StringUtils.hasText(systemEnvValue)) {
            return systemEnvValue.trim();
        }
        var systemPropertyValue = System.getProperty(propertyName);
        if (StringUtils.hasText(systemPropertyValue)) {
            return systemPropertyValue.trim();
        }
        var propertyValue = properties.get(propertyName);
        if (propertyValue != null && StringUtils.hasText(String.valueOf(propertyValue))) {
            var resolvedValue = resolvePlaceholder(String.valueOf(propertyValue));
            return StringUtils.hasText(resolvedValue) ? resolvedValue : defaultValue;
        }
        return defaultValue;
    }

    private String resolvePlaceholder(String value) {
        var trimmed = value.trim();
        if (!trimmed.startsWith("${") || !trimmed.endsWith("}")) {
            return trimmed;
        }

        var expression = trimmed.substring(2, trimmed.length() - 1);
        var separatorIndex = expression.indexOf(':');
        var placeholderName = separatorIndex < 0 ? expression : expression.substring(0, separatorIndex);
        var defaultValue = separatorIndex < 0 ? "" : expression.substring(separatorIndex + 1);
        var envValue = System.getenv(placeholderName);
        if (StringUtils.hasText(envValue)) {
            return envValue.trim();
        }
        var systemValue = System.getProperty(placeholderName);
        if (StringUtils.hasText(systemValue)) {
            return systemValue.trim();
        }
        return defaultValue;
    }

    private static Duration infrastructureTestTimeout() {
        var timeout = System.getenv("INFRASTRUCTURE_TEST_TIMEOUT_SECONDS");
        if (!StringUtils.hasText(timeout)) {
            timeout = System.getProperty("INFRASTRUCTURE_TEST_TIMEOUT_SECONDS");
        }
        if (!StringUtils.hasText(timeout)) {
            return Duration.ofSeconds(15);
        }
        return Duration.ofSeconds(Long.parseLong(timeout));
    }
}

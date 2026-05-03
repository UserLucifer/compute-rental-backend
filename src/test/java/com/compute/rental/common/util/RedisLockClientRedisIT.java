package com.compute.rental.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.time.Duration;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

class RedisLockClientRedisIT {

    private static final String LOCK_KEY = RedisKeys.LOCK_PREFIX + "it:redis-lock-client";

    private StringRedisTemplate redisTemplate;
    private LettuceConnectionFactory connectionFactory;

    @BeforeEach
    void setUp() {
        redisTemplate = realRedisTemplate();
        assertThat(redisTemplate.execute((RedisCallback<String>) connection -> connection.ping())).isEqualTo("PONG");
        redisTemplate.delete(LOCK_KEY);
    }

    @AfterEach
    void tearDown() {
        if (redisTemplate != null) {
            redisTemplate.delete(LOCK_KEY);
        }
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void shouldAcquireRejectConcurrentAndReleaseWithOwnerTokenAgainstRealRedis() {
        var client = new RedisLockClient(redisTemplate);

        var firstLock = client.tryLock(LOCK_KEY, Duration.ofSeconds(30));
        var secondLock = client.tryLock(LOCK_KEY, Duration.ofSeconds(30));

        assertThat(firstLock).isPresent();
        assertThat(secondLock).isEmpty();

        client.unlock(new RedisLockClient.RedisLock(LOCK_KEY, "wrong-token"));
        assertThat(redisTemplate.hasKey(LOCK_KEY)).isTrue();

        client.unlock(firstLock.get());
        assertThat(redisTemplate.hasKey(LOCK_KEY)).isFalse();
        assertThat(client.tryLock(LOCK_KEY, Duration.ofSeconds(30))).isPresent();
    }

    private StringRedisTemplate realRedisTemplate() {
        var properties = redisProperties();
        var host = property(properties, "REDIS_HOST", "spring.data.redis.host", "localhost");
        var port = Integer.parseInt(property(properties, "REDIS_PORT", "spring.data.redis.port", "6379"));
        var database = Integer.parseInt(property(properties, "REDIS_DATABASE", "spring.data.redis.database", "0"));
        var password = property(properties, "REDIS_PASSWORD", "spring.data.redis.password", "");

        var redisConfig = new RedisStandaloneConfiguration(host, port);
        redisConfig.setDatabase(database);
        if (StringUtils.hasText(password)) {
            redisConfig.setPassword(RedisPassword.of(password));
        }
        var clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(3))
                .shutdownTimeout(Duration.ZERO)
                .build();
        connectionFactory = new LettuceConnectionFactory(redisConfig, clientConfig);
        connectionFactory.setValidateConnection(true);
        connectionFactory.afterPropertiesSet();

        var template = new StringRedisTemplate(connectionFactory);
        template.afterPropertiesSet();
        return template;
    }

    private Properties redisProperties() {
        var properties = new Properties();
        properties.putAll(loadYaml(new ClassPathResource("application.yml")));
        var localConfig = new File("application-local.yml");
        if (localConfig.exists()) {
            properties.putAll(loadYaml(new FileSystemResource(localConfig)));
        }
        return properties;
    }

    private Properties loadYaml(org.springframework.core.io.Resource resource) {
        var factory = new YamlPropertiesFactoryBean();
        factory.setResources(resource);
        var properties = factory.getObject();
        return properties == null ? new Properties() : properties;
    }

    private String property(Properties properties, String envName, String propertyName, String defaultValue) {
        var envValue = System.getenv(envName);
        if (StringUtils.hasText(envValue)) {
            return envValue;
        }
        var propertyValue = properties.get(propertyName);
        if (propertyValue != null && StringUtils.hasText(String.valueOf(propertyValue))) {
            return String.valueOf(propertyValue);
        }
        return defaultValue;
    }
}

package com.compute.rental.modules.system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import com.compute.rental.common.util.RedisKeys;
import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

class RedisCacheAdminServiceRedisIT {

    private static final String TEST_CACHE_PREFIX = RedisKeys.CACHE_PREFIX + "it:admin-clear:";
    private static final String TEST_EMAIL_PREFIX = RedisKeys.EMAIL_CODE_PREFIX + "it:admin-clear:";
    private static final String CACHE_KEY = TEST_CACHE_PREFIX + "catalog";
    private static final String EMAIL_KEY = TEST_EMAIL_PREFIX + "rate";
    private static final String LOCK_KEY = RedisKeys.LOCK_PREFIX + "it:admin-clear";
    private static final String SCHEDULER_LOCK_KEY = "scheduler:it:admin-clear:lock";
    private static final String OUTSIDE_KEY = "compute-rental:it:admin-clear:outside";

    private StringRedisTemplate redisTemplate;
    private LettuceConnectionFactory connectionFactory;
    private boolean redisReady;

    @BeforeEach
    void setUp() {
        redisReady = false;
        redisTemplate = realRedisTemplate();
        assertThat(redisTemplate.execute((RedisCallback<String>) connection -> connection.ping())).isEqualTo("PONG");
        redisReady = true;
        cleanupTestKeys();
        assertNoNonTestKeys(RedisKeys.CACHE_PREFIX + "*", TEST_CACHE_PREFIX);
        assertNoNonTestKeys(RedisKeys.EMAIL_CODE_PREFIX + "*", TEST_EMAIL_PREFIX);
    }

    @AfterEach
    void tearDown() {
        if (redisReady) {
            cleanupTestKeys();
        }
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void shouldClearWhitelistedCacheAndEmailKeysButKeepLocksAgainstRealRedis() {
        redisTemplate.opsForValue().set(CACHE_KEY, "cache");
        redisTemplate.opsForValue().set(EMAIL_KEY, "email");
        redisTemplate.opsForValue().set(LOCK_KEY, "lock");
        redisTemplate.opsForValue().set(SCHEDULER_LOCK_KEY, "scheduler-lock");
        redisTemplate.opsForValue().set(OUTSIDE_KEY, "outside");

        var result = new RedisCacheAdminService(redisTemplate).clearApplicationCache();

        assertThat(result.deletedCount()).isEqualTo(2);
        assertThat(result.prefixes()).containsExactly(RedisKeys.CACHE_PREFIX, RedisKeys.EMAIL_CODE_PREFIX);
        assertThat(redisTemplate.hasKey(CACHE_KEY)).isFalse();
        assertThat(redisTemplate.hasKey(EMAIL_KEY)).isFalse();
        assertThat(redisTemplate.opsForValue().get(LOCK_KEY)).isEqualTo("lock");
        assertThat(redisTemplate.opsForValue().get(SCHEDULER_LOCK_KEY)).isEqualTo("scheduler-lock");
        assertThat(redisTemplate.opsForValue().get(OUTSIDE_KEY)).isEqualTo("outside");
    }

    private void assertNoNonTestKeys(String pattern, String allowedPrefix) {
        var nonTestKeys = scanKeys(pattern).stream()
                .filter(key -> !key.startsWith(allowedPrefix))
                .toList();
        if (!nonTestKeys.isEmpty()) {
            fail("Refuse to run real Redis clear test because non-test keys exist for pattern "
                    + pattern + ": " + nonTestKeys);
        }
    }

    private void cleanupTestKeys() {
        var keys = new ArrayList<String>();
        keys.addAll(scanKeys(TEST_CACHE_PREFIX + "*"));
        keys.addAll(scanKeys(TEST_EMAIL_PREFIX + "*"));
        keys.add(LOCK_KEY);
        keys.add(SCHEDULER_LOCK_KEY);
        keys.add(OUTSIDE_KEY);
        redisTemplate.delete(keys);
    }

    private List<String> scanKeys(String pattern) {
        var keys = new ArrayList<String>();
        var options = ScanOptions.scanOptions()
                .match(pattern)
                .count(100)
                .build();
        try (var cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        }
        return keys;
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

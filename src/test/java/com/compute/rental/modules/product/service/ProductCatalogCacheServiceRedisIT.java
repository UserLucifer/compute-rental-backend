package com.compute.rental.modules.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import com.compute.rental.common.util.RedisKeys;
import com.compute.rental.modules.product.dto.RegionResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

class ProductCatalogCacheServiceRedisIT {

    private static final String TEST_CATALOG_PREFIX = RedisKeys.CATALOG_CACHE_PREFIX + "it:product-catalog-cache:";
    private static final String OUTSIDE_KEY = RedisKeys.CACHE_PREFIX + "it:product-catalog-cache:outside";

    private ProductCatalogCacheService service;
    private StringRedisTemplate redisTemplate;
    private LettuceConnectionFactory connectionFactory;
    private boolean redisReady;

    @BeforeEach
    void setUp() {
        redisReady = false;
        redisTemplate = realRedisTemplate();
        service = new ProductCatalogCacheService(redisTemplate, new ObjectMapper());
        assertThat(redisTemplate.execute((RedisCallback<String>) connection -> connection.ping())).isEqualTo("PONG");
        redisReady = true;
        cleanupTestKeys();
        assertNoNonTestCatalogKeys();
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
    void shouldRoundTripAndEvictCatalogCacheWithRealRedis() {
        var catalogKey = TEST_CATALOG_PREFIX + "regions";
        var cachedRegions = List.of(new RegionResponse(1L, "HK", "Hong Kong"));
        service.put(catalogKey, cachedRegions);

        var result = service.get(catalogKey, new TypeReference<List<RegionResponse>>() {
        });

        assertThat(result).containsExactly(new RegionResponse(1L, "HK", "Hong Kong"));
        assertThat(redisTemplate.getExpire(catalogKey)).isPositive();

        redisTemplate.opsForValue().set(OUTSIDE_KEY, "keep");
        service.evictCatalog();

        assertThat(redisTemplate.hasKey(catalogKey)).isFalse();
        assertThat(redisTemplate.opsForValue().get(OUTSIDE_KEY)).isEqualTo("keep");
    }

    private void assertNoNonTestCatalogKeys() {
        var nonTestKeys = scanKeys(RedisKeys.CATALOG_CACHE_PREFIX + "*").stream()
                .filter(key -> !key.startsWith(TEST_CATALOG_PREFIX))
                .toList();
        if (!nonTestKeys.isEmpty()) {
            fail("Refuse to run real Redis catalog eviction test because non-test catalog keys exist: " + nonTestKeys);
        }
    }

    private void cleanupTestKeys() {
        var keys = scanKeys(TEST_CATALOG_PREFIX + "*");
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

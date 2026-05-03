package com.compute.rental.modules.product.service;

import com.compute.rental.common.util.RedisKeys;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ProductCatalogCacheService {

    private static final Logger log = LoggerFactory.getLogger(ProductCatalogCacheService.class);
    private static final Duration CATALOG_CACHE_TTL = Duration.ofMinutes(5);
    private static final int SCAN_COUNT = 500;
    private static final int DELETE_BATCH_SIZE = 500;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ProductCatalogCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public <T> T get(String key, TypeReference<T> typeReference) {
        try {
            var json = redisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(json)) {
                return null;
            }
            return objectMapper.readValue(json, typeReference);
        } catch (RuntimeException | JsonProcessingException ex) {
            log.warn("Read product catalog cache failed, key={}", key, ex);
            return null;
        }
    }

    public void put(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), CATALOG_CACHE_TTL);
        } catch (RuntimeException | JsonProcessingException ex) {
            log.warn("Write product catalog cache failed, key={}", key, ex);
        }
    }

    public void evictCatalog() {
        var batch = new ArrayList<String>(DELETE_BATCH_SIZE);
        var options = ScanOptions.scanOptions()
                .match(RedisKeys.CATALOG_CACHE_PREFIX + "*")
                .count(SCAN_COUNT)
                .build();
        try (var cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                batch.add(cursor.next());
                if (batch.size() >= DELETE_BATCH_SIZE) {
                    deleteBatch(batch);
                }
            }
            deleteBatch(batch);
        } catch (RuntimeException ex) {
            log.warn("Evict product catalog cache failed", ex);
        }
    }

    private void deleteBatch(List<String> keys) {
        if (keys.isEmpty()) {
            return;
        }
        redisTemplate.delete(keys);
        keys.clear();
    }
}

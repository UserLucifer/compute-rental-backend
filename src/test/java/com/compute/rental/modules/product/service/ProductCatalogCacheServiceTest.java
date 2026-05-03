package com.compute.rental.modules.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.compute.rental.common.util.RedisKeys;
import com.compute.rental.modules.product.dto.RegionResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class ProductCatalogCacheServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private Cursor<String> cursor;

    private ProductCatalogCacheService service;

    @BeforeEach
    void setUp() {
        service = new ProductCatalogCacheService(redisTemplate, new ObjectMapper());
    }

    @Test
    void getShouldDeserializeCachedJson() {
        var key = RedisKeys.catalogRegions();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn("[{\"id\":1,\"regionCode\":\"HK\",\"regionName\":\"Hong Kong\"}]");

        var result = service.get(key, new TypeReference<List<RegionResponse>>() {
        });

        assertThat(result).containsExactly(new RegionResponse(1L, "HK", "Hong Kong"));
    }

    @Test
    void getShouldReturnNullWhenRedisFails() {
        var key = RedisKeys.catalogRegions();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenThrow(new IllegalStateException("redis down"));

        var result = service.get(key, new TypeReference<List<RegionResponse>>() {
        });

        assertThat(result).isNull();
    }

    @Test
    void putShouldSerializeValueWithTtl() {
        var key = RedisKeys.catalogRegions();
        var value = List.of(new RegionResponse(1L, "HK", "Hong Kong"));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        service.put(key, value);

        verify(valueOperations).set(eq(key), eq("[{\"id\":1,\"regionCode\":\"HK\",\"regionName\":\"Hong Kong\"}]"),
                eq(Duration.ofMinutes(5)));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void evictCatalogShouldDeleteOnlyCatalogPrefix() {
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(true, false);
        when(cursor.next()).thenReturn("compute-rental:cache:catalog:regions");

        service.evictCatalog();

        var optionsCaptor = ArgumentCaptor.forClass(ScanOptions.class);
        verify(redisTemplate).scan(optionsCaptor.capture());
        assertThat(new String(optionsCaptor.getValue().getBytePattern(), StandardCharsets.UTF_8))
                .isEqualTo("compute-rental:cache:catalog:*");
        verify(redisTemplate).delete(any(Collection.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void evictCatalogShouldSkipDeleteWhenNoKeysMatch() {
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(false);

        service.evictCatalog();

        verify(redisTemplate, never()).delete(any(Collection.class));
    }
}

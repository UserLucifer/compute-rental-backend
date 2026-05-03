package com.compute.rental.modules.system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class RedisCacheAdminServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private Cursor<String> cacheCursor;

    @Mock
    private Cursor<String> rateLimitCursor;

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void clearApplicationCacheDeletesOnlyWhitelistedPrefixes() {
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cacheCursor, rateLimitCursor);
        when(cacheCursor.hasNext()).thenReturn(true, false);
        when(cacheCursor.next()).thenReturn("compute-rental:cache:products");
        when(rateLimitCursor.hasNext()).thenReturn(true, false);
        when(rateLimitCursor.next()).thenReturn("compute-rental:email-code:rate:test@example.com:SIGNUP");
        when(redisTemplate.delete(any(Collection.class))).thenReturn(1L, 1L);

        var result = new RedisCacheAdminService(redisTemplate).clearApplicationCache();

        assertThat(result.deletedCount()).isEqualTo(2);
        assertThat(result.prefixes()).containsExactly("compute-rental:cache:", "compute-rental:email-code:");

        var optionsCaptor = ArgumentCaptor.forClass(ScanOptions.class);
        verify(redisTemplate, org.mockito.Mockito.times(2)).scan(optionsCaptor.capture());
        var patterns = optionsCaptor.getAllValues().stream()
                .map(options -> new String(options.getBytePattern(), StandardCharsets.UTF_8))
                .toList();
        assertThat(patterns).containsExactly("compute-rental:cache:*", "compute-rental:email-code:*");
        assertThat(patterns).noneMatch(pattern -> pattern.startsWith("scheduler:"));
    }

    @Test
    void clearApplicationCacheDoesNotDeleteWhenNoKeysMatch() {
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cacheCursor, rateLimitCursor);
        when(cacheCursor.hasNext()).thenReturn(false);
        when(rateLimitCursor.hasNext()).thenReturn(false);

        var result = new RedisCacheAdminService(redisTemplate).clearApplicationCache();

        assertThat(result.deletedCount()).isZero();
        verify(redisTemplate, never()).delete(any(Collection.class));
    }
}

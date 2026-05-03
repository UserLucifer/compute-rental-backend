package com.compute.rental.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@ExtendWith(MockitoExtension.class)
class RedisLockClientTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    void tryLockShouldReturnLockWhenRedisSetIfAbsentSucceeds() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("lock:key"), any(), eq(Duration.ofSeconds(30)))).thenReturn(true);

        var lock = new RedisLockClient(redisTemplate).tryLock("lock:key", Duration.ofSeconds(30));

        assertThat(lock).isPresent();
        assertThat(lock.get().key()).isEqualTo("lock:key");
        assertThat(lock.get().value()).isNotBlank();
    }

    @Test
    void tryLockShouldReturnEmptyWhenLockAlreadyExists() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("lock:key"), any(), eq(Duration.ofSeconds(30)))).thenReturn(false);

        var lock = new RedisLockClient(redisTemplate).tryLock("lock:key", Duration.ofSeconds(30));

        assertThat(lock).isEmpty();
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void unlockShouldUseOwnerTokenLuaScript() {
        var lock = new RedisLockClient.RedisLock("lock:key", "owner-token");

        new RedisLockClient(redisTemplate).unlock(lock);

        verify(redisTemplate).execute(any(DefaultRedisScript.class), eq(List.of("lock:key")), eq("owner-token"));
    }
}

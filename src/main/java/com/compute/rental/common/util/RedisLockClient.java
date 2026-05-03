package com.compute.rental.common.util;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisLockClient {

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    private final StringRedisTemplate redisTemplate;

    public RedisLockClient(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Optional<RedisLock> tryLock(String key, Duration ttl) {
        var value = UUID.randomUUID().toString();
        var locked = redisTemplate.opsForValue().setIfAbsent(key, value, ttl);
        if (Boolean.TRUE.equals(locked)) {
            return Optional.of(new RedisLock(key, value));
        }
        return Optional.empty();
    }

    public void unlock(RedisLock lock) {
        redisTemplate.execute(RELEASE_SCRIPT, Collections.singletonList(lock.key()), lock.value());
    }

    public record RedisLock(String key, String value) {
    }
}

package com.compute.rental.scheduler;

import com.compute.rental.common.util.RedisLockClient;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SchedulerLockTemplate {

    private static final Logger log = LoggerFactory.getLogger(SchedulerLockTemplate.class);

    private final RedisLockClient redisLockClient;

    public SchedulerLockTemplate(RedisLockClient redisLockClient) {
        this.redisLockClient = redisLockClient;
    }

    public boolean runWithLock(String lockKey, Duration ttl, Runnable task) {
        var lock = redisLockClient.tryLock(lockKey, ttl);
        if (lock.isEmpty()) {
            log.info("Scheduler lock skipped, key={}", lockKey);
            return false;
        }
        try {
            task.run();
            return true;
        } finally {
            redisLockClient.unlock(lock.get());
        }
    }
}

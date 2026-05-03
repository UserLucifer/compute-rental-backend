package com.compute.rental.modules.system.service;

import com.compute.rental.common.util.DateTimeUtils;
import com.compute.rental.common.util.RedisKeys;
import com.compute.rental.modules.system.dto.RedisCacheClearResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisCacheAdminService {

    private static final int SCAN_COUNT = 500;
    private static final int DELETE_BATCH_SIZE = 500;

    private final StringRedisTemplate redisTemplate;

    public RedisCacheAdminService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public RedisCacheClearResponse clearApplicationCache() {
        var deletedCount = 0L;
        for (var prefix : RedisKeys.ADMIN_CLEARABLE_PREFIXES) {
            deletedCount += deleteByPrefix(prefix);
        }
        return new RedisCacheClearResponse(
                deletedCount,
                RedisKeys.ADMIN_CLEARABLE_PREFIXES,
                DateTimeUtils.now()
        );
    }

    private long deleteByPrefix(String prefix) {
        var deletedCount = 0L;
        var batch = new ArrayList<String>(DELETE_BATCH_SIZE);
        var options = ScanOptions.scanOptions()
                .match(prefix + "*")
                .count(SCAN_COUNT)
                .build();
        try (var cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                batch.add(cursor.next());
                if (batch.size() >= DELETE_BATCH_SIZE) {
                    deletedCount += deleteBatch(batch);
                }
            }
        }
        deletedCount += deleteBatch(batch);
        return deletedCount;
    }

    private long deleteBatch(List<String> keys) {
        if (keys.isEmpty()) {
            return 0L;
        }
        var deleted = redisTemplate.delete(keys);
        keys.clear();
        return deleted == null ? 0L : deleted;
    }
}

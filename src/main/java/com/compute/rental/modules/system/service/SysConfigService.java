package com.compute.rental.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.common.util.DateTimeUtils;
import com.compute.rental.common.util.RedisKeys;
import com.compute.rental.modules.system.dto.AdminSysConfigQueryRequest;
import com.compute.rental.modules.system.dto.AdminSysConfigResponse;
import com.compute.rental.modules.system.dto.UpdateSysConfigRequest;
import com.compute.rental.modules.system.entity.SysConfig;
import com.compute.rental.modules.system.mapper.SysConfigMapper;
import java.math.BigDecimal;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
public class SysConfigService {

    private static final Logger log = LoggerFactory.getLogger(SysConfigService.class);
    private static final Duration CONFIG_CACHE_TTL = Duration.ofMinutes(10);

    private final SysConfigMapper sysConfigMapper;
    private final AdminLogService adminLogService;
    private final StringRedisTemplate redisTemplate;

    public SysConfigService(
            SysConfigMapper sysConfigMapper,
            AdminLogService adminLogService,
            StringRedisTemplate redisTemplate
    ) {
        this.sysConfigMapper = sysConfigMapper;
        this.adminLogService = adminLogService;
        this.redisTemplate = redisTemplate;
    }

    public String getString(String key) {
        return getString(key, SysConfigDefaults.defaultValue(key));
    }

    public String getString(String key, String defaultValue) {
        var cachedValue = getCachedValue(key);
        if (StringUtils.hasText(cachedValue)) {
            return cachedValue;
        }
        var config = sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, key)
                .last("LIMIT 1"));
        if (config != null && StringUtils.hasText(config.getConfigValue())) {
            cacheValue(key, config.getConfigValue());
            return config.getConfigValue();
        }
        if (defaultValue != null) {
            return defaultValue;
        }
        throw new BusinessException(ErrorCode.SYS_CONFIG_MISSING, "缺少系统配置：" + key);
    }

    public BigDecimal getBigDecimal(String key) {
        return new BigDecimal(getString(key));
    }

    public BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
        var value = getString(key, defaultValue == null ? null : defaultValue.toPlainString());
        return new BigDecimal(value);
    }

    public Integer getInteger(String key) {
        return Integer.valueOf(getString(key));
    }

    public Integer getInteger(String key, Integer defaultValue) {
        var value = getString(key, defaultValue == null ? null : String.valueOf(defaultValue));
        return Integer.valueOf(value);
    }

    public Boolean getBoolean(String key) {
        return Boolean.valueOf(getString(key));
    }

    public Boolean getBoolean(String key, Boolean defaultValue) {
        var value = getString(key, defaultValue == null ? null : String.valueOf(defaultValue));
        return Boolean.valueOf(value);
    }

    public PageResult<AdminSysConfigResponse> pageAdminConfigs(AdminSysConfigQueryRequest request) {
        var page = new Page<SysConfig>(request.current(), request.size());
        var wrapper = new LambdaQueryWrapper<SysConfig>()
                .like(StringUtils.hasText(request.configKey()), SysConfig::getConfigKey,
                        request.configKey() == null ? null : request.configKey().trim())
                .orderByAsc(SysConfig::getConfigKey);
        var result = sysConfigMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getRecords().stream().map(this::toAdminResponse).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    public AdminSysConfigResponse getAdminConfig(String configKey) {
        return toAdminResponse(requireConfig(configKey));
    }

    @Transactional
    public AdminSysConfigResponse updateAdminConfig(String configKey, UpdateSysConfigRequest request,
                                                    Long adminId, String ip) {
        var config = requireConfig(configKey);
        var before = snapshot(config);
        var now = DateTimeUtils.now();
        evictConfigCache(configKey);
        var updated = sysConfigMapper.update(null, new LambdaUpdateWrapper<SysConfig>()
                .eq(SysConfig::getId, config.getId())
                .set(SysConfig::getConfigValue, request.configValue().trim())
                .set(SysConfig::getConfigDesc, trimToNull(request.configDesc()))
                .set(SysConfig::getUpdatedAt, now));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.SYS_CONFIG_UPDATE_FAILED);
        }
        var after = sysConfigMapper.selectById(config.getId());
        evictConfigCacheAfterCommit(configKey);
        adminLogService.log(adminId, AdminLogService.UPDATE_SYS_CONFIG, "sys_config", config.getId(),
                before, snapshot(after), "Update config " + config.getConfigKey(), ip);
        return toAdminResponse(after);
    }

    private String getCachedValue(String key) {
        try {
            return redisTemplate.opsForValue().get(RedisKeys.sysConfig(key));
        } catch (RuntimeException ex) {
            log.warn("Read sys_config cache failed, key={}", key, ex);
            return null;
        }
    }

    private void cacheValue(String key, String value) {
        try {
            redisTemplate.opsForValue().set(RedisKeys.sysConfig(key), value, CONFIG_CACHE_TTL);
        } catch (RuntimeException ex) {
            log.warn("Write sys_config cache failed, key={}", key, ex);
        }
    }

    private void evictConfigCache(String key) {
        try {
            redisTemplate.delete(RedisKeys.sysConfig(key));
        } catch (RuntimeException ex) {
            log.warn("Evict sys_config cache failed, key={}", key, ex);
        }
    }

    private void evictConfigCacheAfterCommit(String key) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            evictConfigCache(key);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                evictConfigCache(key);
            }
        });
    }

    private SysConfig requireConfig(String configKey) {
        var config = sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, configKey)
                .last("LIMIT 1"));
        if (config == null) {
            throw new BusinessException(ErrorCode.SYS_CONFIG_NOT_FOUND);
        }
        return config;
    }

    private AdminSysConfigResponse toAdminResponse(SysConfig config) {
        return new AdminSysConfigResponse(
                config.getId(),
                config.getConfigKey(),
                config.getConfigValue(),
                config.getConfigDesc(),
                config.getCreatedAt(),
                config.getUpdatedAt()
        );
    }

    private String snapshot(SysConfig config) {
        if (config == null) {
            return null;
        }
        return "{\"configKey\":\"" + escape(config.getConfigKey()) + "\",\"configValue\":\""
                + escape(config.getConfigValue()) + "\",\"configDesc\":\"" + escape(config.getConfigDesc()) + "\"}";
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}

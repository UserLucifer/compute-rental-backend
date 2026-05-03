package com.compute.rental.modules.system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.compute.rental.common.util.RedisKeys;
import com.compute.rental.modules.system.dto.UpdateSysConfigRequest;
import com.compute.rental.modules.system.entity.SysConfig;
import com.compute.rental.modules.system.mapper.SysConfigMapper;
import java.time.Duration;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class SysConfigAdminServiceTest {

    @Mock
    private SysConfigMapper sysConfigMapper;

    @Mock
    private AdminLogService adminLogService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        var configuration = new Configuration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), SysConfig.class);
    }

    @Test
    void updateAdminConfigShouldWriteAdminLog() {
        var service = new SysConfigService(sysConfigMapper, adminLogService, redisTemplate);
        var before = config("withdraw.min_amount", "10", "old");
        var after = config("withdraw.min_amount", "20", "new");
        when(sysConfigMapper.selectOne(any(Wrapper.class))).thenReturn(before);
        when(sysConfigMapper.update(any(), any(Wrapper.class))).thenReturn(1);
        when(sysConfigMapper.selectById(1L)).thenReturn(after);

        var response = service.updateAdminConfig("withdraw.min_amount",
                new UpdateSysConfigRequest("20", "new"), 9L, "127.0.0.1");

        assertThat(response.configValue()).isEqualTo("20");
        verify(adminLogService).log(eq(9L), eq(AdminLogService.UPDATE_SYS_CONFIG), eq("sys_config"),
                eq(1L), any(), any(), eq("Update config withdraw.min_amount"), eq("127.0.0.1"));
        verify(redisTemplate, org.mockito.Mockito.times(2)).delete(RedisKeys.sysConfig("withdraw.min_amount"));
    }

    @Test
    void updateAdminConfigShouldEvictAgainAfterTransactionCommit() {
        var service = new SysConfigService(sysConfigMapper, adminLogService, redisTemplate);
        var before = config("withdraw.min_amount", "10", "old");
        var after = config("withdraw.min_amount", "20", "new");
        when(sysConfigMapper.selectOne(any(Wrapper.class))).thenReturn(before);
        when(sysConfigMapper.update(any(), any(Wrapper.class))).thenReturn(1);
        when(sysConfigMapper.selectById(1L)).thenReturn(after);

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.updateAdminConfig("withdraw.min_amount",
                    new UpdateSysConfigRequest("20", "new"), 9L, "127.0.0.1");

            verify(redisTemplate).delete(RedisKeys.sysConfig("withdraw.min_amount"));
            var synchronizations = TransactionSynchronizationManager.getSynchronizations();
            assertThat(synchronizations).hasSize(1);
            synchronizations.get(0).afterCommit();
            verify(redisTemplate, org.mockito.Mockito.times(2)).delete(RedisKeys.sysConfig("withdraw.min_amount"));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void getStringShouldReturnCachedValueWithoutQueryingDatabase() {
        var service = new SysConfigService(sysConfigMapper, adminLogService, redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisKeys.sysConfig("withdraw.min_amount"))).thenReturn("10");

        var value = service.getString("withdraw.min_amount", "5");

        assertThat(value).isEqualTo("10");
        verify(sysConfigMapper, never()).selectOne(any(Wrapper.class));
    }

    @Test
    void getStringShouldCacheDatabaseValueOnMiss() {
        var service = new SysConfigService(sysConfigMapper, adminLogService, redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisKeys.sysConfig("withdraw.min_amount"))).thenReturn(null);
        when(sysConfigMapper.selectOne(any(Wrapper.class))).thenReturn(config("withdraw.min_amount", "10", "desc"));

        var value = service.getString("withdraw.min_amount", "5");

        assertThat(value).isEqualTo("10");
        verify(valueOperations).set(eq(RedisKeys.sysConfig("withdraw.min_amount")), eq("10"), any(Duration.class));
    }

    @Test
    void getStringShouldFallBackToDatabaseWhenRedisReadFails() {
        var service = new SysConfigService(sysConfigMapper, adminLogService, redisTemplate);
        when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis down"));
        when(sysConfigMapper.selectOne(any(Wrapper.class))).thenReturn(config("withdraw.min_amount", "10", "desc"));

        var value = service.getString("withdraw.min_amount", "5");

        assertThat(value).isEqualTo("10");
        verify(sysConfigMapper).selectOne(any(Wrapper.class));
    }

    @Test
    void getStringShouldUseDefaultValueWhenDbValueMissingAndShouldNotCacheDefault() {
        var service = new SysConfigService(sysConfigMapper, adminLogService, redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisKeys.sysConfig("withdraw.min_amount"))).thenReturn(null);
        when(sysConfigMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        var value = service.getString("withdraw.min_amount", "5");

        assertThat(value).isEqualTo("5");
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    private SysConfig config(String key, String value, String desc) {
        var config = new SysConfig();
        config.setId(1L);
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setConfigDesc(desc);
        return config;
    }
}

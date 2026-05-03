package com.compute.rental.modules.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.compute.rental.modules.user.dto.RegisterPushDeviceRequest;
import com.compute.rental.modules.user.entity.UserPushDevice;
import com.compute.rental.modules.user.mapper.UserPushDeviceMapper;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PushDeviceServiceTest {

    @Mock
    private UserPushDeviceMapper userPushDeviceMapper;

    private PushDeviceService service;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new Configuration(), ""), UserPushDevice.class);
    }

    @BeforeEach
    void setUp() {
        service = new PushDeviceService(userPushDeviceMapper);
    }

    @Test
    void registerShouldReturnMaskedDeviceToken() {
        when(userPushDeviceMapper.selectOne(any())).thenReturn(null);
        var response = service.register(10L, new RegisterPushDeviceRequest("IOS", "push-token-123456"));

        assertThat(response.userId()).isEqualTo(10L);
        assertThat(response.deviceType()).isEqualTo("IOS");
        assertThat(response.deviceTokenMasked()).isEqualTo("****3456");
    }

    @Test
    void listShouldNotExposeRawDeviceToken() {
        var device = new UserPushDevice();
        device.setId(20L);
        device.setUserId(10L);
        device.setDeviceType("ANDROID");
        device.setDeviceToken("raw-device-token");

        when(userPushDeviceMapper.selectList(any())).thenReturn(List.of(device));

        var result = service.list(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(20L);
        assertThat(result.get(0).deviceTokenMasked()).isEqualTo("****oken");
        assertThat(result.get(0).deviceTokenMasked()).doesNotContain("raw-device-token");
    }
}

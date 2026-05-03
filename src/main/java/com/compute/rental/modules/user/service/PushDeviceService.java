package com.compute.rental.modules.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.compute.rental.common.enums.CommonStatus;
import com.compute.rental.common.util.DateTimeUtils;
import com.compute.rental.modules.user.dto.PushDeviceResponse;
import com.compute.rental.modules.user.dto.RegisterPushDeviceRequest;
import com.compute.rental.modules.user.entity.UserPushDevice;
import com.compute.rental.modules.user.mapper.UserPushDeviceMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PushDeviceService {

    private final UserPushDeviceMapper userPushDeviceMapper;

    public PushDeviceService(UserPushDeviceMapper userPushDeviceMapper) {
        this.userPushDeviceMapper = userPushDeviceMapper;
    }

    @Transactional
    public PushDeviceResponse register(Long userId, RegisterPushDeviceRequest request) {
        var now = DateTimeUtils.now();
        var existing = userPushDeviceMapper.selectOne(new LambdaQueryWrapper<UserPushDevice>()
                .eq(UserPushDevice::getDeviceToken, request.deviceToken())
                .last("LIMIT 1"));
        if (existing != null) {
            userPushDeviceMapper.update(null, new LambdaUpdateWrapper<UserPushDevice>()
                    .eq(UserPushDevice::getId, existing.getId())
                    .set(UserPushDevice::getUserId, userId)
                    .set(UserPushDevice::getDeviceType, request.deviceType())
                    .set(UserPushDevice::getStatus, CommonStatus.ENABLED.value())
                    .set(UserPushDevice::getLastActiveAt, now)
                    .set(UserPushDevice::getUpdatedAt, now));
            return toResponse(userPushDeviceMapper.selectById(existing.getId()));
        }
        var device = new UserPushDevice();
        device.setUserId(userId);
        device.setDeviceType(request.deviceType());
        device.setDeviceToken(request.deviceToken());
        device.setStatus(CommonStatus.ENABLED.value());
        device.setLastActiveAt(now);
        device.setCreatedAt(now);
        device.setUpdatedAt(now);
        userPushDeviceMapper.insert(device);
        return toResponse(device);
    }

    @Transactional
    public void unregister(Long userId, String deviceToken) {
        userPushDeviceMapper.update(null, new LambdaUpdateWrapper<UserPushDevice>()
                .eq(UserPushDevice::getUserId, userId)
                .eq(UserPushDevice::getDeviceToken, deviceToken)
                .set(UserPushDevice::getStatus, CommonStatus.DISABLED.value())
                .set(UserPushDevice::getUpdatedAt, DateTimeUtils.now()));
    }

    public List<PushDeviceResponse> list(Long userId) {
        return userPushDeviceMapper.selectList(new LambdaQueryWrapper<UserPushDevice>()
                .eq(UserPushDevice::getUserId, userId)
                .orderByDesc(UserPushDevice::getId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private PushDeviceResponse toResponse(UserPushDevice device) {
        return new PushDeviceResponse(
                device.getId(),
                device.getUserId(),
                device.getDeviceType(),
                maskDeviceToken(device.getDeviceToken()),
                device.getStatus(),
                device.getLastActiveAt(),
                device.getCreatedAt(),
                device.getUpdatedAt());
    }

    private String maskDeviceToken(String deviceToken) {
        if (deviceToken == null || deviceToken.isBlank()) {
            return null;
        }
        var normalized = deviceToken.trim();
        if (normalized.length() <= 4) {
            return "****";
        }
        return "****" + normalized.substring(normalized.length() - 4);
    }
}

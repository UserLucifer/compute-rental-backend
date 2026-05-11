package com.compute.rental.modules.recharge.dto;

import java.time.LocalDateTime;

public record AdminRechargeChannelResponse(
        Long channelId,
        String channelCode,
        String channelName,
        String network,
        String displayUrl,
        String accountName,
        String accountNo,
        Integer sortNo,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

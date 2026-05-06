package com.compute.rental.modules.recharge.dto;

import java.time.LocalDateTime;

public record RechargeChannelTranslationResponse(
        Long channelId,
        String locale,
        String channelName,
        String accountName,
        Boolean configured,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

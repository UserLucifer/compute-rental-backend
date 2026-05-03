package com.compute.rental.modules.recharge.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminRechargeChannelResponse(
        Long channelId,
        String channelCode,
        String channelName,
        String network,
        String displayUrl,
        String accountName,
        String accountNo,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        BigDecimal feeRate,
        Integer sortNo,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

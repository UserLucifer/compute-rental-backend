package com.compute.rental.modules.recharge.dto;

import java.math.BigDecimal;

public record RechargeChannelResponse(
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
        Integer sortNo
) {
}

package com.compute.rental.modules.recharge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record RechargeChannelResponse(
        Long channelId,
        String channelCode,
        String channelName,
        String network,
        String displayUrl,
        String accountName,
        String accountNo,
        @Schema(description = "有效最低充值金额，取渠道最低金额和系统参数 recharge.min_amount 的较大值")
        BigDecimal minAmount,
        BigDecimal maxAmount,
        BigDecimal feeRate,
        Integer sortNo,
        String locale,
        String requestedLocale,
        Boolean localeFallback
) {
}

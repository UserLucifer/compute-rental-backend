package com.compute.rental.modules.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SettlementOrderResponse(
        String settlementNo,
        String orderNo,
        String settlementType,
        String currency,
        BigDecimal principalAmount,
        BigDecimal profitAmount,
        BigDecimal penaltyAmount,
        BigDecimal actualSettleAmount,
        String status,
        Long reviewedBy,
        LocalDateTime reviewedAt,
        LocalDateTime settledAt,
        String walletTxNo,
        String remark,
        LocalDateTime createdAt
) {
}

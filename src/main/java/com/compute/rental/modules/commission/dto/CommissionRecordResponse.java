package com.compute.rental.modules.commission.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CommissionRecordResponse(
        String commissionNo,
        Long sourceUserId,
        @Schema(description = "用户名称")
        String userName,
        Long sourceOrderId,
        Long sourceProfitId,
        Integer levelNo,
        BigDecimal sourceProfitAmount,
        BigDecimal commissionRateSnapshot,
        BigDecimal commissionAmount,
        String status,
        String walletTxNo,
        LocalDateTime settledAt,
        LocalDateTime createdAt
) {
}

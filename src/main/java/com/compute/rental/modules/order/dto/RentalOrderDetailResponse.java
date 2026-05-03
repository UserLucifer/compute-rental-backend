package com.compute.rental.modules.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RentalOrderDetailResponse(
        String orderNo,
        @Schema(description = "用户名称")
        String userName,
        Long productId,
        Long aiModelId,
        Long cycleRuleId,
        String productCodeSnapshot,
        String productNameSnapshot,
        String machineCodeSnapshot,
        String machineAliasSnapshot,
        String regionNameSnapshot,
        String gpuModelSnapshot,
        Integer gpuMemorySnapshotGb,
        BigDecimal gpuPowerTopsSnapshot,
        BigDecimal gpuRentPriceSnapshot,
        Long tokenOutputPerDaySnapshot,
        String aiModelNameSnapshot,
        String aiVendorNameSnapshot,
        BigDecimal monthlyTokenConsumptionSnapshot,
        BigDecimal tokenUnitPriceSnapshot,
        BigDecimal deployFeeSnapshot,
        Integer cycleDaysSnapshot,
        BigDecimal yieldMultiplierSnapshot,
        BigDecimal earlyPenaltyRateSnapshot,
        String currency,
        BigDecimal orderAmount,
        BigDecimal paidAmount,
        BigDecimal expectedDailyProfit,
        BigDecimal expectedTotalProfit,
        String orderStatus,
        String profitStatus,
        String settlementStatus,
        String machinePayTxNo,
        LocalDateTime paidAt,
        LocalDateTime apiGeneratedAt,
        LocalDateTime deployFeePaidAt,
        LocalDateTime activatedAt,
        LocalDateTime autoPauseAt,
        LocalDateTime pausedAt,
        LocalDateTime startedAt,
        LocalDateTime profitStartAt,
        LocalDateTime profitEndAt,
        LocalDateTime expiredAt,
        LocalDateTime canceledAt,
        LocalDateTime finishedAt,
        LocalDateTime createdAt,
        ApiCredentialResponse apiCredential
) {
}

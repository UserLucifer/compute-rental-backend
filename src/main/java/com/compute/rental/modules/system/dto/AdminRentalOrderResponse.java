package com.compute.rental.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminRentalOrderResponse(
        @Schema(description = "Rental order internal ID")
        Long id,
        @Schema(description = "Rental order number")
        String orderNo,
        @Schema(description = "User internal ID")
        Long userId,
        @Schema(description = "User name")
        String userName,
        @Schema(description = "Product internal ID")
        Long productId,
        @Schema(description = "AI model internal ID")
        Long aiModelId,
        @Schema(description = "Cycle rule internal ID")
        Long cycleRuleId,
        @Schema(description = "Product code snapshot")
        String productCodeSnapshot,
        @Schema(description = "Product name snapshot")
        String productNameSnapshot,
        @Schema(description = "Machine code snapshot")
        String machineCodeSnapshot,
        @Schema(description = "Machine alias snapshot")
        String machineAliasSnapshot,
        @Schema(description = "Region name snapshot")
        String regionNameSnapshot,
        @Schema(description = "GPU model snapshot")
        String gpuModelSnapshot,
        @Schema(description = "GPU memory snapshot in GB")
        Integer gpuMemorySnapshotGb,
        @Schema(description = "GPU power snapshot in TOPS")
        BigDecimal gpuPowerTopsSnapshot,
        @Schema(description = "GPU rent price snapshot")
        BigDecimal gpuRentPriceSnapshot,
        @Schema(description = "Token output per day snapshot")
        Long tokenOutputPerDaySnapshot,
        @Schema(description = "AI model name snapshot")
        String aiModelNameSnapshot,
        @Schema(description = "AI vendor name snapshot")
        String aiVendorNameSnapshot,
        @Schema(description = "Monthly token consumption snapshot")
        BigDecimal monthlyTokenConsumptionSnapshot,
        @Schema(description = "Token unit price snapshot")
        BigDecimal tokenUnitPriceSnapshot,
        @Schema(description = "Deploy fee snapshot")
        BigDecimal deployFeeSnapshot,
        @Schema(description = "Cycle days snapshot")
        Integer cycleDaysSnapshot,
        @Schema(description = "Yield multiplier snapshot")
        BigDecimal yieldMultiplierSnapshot,
        @Schema(description = "Early penalty rate snapshot")
        BigDecimal earlyPenaltyRateSnapshot,
        @Schema(description = "Currency code")
        String currency,
        @Schema(description = "Order amount")
        BigDecimal orderAmount,
        @Schema(description = "Paid amount")
        BigDecimal paidAmount,
        @Schema(description = "Expected daily profit")
        BigDecimal expectedDailyProfit,
        @Schema(description = "Expected total profit")
        BigDecimal expectedTotalProfit,
        @Schema(description = "Order status")
        String orderStatus,
        @Schema(description = "Profit status")
        String profitStatus,
        @Schema(description = "Settlement status")
        String settlementStatus,
        @Schema(description = "Machine payment transaction number")
        String machinePayTxNo,
        @Schema(description = "Paid time")
        LocalDateTime paidAt,
        @Schema(description = "API generated time")
        LocalDateTime apiGeneratedAt,
        @Schema(description = "Deploy fee paid time")
        LocalDateTime deployFeePaidAt,
        @Schema(description = "Activated time")
        LocalDateTime activatedAt,
        @Schema(description = "Auto pause time")
        LocalDateTime autoPauseAt,
        @Schema(description = "Paused time")
        LocalDateTime pausedAt,
        @Schema(description = "Started time")
        LocalDateTime startedAt,
        @Schema(description = "Profit start time")
        LocalDateTime profitStartAt,
        @Schema(description = "Profit end time")
        LocalDateTime profitEndAt,
        @Schema(description = "Expired time")
        LocalDateTime expiredAt,
        @Schema(description = "Canceled time")
        LocalDateTime canceledAt,
        @Schema(description = "Finished time")
        LocalDateTime finishedAt,
        @Schema(description = "Created time")
        LocalDateTime createdAt,
        @Schema(description = "Updated time")
        LocalDateTime updatedAt
) {
}

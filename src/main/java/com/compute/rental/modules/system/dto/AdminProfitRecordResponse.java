package com.compute.rental.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminProfitRecordResponse(
        @Schema(description = "Profit record internal ID")
        Long id,
        @Schema(description = "Profit record number")
        String profitNo,
        @Schema(description = "User internal ID")
        Long userId,
        @Schema(description = "User name")
        String userName,
        @Schema(description = "Rental order internal ID")
        Long rentalOrderId,
        @Schema(description = "Profit date")
        LocalDate profitDate,
        @Schema(description = "GPU daily token snapshot")
        Long gpuDailyTokenSnapshot,
        @Schema(description = "Token price snapshot")
        BigDecimal tokenPriceSnapshot,
        @Schema(description = "Yield multiplier snapshot")
        BigDecimal yieldMultiplierSnapshot,
        @Schema(description = "Base profit amount")
        BigDecimal baseProfitAmount,
        @Schema(description = "Final profit amount")
        BigDecimal finalProfitAmount,
        @Schema(description = "Profit status")
        String status,
        @Schema(description = "Wallet transaction number")
        String walletTxNo,
        @Schema(description = "Whether commission has been generated")
        Integer commissionGenerated,
        @Schema(description = "Settled time")
        LocalDateTime settledAt,
        @Schema(description = "Remark")
        String remark,
        @Schema(description = "Created time")
        LocalDateTime createdAt,
        @Schema(description = "Updated time")
        LocalDateTime updatedAt
) {
}

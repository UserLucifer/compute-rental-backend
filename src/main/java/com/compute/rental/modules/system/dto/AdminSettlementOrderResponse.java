package com.compute.rental.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminSettlementOrderResponse(
        @Schema(description = "Settlement order internal ID")
        Long id,
        @Schema(description = "Settlement order number")
        String settlementNo,
        @Schema(description = "User internal ID")
        Long userId,
        @Schema(description = "User name")
        String userName,
        @Schema(description = "Rental order internal ID")
        Long rentalOrderId,
        @Schema(description = "Settlement type")
        String settlementType,
        @Schema(description = "Currency code")
        String currency,
        @Schema(description = "Principal amount")
        BigDecimal principalAmount,
        @Schema(description = "Profit amount")
        BigDecimal profitAmount,
        @Schema(description = "Penalty amount")
        BigDecimal penaltyAmount,
        @Schema(description = "Actual settlement amount")
        BigDecimal actualSettleAmount,
        @Schema(description = "Settlement status")
        String status,
        @Schema(description = "Reviewer admin ID")
        Long reviewedBy,
        @Schema(description = "Reviewed time")
        LocalDateTime reviewedAt,
        @Schema(description = "Settled time")
        LocalDateTime settledAt,
        @Schema(description = "Wallet transaction number")
        String walletTxNo,
        @Schema(description = "Remark")
        String remark,
        @Schema(description = "Created time")
        LocalDateTime createdAt,
        @Schema(description = "Updated time")
        LocalDateTime updatedAt
) {
}

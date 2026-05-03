package com.compute.rental.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminCommissionRecordResponse(
        @Schema(description = "Commission record internal ID")
        Long id,
        @Schema(description = "Commission record number")
        String commissionNo,
        @Schema(description = "Benefit user internal ID")
        Long benefitUserId,
        @Schema(description = "Source user internal ID")
        Long sourceUserId,
        @Schema(description = "Source user name")
        String userName,
        @Schema(description = "Source order internal ID")
        Long sourceOrderId,
        @Schema(description = "Source profit internal ID")
        Long sourceProfitId,
        @Schema(description = "Commission level")
        Integer levelNo,
        @Schema(description = "Currency code")
        String currency,
        @Schema(description = "Source profit amount")
        BigDecimal sourceProfitAmount,
        @Schema(description = "Commission rate snapshot")
        BigDecimal commissionRateSnapshot,
        @Schema(description = "Commission amount")
        BigDecimal commissionAmount,
        @Schema(description = "Commission status")
        String status,
        @Schema(description = "Wallet transaction number")
        String walletTxNo,
        @Schema(description = "Settled time")
        LocalDateTime settledAt,
        @Schema(description = "Created time")
        LocalDateTime createdAt,
        @Schema(description = "Updated time")
        LocalDateTime updatedAt
) {
}

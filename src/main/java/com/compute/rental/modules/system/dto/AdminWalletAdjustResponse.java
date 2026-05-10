package com.compute.rental.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminWalletAdjustResponse(
        @Schema(description = "Wallet transaction internal ID")
        Long transactionId,
        @Schema(description = "Wallet transaction number")
        String txNo,
        @Schema(description = "User internal ID")
        Long userId,
        @Schema(description = "Wallet internal ID")
        Long walletId,
        @Schema(description = "Currency")
        String currency,
        @Schema(description = "Transaction type")
        String txType,
        @Schema(description = "Adjustment amount")
        BigDecimal amount,
        @Schema(description = "Available balance before adjustment")
        BigDecimal beforeAvailableBalance,
        @Schema(description = "Available balance after adjustment")
        BigDecimal afterAvailableBalance,
        @Schema(description = "Frozen balance before adjustment")
        BigDecimal beforeFrozenBalance,
        @Schema(description = "Frozen balance after adjustment")
        BigDecimal afterFrozenBalance,
        @Schema(description = "Wallet business type")
        String bizType,
        @Schema(description = "Adjustment number")
        String adjustNo,
        @Schema(description = "Adjustment reason")
        String reason,
        @Schema(description = "Transaction created time")
        LocalDateTime createdAt
) {
}

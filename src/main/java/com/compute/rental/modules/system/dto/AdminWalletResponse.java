package com.compute.rental.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminWalletResponse(
        @Schema(description = "Wallet internal ID")
        Long id,
        @Schema(description = "Wallet number")
        String walletNo,
        @Schema(description = "User internal ID")
        Long userId,
        @Schema(description = "User name")
        String userName,
        @Schema(description = "Currency code")
        String currency,
        @Schema(description = "Available balance")
        BigDecimal availableBalance,
        @Schema(description = "Frozen balance")
        BigDecimal frozenBalance,
        @Schema(description = "Total recharge amount")
        BigDecimal totalRecharge,
        @Schema(description = "Total withdraw amount")
        BigDecimal totalWithdraw,
        @Schema(description = "Total profit amount")
        BigDecimal totalProfit,
        @Schema(description = "Total commission amount")
        BigDecimal totalCommission,
        @Schema(description = "Wallet status")
        Integer status,
        @Schema(description = "Version number")
        Integer versionNo,
        @Schema(description = "Created time")
        LocalDateTime createdAt,
        @Schema(description = "Updated time")
        LocalDateTime updatedAt
) {
}

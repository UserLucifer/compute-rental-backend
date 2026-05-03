package com.compute.rental.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminWalletTransactionResponse(
        @Schema(description = "Transaction internal ID")
        Long id,
        @Schema(description = "Transaction number")
        String txNo,
        @Schema(description = "Idempotency key")
        String idempotencyKey,
        @Schema(description = "User internal ID")
        Long userId,
        @Schema(description = "User name")
        String userName,
        @Schema(description = "Wallet internal ID")
        Long walletId,
        @Schema(description = "Currency code")
        String currency,
        @Schema(description = "Transaction type")
        String txType,
        @Schema(description = "Transaction amount")
        BigDecimal amount,
        @Schema(description = "Available balance before transaction")
        BigDecimal beforeAvailableBalance,
        @Schema(description = "Available balance after transaction")
        BigDecimal afterAvailableBalance,
        @Schema(description = "Frozen balance before transaction")
        BigDecimal beforeFrozenBalance,
        @Schema(description = "Frozen balance after transaction")
        BigDecimal afterFrozenBalance,
        @Schema(description = "Business type")
        String bizType,
        @Schema(description = "Business order number")
        String bizOrderNo,
        @Schema(description = "Remark")
        String remark,
        @Schema(description = "Created time")
        LocalDateTime createdAt
) {
}

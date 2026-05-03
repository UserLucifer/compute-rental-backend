package com.compute.rental.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminApiDeployOrderResponse(
        @Schema(description = "Deploy order internal ID")
        Long id,
        @Schema(description = "Deploy order number")
        String deployNo,
        @Schema(description = "User internal ID")
        Long userId,
        @Schema(description = "User name")
        String userName,
        @Schema(description = "Rental order internal ID")
        Long rentalOrderId,
        @Schema(description = "API credential internal ID")
        Long apiCredentialId,
        @Schema(description = "AI model internal ID")
        Long aiModelId,
        @Schema(description = "Model name snapshot")
        String modelNameSnapshot,
        @Schema(description = "Currency code")
        String currency,
        @Schema(description = "Deploy fee amount")
        BigDecimal deployFeeAmount,
        @Schema(description = "Deploy order status")
        String status,
        @Schema(description = "Wallet transaction number")
        String walletTxNo,
        @Schema(description = "Paid time")
        LocalDateTime paidAt,
        @Schema(description = "Canceled time")
        LocalDateTime canceledAt,
        @Schema(description = "Created time")
        LocalDateTime createdAt,
        @Schema(description = "Updated time")
        LocalDateTime updatedAt
) {
}

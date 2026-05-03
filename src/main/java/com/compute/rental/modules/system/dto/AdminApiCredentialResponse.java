package com.compute.rental.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminApiCredentialResponse(
        @Schema(description = "Credential internal ID")
        Long id,
        @Schema(description = "Credential number")
        String credentialNo,
        @Schema(description = "User internal ID")
        Long userId,
        @Schema(description = "Rental order internal ID")
        Long rentalOrderId,
        @Schema(description = "API name")
        String apiName,
        @Schema(description = "API base URL")
        String apiBaseUrl,
        @Schema(description = "Masked token")
        String tokenMasked,
        @Schema(description = "Model name snapshot")
        String modelNameSnapshot,
        @Schema(description = "Deploy fee snapshot")
        BigDecimal deployFeeSnapshot,
        @Schema(description = "Token status")
        String tokenStatus,
        @Schema(description = "Generated time")
        LocalDateTime generatedAt,
        @Schema(description = "Activation fee paid time")
        LocalDateTime activationPaidAt,
        @Schema(description = "Activated time")
        LocalDateTime activatedAt,
        @Schema(description = "Auto pause time")
        LocalDateTime autoPauseAt,
        @Schema(description = "Paused time")
        LocalDateTime pausedAt,
        @Schema(description = "Started time")
        LocalDateTime startedAt,
        @Schema(description = "Expired time")
        LocalDateTime expiredAt,
        @Schema(description = "Revoked time")
        LocalDateTime revokedAt,
        @Schema(description = "Mock request count")
        Long mockRequestCount,
        @Schema(description = "Mock token display value")
        Long mockTokenDisplay,
        @Schema(description = "Mock last refresh time")
        LocalDateTime mockLastRefreshAt,
        @Schema(description = "Remark")
        String remark,
        @Schema(description = "Created time")
        LocalDateTime createdAt,
        @Schema(description = "Updated time")
        LocalDateTime updatedAt
) {
}

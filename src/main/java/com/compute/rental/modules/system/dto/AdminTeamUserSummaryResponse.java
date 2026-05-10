package com.compute.rental.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record AdminTeamUserSummaryResponse(
        @Schema(description = "User internal ID")
        Long userId,
        @Schema(description = "User name")
        String userName,
        @Schema(description = "Email")
        String email,
        @Schema(description = "Avatar key")
        String avatarKey,
        @Schema(description = "User status")
        Integer userStatus,
        @Schema(description = "Direct team member count")
        long directCount,
        @Schema(description = "Indirect team member count")
        long indirectCount,
        @Schema(description = "Total team member count")
        long totalTeamCount,
        @Schema(description = "Yesterday settled team commission")
        BigDecimal yesterdayCommission,
        @Schema(description = "Total settled team commission")
        BigDecimal totalCommission,
        @Schema(description = "Active team order count")
        long activeOrderCount,
        @Schema(description = "Running team order count")
        long runningOrderCount,
        @Schema(description = "Currency")
        String currency
) {
}

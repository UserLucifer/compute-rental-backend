package com.compute.rental.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record AdminTeamLeaderboardRow(
        @Schema(description = "Rank number in the selected sort order")
        long rankNo,
        @Schema(description = "User internal ID")
        Long userId,
        @Schema(description = "User name")
        String userName,
        @Schema(description = "Avatar key")
        String avatarKey,
        @Schema(description = "User status")
        Integer userStatus,
        @Schema(description = "Direct team member count")
        long directCount,
        @Schema(description = "Indirect team member count")
        long indirectCount,
        @Schema(description = "Yesterday settled team commission")
        BigDecimal yesterdayCommission,
        @Schema(description = "Total settled team commission")
        BigDecimal totalCommission,
        @Schema(description = "Active team order count")
        long activeOrderCount,
        @Schema(description = "Currency")
        String currency
) {
}

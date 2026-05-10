package com.compute.rental.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminTeamMemberRow(
        @Schema(description = "Team relation internal ID")
        Long relationId,
        @Schema(description = "Ancestor user internal ID")
        Long ancestorUserId,
        @Schema(description = "Direct parent user internal ID")
        Long parentUserId,
        @Schema(description = "User internal ID")
        Long userId,
        @Schema(description = "User name")
        String userName,
        @Schema(description = "Avatar key")
        String avatarKey,
        @Schema(description = "User status")
        Integer userStatus,
        @Schema(description = "Level depth relative to the ancestor user")
        Integer levelDepth,
        @Schema(description = "Relationship created time")
        LocalDateTime relationshipCreatedAt,
        @Schema(description = "Current order status: RUNNING, PAUSED, NONE")
        String orderStatus,
        @Schema(description = "Latest active order number")
        String latestOrderNo,
        @Schema(description = "Yesterday settled commission contribution to the ancestor user")
        BigDecimal yesterdayContributionAmount,
        @Schema(description = "Total settled commission contribution to the ancestor user")
        BigDecimal totalContributionAmount,
        @Schema(description = "Currency")
        String currency
) {
}

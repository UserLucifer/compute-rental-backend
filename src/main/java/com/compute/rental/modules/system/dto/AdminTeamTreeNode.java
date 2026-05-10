package com.compute.rental.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record AdminTeamTreeNode(
        @Schema(description = "User internal ID")
        Long userId,
        @Schema(description = "User name")
        String userName,
        @Schema(description = "Avatar key")
        String avatarKey,
        @Schema(description = "User status")
        Integer userStatus,
        @Schema(description = "Level depth relative to the selected root user")
        int levelDepth,
        @Schema(description = "Parent user internal ID")
        Long parentUserId,
        @Schema(description = "Direct team member count")
        long directCount,
        @Schema(description = "Indirect team member count")
        long indirectCount,
        @Schema(description = "Whether this node can load children in the current two-level tree")
        boolean hasChildren,
        @Schema(description = "Children count available for lazy loading")
        long childrenCount,
        @Schema(description = "Total settled commission contribution to the selected root user")
        BigDecimal totalContributionAmount,
        @Schema(description = "Yesterday settled commission contribution to the selected root user")
        BigDecimal yesterdayContributionAmount,
        @Schema(description = "Currency")
        String currency
) {
}

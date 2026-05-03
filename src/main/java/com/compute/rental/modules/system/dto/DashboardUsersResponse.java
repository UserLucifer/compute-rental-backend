package com.compute.rental.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record DashboardUsersResponse(
        @Schema(description = "Today new users")
        Long todayNewUsers,
        @Schema(description = "Current month new users")
        Long currentMonthNewUsers,
        @Schema(description = "Active users")
        Long activeUsers,
        @Schema(description = "Disabled users")
        Long disabledUsers,
        @Schema(description = "Users with parent")
        Long usersWithParent,
        @Schema(description = "Users without parent")
        Long usersWithoutParent
) {
}

package com.compute.rental.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record DashboardOrdersResponse(
        @Schema(description = "Order status counts")
        List<DashboardStatusCountResponse> orderStatusCounts,
        @Schema(description = "Profit status counts")
        List<DashboardStatusCountResponse> profitStatusCounts,
        @Schema(description = "Today new order count")
        Long todayNewOrderCount,
        @Schema(description = "Today paid order count")
        Long todayPaidOrderCount,
        @Schema(description = "Running order count")
        Long runningOrderCount,
        @Schema(description = "Paused order count")
        Long pausedOrderCount
) {
}

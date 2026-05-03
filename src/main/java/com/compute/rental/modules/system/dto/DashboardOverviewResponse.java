package com.compute.rental.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record DashboardOverviewResponse(
        @Schema(description = "Total users")
        Long totalUsers,
        @Schema(description = "Active users")
        Long activeUsers,
        @Schema(description = "Disabled users")
        Long disabledUsers,
        @Schema(description = "Total recharge amount")
        BigDecimal totalRechargeAmount,
        @Schema(description = "Total withdraw amount")
        BigDecimal totalWithdrawAmount,
        @Schema(description = "Total order amount")
        BigDecimal totalOrderAmount,
        @Schema(description = "Total profit amount")
        BigDecimal totalProfitAmount,
        @Schema(description = "Total commission amount")
        BigDecimal totalCommissionAmount,
        @Schema(description = "Running order count")
        Long runningOrderCount,
        @Schema(description = "Pending recharge count")
        Long pendingRechargeCount,
        @Schema(description = "Pending withdraw count")
        Long pendingWithdrawCount
) {
}

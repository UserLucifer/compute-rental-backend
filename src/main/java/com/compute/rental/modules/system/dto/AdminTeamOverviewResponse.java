package com.compute.rental.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record AdminTeamOverviewResponse(
        @Schema(description = "Active team count")
        long activeTeamCount,
        @Schema(description = "Estimated commission amount for the selected day/range")
        BigDecimal todayEstimatedCommission,
        @Schema(description = "Currency")
        String currency,
        @Schema(description = "Active team growth rate")
        BigDecimal activeTeamGrowthRate,
        @Schema(description = "Commission growth rate")
        BigDecimal commissionGrowthRate
) {
}

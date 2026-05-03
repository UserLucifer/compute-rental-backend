package com.compute.rental.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminRentalCycleRuleResponse(
        @Schema(description = "Cycle rule internal ID")
        Long id,
        @Schema(description = "Cycle code")
        String cycleCode,
        @Schema(description = "Cycle name")
        String cycleName,
        @Schema(description = "Cycle days")
        Integer cycleDays,
        @Schema(description = "Yield multiplier")
        BigDecimal yieldMultiplier,
        @Schema(description = "Early penalty rate")
        BigDecimal earlyPenaltyRate,
        @Schema(description = "Status")
        Integer status,
        @Schema(description = "Sort number")
        Integer sortNo,
        @Schema(description = "Created time")
        LocalDateTime createdAt,
        @Schema(description = "Updated time")
        LocalDateTime updatedAt
) {
}

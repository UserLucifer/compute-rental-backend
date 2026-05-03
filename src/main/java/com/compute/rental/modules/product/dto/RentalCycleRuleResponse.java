package com.compute.rental.modules.product.dto;

import java.math.BigDecimal;

public record RentalCycleRuleResponse(
        Long id,
        String cycleCode,
        String cycleName,
        Integer cycleDays,
        BigDecimal yieldMultiplier,
        BigDecimal earlyPenaltyRate
) {
}

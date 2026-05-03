package com.compute.rental.modules.order.dto;

import java.math.BigDecimal;

public record RentalEstimateResponse(
        Long productId,
        String productName,
        Long aiModelId,
        String aiModelName,
        Long cycleRuleId,
        String cycleName,
        Integer cycleDays,
        BigDecimal rentPrice,
        BigDecimal deployTechFee,
        Long tokenOutputPerDay,
        BigDecimal tokenUnitPrice,
        BigDecimal yieldMultiplier,
        BigDecimal expectedDailyProfit,
        BigDecimal expectedTotalProfit
) {
}

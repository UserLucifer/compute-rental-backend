package com.compute.rental.modules.product.dto;

import java.time.LocalDateTime;

public record RentalCycleRuleTranslationResponse(
        Long cycleRuleId,
        String cycleCode,
        String locale,
        String cycleName,
        Boolean configured,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

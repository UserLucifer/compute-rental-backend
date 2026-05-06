package com.compute.rental.modules.product.dto;

import jakarta.validation.constraints.NotBlank;

public record RentalCycleRuleTranslationRequest(
        @NotBlank String locale,
        String cycleName
) {
}

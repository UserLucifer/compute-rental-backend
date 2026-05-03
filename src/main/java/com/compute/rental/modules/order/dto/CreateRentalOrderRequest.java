package com.compute.rental.modules.order.dto;

import jakarta.validation.constraints.NotNull;

public record CreateRentalOrderRequest(
        @NotNull
        Long productId,

        @NotNull
        Long aiModelId,

        @NotNull
        Long cycleRuleId
) {
}

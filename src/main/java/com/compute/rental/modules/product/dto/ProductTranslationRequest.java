package com.compute.rental.modules.product.dto;

import jakarta.validation.constraints.NotBlank;

public record ProductTranslationRequest(
        @NotBlank String locale,
        String productName
) {
}

package com.compute.rental.modules.product.dto;

import jakarta.validation.constraints.NotBlank;

public record AiModelTranslationRequest(
        @NotBlank String locale,
        String modelName,
        String vendorName
) {
}

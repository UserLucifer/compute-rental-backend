package com.compute.rental.modules.product.dto;

import jakarta.validation.constraints.NotBlank;

public record GpuModelTranslationRequest(
        @NotBlank String locale,
        String modelName
) {
}

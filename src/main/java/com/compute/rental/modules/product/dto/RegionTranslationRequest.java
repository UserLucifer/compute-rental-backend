package com.compute.rental.modules.product.dto;

import jakarta.validation.constraints.NotBlank;

public record RegionTranslationRequest(
        @NotBlank String locale,
        String regionName
) {
}

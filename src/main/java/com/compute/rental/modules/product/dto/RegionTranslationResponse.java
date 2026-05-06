package com.compute.rental.modules.product.dto;

import java.time.LocalDateTime;

public record RegionTranslationResponse(
        Long regionId,
        String locale,
        String regionName,
        Boolean configured,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

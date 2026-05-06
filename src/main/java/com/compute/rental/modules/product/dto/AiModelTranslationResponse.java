package com.compute.rental.modules.product.dto;

import java.time.LocalDateTime;

public record AiModelTranslationResponse(
        Long aiModelId,
        String modelCode,
        String locale,
        String modelName,
        String vendorName,
        Boolean configured,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

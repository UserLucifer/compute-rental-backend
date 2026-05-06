package com.compute.rental.modules.product.dto;

import java.time.LocalDateTime;

public record GpuModelTranslationResponse(
        Long gpuModelId,
        String locale,
        String modelName,
        Boolean configured,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

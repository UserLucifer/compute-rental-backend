package com.compute.rental.modules.product.dto;

public record GpuModelResponse(
        Long id,
        String modelCode,
        String modelName,
        String locale,
        String requestedLocale,
        Boolean localeFallback
) {
}

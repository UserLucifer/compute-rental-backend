package com.compute.rental.modules.product.dto;

public record RegionResponse(
        Long id,
        String regionCode,
        String regionName,
        String locale,
        String requestedLocale,
        Boolean localeFallback
) {
}

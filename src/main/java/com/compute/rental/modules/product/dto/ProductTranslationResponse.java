package com.compute.rental.modules.product.dto;

import java.time.LocalDateTime;

public record ProductTranslationResponse(
        Long productId,
        String productCode,
        String locale,
        String productName,
        Boolean configured,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

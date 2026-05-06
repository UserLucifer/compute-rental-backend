package com.compute.rental.modules.blog.dto;

import java.time.LocalDateTime;

public record BlogCategoryTranslationResponse(
        Long categoryId,
        String locale,
        String categoryName,
        Boolean configured,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

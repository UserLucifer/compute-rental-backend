package com.compute.rental.modules.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record BlogCategoryResponse(
        @Schema(description = "Category internal ID")
        Long id,
        @Schema(description = "Category name")
        String categoryName,
        @Schema(description = "Sort number")
        Integer sortNo,
        @Schema(description = "Category status")
        Integer status,
        @Schema(description = "Created time")
        LocalDateTime createdAt,
        @Schema(description = "Updated time")
        LocalDateTime updatedAt
) {
}

package com.compute.rental.modules.docs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record DocCategoryResponse(
        @Schema(description = "Category internal ID")
        Long id,
        @Schema(description = "Parent category internal ID")
        Long parentId,
        @Schema(description = "Category route code")
        String categoryCode,
        @Schema(description = "Category name")
        String categoryName,
        @Schema(description = "Icon key")
        String icon,
        @Schema(description = "Sort number")
        Integer sortNo,
        @Schema(description = "Status: 1 enabled, 0 disabled")
        Integer status,
        @Schema(description = "Child categories")
        List<DocCategoryResponse> children,
        @Schema(description = "Created time")
        LocalDateTime createdAt,
        @Schema(description = "Updated time")
        LocalDateTime updatedAt
) {
}

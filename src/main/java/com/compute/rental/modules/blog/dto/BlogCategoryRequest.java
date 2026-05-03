package com.compute.rental.modules.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record BlogCategoryRequest(
        @Schema(description = "Category name")
        @NotBlank
        String categoryName,
        @Schema(description = "Sort number")
        Integer sortNo,
        @Schema(description = "Category status")
        Integer status
) {
}

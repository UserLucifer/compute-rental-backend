package com.compute.rental.modules.docs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record DocCategoryRequest(
        @Schema(description = "Document language: zh-CN, en-US")
        String language,
        @Schema(description = "Document section: guide, integration, faq, support")
        @NotBlank
        String section,
        @Schema(description = "Parent category internal ID")
        Long parentId,
        @Schema(description = "Category route code")
        @NotBlank
        String categoryCode,
        @Schema(description = "Category name")
        @NotBlank
        String categoryName,
        @Schema(description = "Icon key")
        String icon,
        @Schema(description = "Sort number")
        Integer sortNo,
        @Schema(description = "Status: 1 enabled, 0 disabled")
        Integer status
) {
}

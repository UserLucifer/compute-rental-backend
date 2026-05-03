package com.compute.rental.modules.docs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DocArticleRequest(
        @Schema(description = "Category internal ID")
        @NotNull
        Long categoryId,
        @Schema(description = "Article title")
        @NotBlank
        String title,
        @Schema(description = "Article route slug")
        @NotBlank
        String slug,
        @Schema(description = "Article summary")
        String summary,
        @Schema(description = "Markdown content")
        @NotBlank
        String contentMarkdown,
        @Schema(description = "Publish status: 0 draft, 1 published, 2 offline")
        Integer publishStatus,
        @Schema(description = "Sort number")
        Integer sortNo
) {
}

package com.compute.rental.modules.docs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record DocArticleResponse(
        @Schema(description = "Article internal ID")
        Long id,
        @Schema(description = "Document language: zh-CN, en-US")
        String language,
        @Schema(description = "Document section: guide, integration, faq, support")
        String section,
        @Schema(description = "Category internal ID")
        Long categoryId,
        @Schema(description = "Category name")
        String categoryName,
        @Schema(description = "Article title")
        String title,
        @Schema(description = "Article route slug")
        String slug,
        @Schema(description = "Article summary")
        String summary,
        @Schema(description = "Markdown content")
        String contentMarkdown,
        @Schema(description = "Publish status: 0 draft, 1 published, 2 offline")
        Integer publishStatus,
        @Schema(description = "Section home flag: 1 home, 0 normal")
        Integer isSectionHome,
        @Schema(description = "Published time")
        LocalDateTime publishedAt,
        @Schema(description = "Sort number")
        Integer sortNo,
        @Schema(description = "View count")
        Long viewCount,
        @Schema(description = "Creator admin ID")
        Long createdBy,
        @Schema(description = "Created time")
        LocalDateTime createdAt,
        @Schema(description = "Updated time")
        LocalDateTime updatedAt
) {
}

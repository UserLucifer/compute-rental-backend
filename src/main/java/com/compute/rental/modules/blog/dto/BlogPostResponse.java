package com.compute.rental.modules.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record BlogPostResponse(
        @Schema(description = "Post internal ID")
        Long id,
        @Schema(description = "Category internal ID")
        Long categoryId,
        @Schema(description = "Category name")
        String categoryName,
        @Schema(description = "Post title")
        String title,
        @Schema(description = "Post summary")
        String summary,
        @Schema(description = "Cover image URL")
        String coverImageUrl,
        @Schema(description = "Markdown content")
        String contentMarkdown,
        @Schema(description = "Publish status")
        Integer publishStatus,
        @Schema(description = "Published time")
        LocalDateTime publishedAt,
        @Schema(description = "Top flag")
        Integer isTop,
        @Schema(description = "Sort number")
        Integer sortNo,
        @Schema(description = "View count")
        Long viewCount,
        @Schema(description = "Creator admin ID")
        Long createdBy,
        @Schema(description = "Tag internal IDs")
        List<Long> tagIds,
        @Schema(description = "Created time")
        LocalDateTime createdAt,
        @Schema(description = "Updated time")
        LocalDateTime updatedAt
) {
}

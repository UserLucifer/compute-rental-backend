package com.compute.rental.modules.blog.dto;

import java.time.LocalDateTime;

public record BlogPostTranslationResponse(
        Long postId,
        String locale,
        String title,
        String summary,
        String contentMarkdown,
        Boolean configured,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

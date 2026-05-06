package com.compute.rental.modules.blog.dto;

import java.time.LocalDateTime;

public record BlogTagTranslationResponse(
        Long tagId,
        String locale,
        String tagName,
        Boolean configured,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

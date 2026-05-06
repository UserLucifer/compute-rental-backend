package com.compute.rental.modules.blog.dto;

import jakarta.validation.constraints.NotBlank;

public record BlogPostTranslationRequest(
        @NotBlank String locale,
        String title,
        String summary,
        String contentMarkdown
) {
}

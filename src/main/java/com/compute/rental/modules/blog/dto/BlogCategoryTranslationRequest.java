package com.compute.rental.modules.blog.dto;

import jakarta.validation.constraints.NotBlank;

public record BlogCategoryTranslationRequest(
        @NotBlank String locale,
        String categoryName
) {
}

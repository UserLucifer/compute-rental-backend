package com.compute.rental.modules.blog.dto;

import jakarta.validation.constraints.NotBlank;

public record BlogTagTranslationRequest(
        @NotBlank String locale,
        String tagName
) {
}

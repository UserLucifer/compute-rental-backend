package com.compute.rental.modules.system.dto;

import jakarta.validation.constraints.NotBlank;

public record NotificationTranslationRequest(
        @NotBlank
        String locale,
        String title,
        String content
) {
}

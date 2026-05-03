package com.compute.rental.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NotificationCreateRequest(
        @NotNull Long userId,
        @NotBlank String title,
        @NotBlank String content,
        @NotBlank String type,
        String bizType,
        Long bizId
) {
}

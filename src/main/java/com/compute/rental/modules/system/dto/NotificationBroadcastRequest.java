package com.compute.rental.modules.system.dto;

import jakarta.validation.constraints.NotBlank;

public record NotificationBroadcastRequest(
        @NotBlank String title,
        @NotBlank String content,
        @NotBlank String type,
        String bizType,
        Long bizId
) {
}

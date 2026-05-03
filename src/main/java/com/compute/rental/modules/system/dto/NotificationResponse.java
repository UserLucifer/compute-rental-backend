package com.compute.rental.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record NotificationResponse(
        @Schema(description = "Notification internal ID")
        Long id,
        @Schema(description = "User internal ID")
        Long userId,
        @Schema(description = "User name")
        String userName,
        @Schema(description = "Notification title")
        String title,
        @Schema(description = "Notification content")
        String content,
        @Schema(description = "Notification type")
        String type,
        @Schema(description = "Business type")
        String bizType,
        @Schema(description = "Business internal ID")
        Long bizId,
        @Schema(description = "Read status")
        Integer readStatus,
        @Schema(description = "Read time")
        LocalDateTime readAt,
        @Schema(description = "Created time")
        LocalDateTime createdAt
) {
}

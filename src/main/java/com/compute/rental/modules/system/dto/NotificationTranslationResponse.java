package com.compute.rental.modules.system.dto;

import java.time.LocalDateTime;

public record NotificationTranslationResponse(
        Long notificationId,
        String locale,
        String title,
        String content,
        Boolean configured,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

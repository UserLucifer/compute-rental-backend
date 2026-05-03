package com.compute.rental.modules.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record PushDeviceResponse(
        @Schema(description = "Push device internal ID")
        Long id,
        @Schema(description = "User internal ID")
        Long userId,
        @Schema(description = "Device type")
        String deviceType,
        @Schema(description = "Masked device token")
        String deviceTokenMasked,
        @Schema(description = "Device status")
        Integer status,
        @Schema(description = "Last active time")
        LocalDateTime lastActiveAt,
        @Schema(description = "Created time")
        LocalDateTime createdAt,
        @Schema(description = "Updated time")
        LocalDateTime updatedAt
) {
}

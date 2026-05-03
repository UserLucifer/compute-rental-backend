package com.compute.rental.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record AdminSysConfigResponse(
        @Schema(description = "System config internal ID")
        Long id,
        @Schema(description = "System config key")
        String configKey,
        @Schema(description = "System config value")
        String configValue,
        @Schema(description = "System config description")
        String configDesc,
        @Schema(description = "Created time")
        LocalDateTime createdAt,
        @Schema(description = "Updated time")
        LocalDateTime updatedAt
) {
}

package com.compute.rental.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record AdminLogResponse(
        @Schema(description = "Log internal ID")
        Long id,
        @Schema(description = "Admin internal ID")
        Long adminId,
        @Schema(description = "Operator name")
        String operatorName,
        @Schema(description = "Action code")
        String action,
        @Schema(description = "Action display name")
        String actionName,
        @Schema(description = "Target table")
        String targetTable,
        @Schema(description = "Target internal ID")
        Long targetId,
        @Schema(description = "Before value snapshot")
        String beforeValue,
        @Schema(description = "After value snapshot")
        String afterValue,
        @Schema(description = "Remark")
        String remark,
        @Schema(description = "Client IP")
        String ip,
        @Schema(description = "Created time")
        LocalDateTime createdAt
) {
}

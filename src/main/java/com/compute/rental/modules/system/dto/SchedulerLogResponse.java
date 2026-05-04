package com.compute.rental.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record SchedulerLogResponse(
        @Schema(description = "Scheduler log ID")
        Long id,
        @Schema(description = "Task name")
        String taskName,
        @Schema(description = "Execution status")
        String status,
        @Schema(description = "Total processed count")
        Integer totalCount,
        @Schema(description = "Success count")
        Integer successCount,
        @Schema(description = "Fail count")
        Integer failCount,
        @Schema(description = "Error message")
        String errorMessage,
        @Schema(description = "Started time")
        LocalDateTime startedAt,
        @Schema(description = "Finished time")
        LocalDateTime finishedAt,
        @Schema(description = "Created time")
        LocalDateTime createdAt
) {
}

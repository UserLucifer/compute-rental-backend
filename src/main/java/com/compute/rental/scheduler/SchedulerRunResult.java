package com.compute.rental.scheduler;

import io.swagger.v3.oas.annotations.media.Schema;

public record SchedulerRunResult(
        @Schema(description = "Scheduler task name")
        String taskName,
        @Schema(description = "Total processed count")
        int totalCount,
        @Schema(description = "Successful processed count")
        int successCount,
        @Schema(description = "Failed processed count")
        int failCount,
        @Schema(description = "Run status")
        String status,
        @Schema(description = "Error message")
        String errorMessage
) {
}

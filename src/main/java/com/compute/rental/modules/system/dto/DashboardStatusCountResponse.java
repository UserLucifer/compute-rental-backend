package com.compute.rental.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record DashboardStatusCountResponse(
        @Schema(description = "Status code")
        String status,
        @Schema(description = "Record count")
        Long count
) {
}

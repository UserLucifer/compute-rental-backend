package com.compute.rental.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record EnumOptionResponse(
        @Schema(description = "Enum constant name")
        String name,
        @Schema(description = "Frontend value")
        Object value,
        @Schema(description = "Display label")
        String label
) {
}

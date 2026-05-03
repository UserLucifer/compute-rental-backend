package com.compute.rental.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record AdminRegionResponse(
        @Schema(description = "Region internal ID")
        Long id,
        @Schema(description = "Region code")
        String regionCode,
        @Schema(description = "Region name")
        String regionName,
        @Schema(description = "Sort number")
        Integer sortNo,
        @Schema(description = "Status")
        Integer status,
        @Schema(description = "Created time")
        LocalDateTime createdAt,
        @Schema(description = "Updated time")
        LocalDateTime updatedAt
) {
}

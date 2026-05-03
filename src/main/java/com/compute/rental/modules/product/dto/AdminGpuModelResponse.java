package com.compute.rental.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record AdminGpuModelResponse(
        @Schema(description = "GPU model internal ID")
        Long id,
        @Schema(description = "GPU model code")
        String modelCode,
        @Schema(description = "GPU model name")
        String modelName,
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

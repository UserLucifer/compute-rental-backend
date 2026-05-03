package com.compute.rental.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AdminGpuModelRequest(
        @Schema(description = "GPU model code")
        String modelCode,
        @Schema(description = "GPU model name")
        String modelName,
        @Schema(description = "Sort number")
        Integer sortNo,
        @Schema(description = "Status")
        Integer status
) {
}

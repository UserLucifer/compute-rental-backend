package com.compute.rental.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AdminRegionRequest(
        @Schema(description = "Region code")
        String regionCode,
        @Schema(description = "Region name")
        String regionName,
        @Schema(description = "Sort number")
        Integer sortNo,
        @Schema(description = "Status")
        Integer status
) {
}

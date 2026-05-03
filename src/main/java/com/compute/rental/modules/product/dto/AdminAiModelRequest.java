package com.compute.rental.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record AdminAiModelRequest(
        @Schema(description = "AI model code")
        String modelCode,
        @Schema(description = "AI model name")
        String modelName,
        @Schema(description = "Vendor name")
        String vendorName,
        @Schema(description = "Logo URL")
        String logoUrl,
        @Schema(description = "Monthly token consumption in trillion")
        BigDecimal monthlyTokenConsumptionTrillion,
        @Schema(description = "Token unit price")
        BigDecimal tokenUnitPrice,
        @Schema(description = "Deploy tech fee")
        BigDecimal deployTechFee,
        @Schema(description = "Status")
        Integer status,
        @Schema(description = "Sort number")
        Integer sortNo
) {
}

package com.compute.rental.modules.product.dto;

import java.math.BigDecimal;

public record AiModelResponse(
        Long id,
        String modelCode,
        String modelName,
        String vendorName,
        String logoUrl,
        BigDecimal monthlyTokenConsumptionTrillion,
        BigDecimal tokenUnitPrice,
        BigDecimal deployTechFee
) {
}

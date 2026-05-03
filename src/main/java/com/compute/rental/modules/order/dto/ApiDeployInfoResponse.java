package com.compute.rental.modules.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ApiDeployInfoResponse(
        String orderNo,
        String orderStatus,
        String credentialNo,
        String tokenStatus,
        String modelNameSnapshot,
        BigDecimal deployFeeSnapshot,
        String apiName,
        String apiBaseUrl,
        String tokenMasked,
        String deployOrderStatus,
        LocalDateTime paidAt
) {
}

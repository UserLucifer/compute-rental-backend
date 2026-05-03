package com.compute.rental.modules.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ApiDeployOrderResponse(
        String deployNo,
        @Schema(description = "用户名称")
        String userName,
        String orderNo,
        String credentialNo,
        String modelNameSnapshot,
        BigDecimal deployFeeAmount,
        String status,
        String walletTxNo,
        LocalDateTime paidAt,
        LocalDateTime createdAt
) {
}

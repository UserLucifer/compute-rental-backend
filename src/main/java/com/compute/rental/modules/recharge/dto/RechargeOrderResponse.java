package com.compute.rental.modules.recharge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RechargeOrderResponse(
        String rechargeNo,
        @Schema(description = "用户名称")
        String userName,
        Long channelId,
        String currency,
        String channelName,
        String network,
        String displayUrl,
        String accountNo,
        BigDecimal applyAmount,
        BigDecimal actualAmount,
        String externalTxNo,
        String paymentProofUrl,
        String userRemark,
        String status,
        Long reviewedBy,
        LocalDateTime reviewedAt,
        String reviewRemark,
        LocalDateTime creditedAt,
        String walletTxNo,
        LocalDateTime createdAt
) {
}

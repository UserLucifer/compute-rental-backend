package com.compute.rental.modules.recharge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateRechargeOrderRequest(
        @NotNull
        Long channelId,

        @NotNull
        @DecimalMin(value = "0.00000000", inclusive = false)
        BigDecimal applyAmount,

        @Size(max = 128)
        String externalTxNo,

        @Size(max = 255)
        String paymentProofUrl,

        @Size(max = 255)
        String userRemark,

        @Schema(description = "客户端请求幂等号，同一次用户点击/重试必须保持一致，新一次主动创建必须重新生成")
        @NotBlank
        @Size(max = 64)
        String clientRequestId
) {
}

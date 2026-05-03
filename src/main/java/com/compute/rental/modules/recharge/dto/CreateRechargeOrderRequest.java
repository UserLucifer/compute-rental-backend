package com.compute.rental.modules.recharge.dto;

import jakarta.validation.constraints.DecimalMin;
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
        String userRemark
) {
}

package com.compute.rental.modules.recharge.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record AdminApproveRechargeRequest(
        @NotNull
        @DecimalMin(value = "0.00000000", inclusive = false)
        BigDecimal actualAmount,

        @Size(max = 255)
        String reviewRemark
) {
}

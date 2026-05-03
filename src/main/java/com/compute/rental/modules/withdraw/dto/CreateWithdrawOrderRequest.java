package com.compute.rental.modules.withdraw.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateWithdrawOrderRequest(
        @NotBlank
        @Size(max = 64)
        String network,

        @Size(max = 64)
        String accountName,

        @NotBlank
        @Size(max = 255)
        String accountNo,

        @NotNull
        @DecimalMin(value = "0.00000000", inclusive = false)
        BigDecimal applyAmount
) {
}

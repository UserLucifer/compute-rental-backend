package com.compute.rental.modules.recharge.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateRechargeChannelRequest(
        @NotBlank
        @Size(max = 64)
        String channelCode,

        @NotBlank
        @Size(max = 64)
        String channelName,

        @Size(max = 64)
        String network,

        @Size(max = 255)
        String displayUrl,

        @Size(max = 128)
        String accountName,

        @Size(max = 255)
        String accountNo,

        @DecimalMin(value = "0.00000000")
        @Digits(integer = 12, fraction = 8)
        BigDecimal minAmount,

        @DecimalMin(value = "0.00000000")
        @Digits(integer = 12, fraction = 8)
        BigDecimal maxAmount,

        @NotNull
        @DecimalMin(value = "0.00000000")
        @Digits(integer = 4, fraction = 8)
        BigDecimal feeRate,

        @NotNull
        Integer sortNo,

        @NotNull
        @Min(0)
        @Max(1)
        Integer status
) {
}

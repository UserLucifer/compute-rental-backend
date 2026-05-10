package com.compute.rental.modules.system.dto;

import com.compute.rental.common.enums.WalletTransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record AdminWalletAdjustRequest(
        @Schema(description = "Adjustment direction. Only IN and OUT are supported.")
        @NotNull
        WalletTransactionType txType,

        @Schema(description = "Adjustment amount")
        @NotNull
        @DecimalMin(value = "0.00000000", inclusive = false)
        BigDecimal amount,

        @Schema(description = "Unique adjustment number for idempotency")
        @NotBlank
        @Size(max = 64)
        String adjustNo,

        @Schema(description = "Adjustment reason")
        @NotBlank
        @Size(max = 255)
        String reason
) {
}

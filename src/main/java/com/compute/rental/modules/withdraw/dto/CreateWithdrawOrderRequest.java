package com.compute.rental.modules.withdraw.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
        BigDecimal applyAmount,

        @Schema(description = "客户端请求幂等号，同一次用户点击/重试必须保持一致，新一次主动创建必须重新生成")
        @NotBlank
        @Size(max = 64)
        String clientRequestId
) {
}

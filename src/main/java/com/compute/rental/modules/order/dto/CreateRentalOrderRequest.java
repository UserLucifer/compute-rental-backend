package com.compute.rental.modules.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRentalOrderRequest(
        @NotNull
        Long productId,

        @NotNull
        Long aiModelId,

        @NotNull
        Long cycleRuleId,

        @Schema(description = "客户端请求幂等号，同一次用户点击/重试必须保持一致，新一次主动创建必须重新生成")
        @NotBlank
        @Size(max = 64)
        String clientRequestId
) {
}

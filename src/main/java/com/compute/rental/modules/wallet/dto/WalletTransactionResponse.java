package com.compute.rental.modules.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletTransactionResponse(
        String txNo,
        @Schema(description = "用户名称")
        String userName,
        String txType,
        BigDecimal amount,
        BigDecimal beforeAvailableBalance,
        BigDecimal afterAvailableBalance,
        BigDecimal beforeFrozenBalance,
        BigDecimal afterFrozenBalance,
        String bizType,
        String bizOrderNo,
        String remark,
        LocalDateTime createdAt
) {
}

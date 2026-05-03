package com.compute.rental.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record DashboardFinanceResponse(
        @Schema(description = "Today recharge amount")
        BigDecimal todayRechargeAmount,
        @Schema(description = "Today withdraw amount")
        BigDecimal todayWithdrawAmount,
        @Schema(description = "Today profit amount")
        BigDecimal todayProfitAmount,
        @Schema(description = "Today commission amount")
        BigDecimal todayCommissionAmount,
        @Schema(description = "Wallet total available balance")
        BigDecimal walletTotalAvailableBalance,
        @Schema(description = "Wallet total frozen balance")
        BigDecimal walletTotalFrozenBalance
) {
}

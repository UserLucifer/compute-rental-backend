package com.compute.rental.modules.wallet.dto;

import java.math.BigDecimal;

public record WalletMeResponse(
        String currency,
        BigDecimal availableBalance,
        BigDecimal frozenBalance,
        BigDecimal totalRecharge,
        BigDecimal totalWithdraw,
        BigDecimal totalProfit,
        BigDecimal totalCommission
) {
}

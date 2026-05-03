package com.compute.rental.modules.order.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProfitRecordResponse(
        String profitNo,
        String orderNo,
        String productNameSnapshot,
        String aiModelNameSnapshot,
        LocalDate profitDate,
        Long gpuDailyTokenSnapshot,
        BigDecimal tokenPriceSnapshot,
        BigDecimal yieldMultiplierSnapshot,
        BigDecimal baseProfitAmount,
        BigDecimal finalProfitAmount,
        String status,
        String walletTxNo,
        Integer commissionGenerated,
        LocalDateTime settledAt
) {
}

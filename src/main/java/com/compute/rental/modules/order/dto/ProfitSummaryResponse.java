package com.compute.rental.modules.order.dto;

import java.math.BigDecimal;

public record ProfitSummaryResponse(
        BigDecimal totalProfit,
        BigDecimal todayProfit,
        BigDecimal yesterdayProfit,
        BigDecimal currentMonthProfit,
        Long settledProfitCount
) {
}

package com.compute.rental.modules.commission.dto;

import java.math.BigDecimal;

public record CommissionSummaryResponse(
        BigDecimal totalCommission,
        BigDecimal todayCommission,
        BigDecimal yesterdayCommission,
        BigDecimal currentMonthCommission,
        BigDecimal level1Commission,
        BigDecimal level2Commission,
        BigDecimal level3Commission
) {
}

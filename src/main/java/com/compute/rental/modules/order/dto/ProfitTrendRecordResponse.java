package com.compute.rental.modules.order.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProfitTrendRecordResponse(
        LocalDate profitDate,
        BigDecimal finalProfitAmount,
        Long recordCount
) {
}

package com.compute.rental.common.query;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfitTrendRow {

    private LocalDate profitDate;

    private BigDecimal finalProfitAmount;

    private Long recordCount;
}

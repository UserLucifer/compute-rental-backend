package com.compute.rental.common.query;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfitSummaryAggregateRow {

    private BigDecimal todayProfit;

    private BigDecimal yesterdayProfit;

    private BigDecimal currentMonthProfit;

    private Long settledProfitCount;
}

package com.compute.rental.modules.order.dto;

import java.util.List;

public record ProfitTrendResponse(
        List<ProfitTrendRecordResponse> records
) {
}

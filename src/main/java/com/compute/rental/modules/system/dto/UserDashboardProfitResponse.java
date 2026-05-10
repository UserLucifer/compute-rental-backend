package com.compute.rental.modules.system.dto;

import com.compute.rental.modules.order.dto.ProfitSummaryResponse;

public record UserDashboardProfitResponse(
        ProfitSummaryResponse summary
) {
}

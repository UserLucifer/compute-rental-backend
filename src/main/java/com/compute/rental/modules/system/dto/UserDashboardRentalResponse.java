package com.compute.rental.modules.system.dto;

import com.compute.rental.modules.order.dto.RentalOrderSummaryResponse;
import java.util.List;

public record UserDashboardRentalResponse(
        Long runningOrderCount,
        Long pendingPayOrderCount,
        List<RentalOrderSummaryResponse> recentOrders
) {
}

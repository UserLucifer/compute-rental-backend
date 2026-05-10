package com.compute.rental.modules.system.dto;

import com.compute.rental.modules.commission.dto.TeamSummaryResponse;
import com.compute.rental.modules.wallet.dto.WalletMeResponse;

public record UserDashboardOverviewResponse(
        WalletMeResponse wallet,
        UserDashboardRentalResponse rental,
        UserDashboardProfitResponse profit,
        TeamSummaryResponse team
) {
}

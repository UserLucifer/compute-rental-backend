package com.compute.rental.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record SchedulerConfigResponse(
        @Schema(description = "Daily profit scheduler cron")
        String dailyProfitCron,
        @Schema(description = "Order expire settlement scheduler cron")
        String orderExpireSettleCron,
        @Schema(description = "Auto pause scheduler cron")
        String autoPauseCron,
        @Schema(description = "Deploy fee timeout cancel scheduler cron")
        String deployFeeTimeoutCancelCron,
        @Schema(description = "Commission generate scheduler cron")
        String commissionGenerateCron,
        @Schema(description = "Auto pause delay after deploy fee payment")
        String autoPauseDelay
) {
}

package com.compute.rental.scheduler;

import com.compute.rental.common.api.ApiResponse;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.modules.system.dto.SchedulerConfigResponse;
import com.compute.rental.modules.system.dto.SchedulerLogResponse;
import com.compute.rental.modules.system.service.AdminLogService;
import com.compute.rental.modules.system.service.SchedulerLogService;
import com.compute.rental.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Scheduler")
@RestController
@RequestMapping("/api/admin/scheduler")
public class AdminSchedulerController {

    private static final String DEFAULT_DAILY_PROFIT_CRON = "0 0 0 * * *";
    private static final String DEFAULT_ORDER_EXPIRE_SETTLE_CRON = "0 * * * * *";
    private static final String DEFAULT_AUTO_PAUSE_CRON = "* * * * * *";
    private static final String DEFAULT_DEPLOY_FEE_TIMEOUT_CANCEL_CRON = "* * * * * *";
    private static final String DEFAULT_COMMISSION_GENERATE_CRON = "0 */5 * * * *";
    private static final String DEFAULT_AUTO_PAUSE_DELAY = "24h";

    private final RentalActivationScheduler rentalActivationScheduler;
    private final ProfitSettlementScheduler profitSettlementScheduler;
    private final CommissionGenerateScheduler commissionGenerateScheduler;
    private final AdminLogService adminLogService;
    private final SchedulerLogService schedulerLogService;
    private final Environment environment;

    public AdminSchedulerController(
            RentalActivationScheduler rentalActivationScheduler,
            ProfitSettlementScheduler profitSettlementScheduler,
            CommissionGenerateScheduler commissionGenerateScheduler,
            AdminLogService adminLogService,
            SchedulerLogService schedulerLogService,
            Environment environment
    ) {
        this.rentalActivationScheduler = rentalActivationScheduler;
        this.profitSettlementScheduler = profitSettlementScheduler;
        this.commissionGenerateScheduler = commissionGenerateScheduler;
        this.adminLogService = adminLogService;
        this.schedulerLogService = schedulerLogService;
        this.environment = environment;
    }

    @Operation(summary = "Admin scheduler runtime config")
    @GetMapping("/config")
    public ApiResponse<SchedulerConfigResponse> config() {
        return ApiResponse.success(new SchedulerConfigResponse(
                property("app.scheduler.daily-profit-cron", DEFAULT_DAILY_PROFIT_CRON),
                property("app.scheduler.order-expire-settle-cron", DEFAULT_ORDER_EXPIRE_SETTLE_CRON),
                property("app.scheduler.auto-pause-cron", DEFAULT_AUTO_PAUSE_CRON),
                deployFeeTimeoutCancelCron(),
                property("app.scheduler.commission-generate-cron", DEFAULT_COMMISSION_GENERATE_CRON),
                property("app.order.auto-pause-delay", DEFAULT_AUTO_PAUSE_DELAY)
        ));
    }

    @Operation(summary = "Admin scheduler logs")
    @GetMapping("/logs")
    public ApiResponse<PageResult<SchedulerLogResponse>> logs(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String taskName
    ) {
        return ApiResponse.success(schedulerLogService.pageLogs(pageNo, pageSize, taskName));
    }

    @Operation(summary = "Run deploy fee timeout cancel scheduler")
    @PostMapping("/deploy-fee-timeout-cancel/run")
    public ApiResponse<SchedulerRunResult> runDeployFeeTimeoutCancel(HttpServletRequest request) {
        var result = rentalActivationScheduler.runDeployFeeTimeoutCancel();
        logSchedulerRun(result, request);
        return ApiResponse.success(result);
    }

    @Operation(summary = "Run deploy fee timeout cancel scheduler (legacy path)")
    @PostMapping("/activation-timeout-cancel/run")
    public ApiResponse<SchedulerRunResult> runActivationTimeoutCancel(HttpServletRequest request) {
        return runDeployFeeTimeoutCancel(request);
    }

    @Operation(summary = "Run auto pause scheduler")
    @PostMapping("/auto-pause/run")
    public ApiResponse<SchedulerRunResult> runAutoPause(HttpServletRequest request) {
        var result = rentalActivationScheduler.runAutoPause();
        logSchedulerRun(result, request);
        return ApiResponse.success(result);
    }

    @Operation(summary = "Run daily profit scheduler")
    @PostMapping("/daily-profit/run")
    public ApiResponse<SchedulerRunResult> runDailyProfit(HttpServletRequest request) {
        var result = profitSettlementScheduler.runDailyProfit();
        logSchedulerRun(result, request);
        return ApiResponse.success(result);
    }

    @Operation(summary = "Run expire settlement scheduler")
    @PostMapping("/expire-settlement/run")
    public ApiResponse<SchedulerRunResult> runExpireSettlement(HttpServletRequest request) {
        var result = profitSettlementScheduler.runExpireSettlement();
        logSchedulerRun(result, request);
        return ApiResponse.success(result);
    }

    @Operation(summary = "Run commission generate scheduler")
    @PostMapping("/commission-generate/run")
    public ApiResponse<SchedulerRunResult> runCommissionGenerate(HttpServletRequest request) {
        var result = commissionGenerateScheduler.runCommissionGenerate();
        logSchedulerRun(result, request);
        return ApiResponse.success(result);
    }

    private void logSchedulerRun(SchedulerRunResult result, HttpServletRequest request) {
        var admin = CurrentUser.requiredAdmin();
        adminLogService.log(admin.id(), AdminLogService.RUN_SCHEDULER, "scheduler_log", null,
                null, result.toString(), "Run scheduler " + result.taskName(), adminLogService.clientIp(request));
    }

    private String deployFeeTimeoutCancelCron() {
        var value = environment.getProperty("app.scheduler.deploy-fee-timeout-cancel-cron");
        if (StringUtils.hasText(value)) {
            return value.trim();
        }
        return property("app.scheduler.activation-timeout-cancel-cron", DEFAULT_DEPLOY_FEE_TIMEOUT_CANCEL_CRON);
    }

    private String property(String key, String defaultValue) {
        var value = environment.getProperty(key);
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }
}

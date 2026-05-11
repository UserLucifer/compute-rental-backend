package com.compute.rental.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.compute.rental.common.enums.RentalOrderStatus;
import com.compute.rental.common.enums.SchedulerLogStatus;
import com.compute.rental.common.util.DateTimeUtils;
import com.compute.rental.modules.order.entity.RentalOrder;
import com.compute.rental.modules.order.mapper.RentalOrderMapper;
import com.compute.rental.modules.system.entity.SchedulerLog;
import com.compute.rental.modules.system.service.SchedulerLogService;
import com.compute.rental.modules.system.service.SysConfigDefaults;
import com.compute.rental.modules.system.service.SysConfigService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RentalActivationScheduler {

    private static final Logger log = LoggerFactory.getLogger(RentalActivationScheduler.class);
    private static final String DEPLOY_FEE_TIMEOUT_LOCK = "scheduler:deploy_fee_timeout_cancel";
    private static final String AUTO_PAUSE_LOCK = "scheduler:auto_pause";
    private static final Duration DEPLOY_FEE_TIMEOUT_LOCK_TTL = Duration.ofSeconds(30);
    private static final Duration AUTO_PAUSE_LOCK_TTL = Duration.ofSeconds(30);
    private static final int DEFAULT_DEPLOY_FEE_TIMEOUT_MINUTES = 15;
    private static final long DEPLOY_FEE_TIMEOUT_PAGE_SIZE = 100L;
    private static final long AUTO_PAUSE_PAGE_SIZE = 100L;

    private final SchedulerLockTemplate schedulerLockTemplate;
    private final SchedulerLogService schedulerLogService;
    private final SysConfigService sysConfigService;
    private final RentalOrderMapper rentalOrderMapper;
    private final RentalActivationSchedulerProcessor processor;

    public RentalActivationScheduler(
            SchedulerLockTemplate schedulerLockTemplate,
            SchedulerLogService schedulerLogService,
            SysConfigService sysConfigService,
            RentalOrderMapper rentalOrderMapper,
            RentalActivationSchedulerProcessor processor
    ) {
        this.schedulerLockTemplate = schedulerLockTemplate;
        this.schedulerLogService = schedulerLogService;
        this.sysConfigService = sysConfigService;
        this.rentalOrderMapper = rentalOrderMapper;
        this.processor = processor;
    }

    @Scheduled(cron = "${app.scheduler.deploy-fee-timeout-cancel-cron:${app.scheduler.activation-timeout-cancel-cron:* * * * * *}}")
    public void scheduledDeployFeeTimeoutCancel() {
        runDeployFeeTimeoutCancel(false);
    }

    @Scheduled(cron = "${app.scheduler.auto-pause-cron:* * * * * *}")
    public void scheduledAutoPause() {
        runAutoPause(false);
    }

    public SchedulerRunResult runDeployFeeTimeoutCancel() {
        return runDeployFeeTimeoutCancel(true);
    }

    private SchedulerRunResult runDeployFeeTimeoutCancel(boolean writeNoopLog) {
        var result = new AtomicReference<SchedulerRunResult>();
        var acquired = schedulerLockTemplate.runWithLock(DEPLOY_FEE_TIMEOUT_LOCK, DEPLOY_FEE_TIMEOUT_LOCK_TTL,
                () -> result.set(doDeployFeeTimeoutCancel(writeNoopLog)));
        if (!acquired) {
            return skipped(SchedulerTaskNames.DEPLOY_FEE_TIMEOUT_CANCEL);
        }
        return result.get();
    }

    public SchedulerRunResult runActivationTimeoutCancel() {
        return runDeployFeeTimeoutCancel();
    }

    public SchedulerRunResult runAutoPause() {
        return runAutoPause(true);
    }

    private SchedulerRunResult runAutoPause(boolean writeNoopLog) {
        var result = new AtomicReference<SchedulerRunResult>();
        var acquired = schedulerLockTemplate.runWithLock(AUTO_PAUSE_LOCK, AUTO_PAUSE_LOCK_TTL,
                () -> result.set(doAutoPause(writeNoopLog)));
        if (!acquired) {
            return skipped(SchedulerTaskNames.AUTO_PAUSE);
        }
        return result.get();
    }

    private SchedulerRunResult doDeployFeeTimeoutCancel(boolean writeNoopLog) {
        var taskName = SchedulerTaskNames.DEPLOY_FEE_TIMEOUT_CANCEL;
        var successCount = 0;
        var failCount = 0;
        var totalCount = 0;
        var errors = new ArrayList<String>();
        SchedulerLog schedulerLog = null;
        var timeoutMinutes = sysConfigService.getInteger(
                SysConfigDefaults.ORDER_PENDING_ACTIVATION_TIMEOUT_MINUTES,
                DEFAULT_DEPLOY_FEE_TIMEOUT_MINUTES);
        if (timeoutMinutes == null || timeoutMinutes <= 0) {
            log.warn("Invalid deploy fee timeout config, fallback to {} minutes", DEFAULT_DEPLOY_FEE_TIMEOUT_MINUTES);
            timeoutMinutes = DEFAULT_DEPLOY_FEE_TIMEOUT_MINUTES;
        }
        var cutoffTime = DateTimeUtils.now().minusMinutes(timeoutMinutes);
        var lastOrderId = 0L;
        java.util.List<RentalOrder> orders;
        do {
            orders = rentalOrderMapper.selectList(new LambdaQueryWrapper<RentalOrder>()
                    .gt(RentalOrder::getId, lastOrderId)
                    .eq(RentalOrder::getOrderStatus, RentalOrderStatus.PENDING_ACTIVATION.name())
                    .le(RentalOrder::getApiGeneratedAt, cutoffTime)
                    .orderByAsc(RentalOrder::getId)
                    .last("LIMIT " + DEPLOY_FEE_TIMEOUT_PAGE_SIZE));
            if (schedulerLog == null && (writeNoopLog || !orders.isEmpty())) {
                schedulerLog = schedulerLogService.start(taskName);
            }
            for (var order : orders) {
                lastOrderId = order.getId();
                totalCount++;
                try {
                    processor.cancelDeployFeeTimeout(order.getId(), cutoffTime);
                    successCount++;
                } catch (Exception ex) {
                    failCount++;
                    errors.add(order.getOrderNo() + ":" + ex.getMessage());
                    log.warn("Deploy fee timeout cancel failed, orderNo={}", order.getOrderNo(), ex);
                }
            }
        } while (orders.size() == DEPLOY_FEE_TIMEOUT_PAGE_SIZE);
        var errorMessage = errors.isEmpty() ? null : String.join("; ", errors);
        if (schedulerLog != null) {
            schedulerLogService.finish(schedulerLog, totalCount, successCount, failCount, errorMessage);
        }
        return result(taskName, totalCount, successCount, failCount, errorMessage);
    }

    private SchedulerRunResult doAutoPause(boolean writeNoopLog) {
        var taskName = SchedulerTaskNames.AUTO_PAUSE;
        var successCount = 0;
        var failCount = 0;
        var totalCount = 0;
        var errors = new ArrayList<String>();
        SchedulerLog schedulerLog = null;
        var now = DateTimeUtils.now();
        var lastOrderId = 0L;
        java.util.List<RentalOrder> orders;
        do {
            orders = rentalOrderMapper.selectList(new LambdaQueryWrapper<RentalOrder>()
                    .gt(RentalOrder::getId, lastOrderId)
                    .in(RentalOrder::getOrderStatus, RentalOrderStatus.RUNNING.name(), RentalOrderStatus.ACTIVATING.name())
                    .le(RentalOrder::getAutoPauseAt, now)
                    .orderByAsc(RentalOrder::getId)
                    .last("LIMIT " + AUTO_PAUSE_PAGE_SIZE));
            if (schedulerLog == null && (writeNoopLog || !orders.isEmpty())) {
                schedulerLog = schedulerLogService.start(taskName);
            }
            for (var order : orders) {
                lastOrderId = order.getId();
                totalCount++;
                try {
                    processor.autoPause(order.getId(), now);
                    successCount++;
                } catch (Exception ex) {
                    failCount++;
                    errors.add(order.getOrderNo() + ":" + ex.getMessage());
                    log.warn("Auto pause failed, orderNo={}", order.getOrderNo(), ex);
                }
            }
        } while (orders.size() == AUTO_PAUSE_PAGE_SIZE);
        var errorMessage = errors.isEmpty() ? null : String.join("; ", errors);
        if (schedulerLog != null) {
            schedulerLogService.finish(schedulerLog, totalCount, successCount, failCount, errorMessage);
        }
        return result(taskName, totalCount, successCount, failCount, errorMessage);
    }

    private SchedulerRunResult result(String taskName, int totalCount, int successCount, int failCount,
                                      String errorMessage) {
        var status = failCount == 0 ? SchedulerLogStatus.SUCCESS
                : successCount > 0 || totalCount > failCount ? SchedulerLogStatus.PARTIAL_FAIL
                : SchedulerLogStatus.FAIL;
        return new SchedulerRunResult(taskName, totalCount, successCount, failCount, status.name(), errorMessage);
    }

    private SchedulerRunResult skipped(String taskName) {
        return new SchedulerRunResult(taskName, 0, 0, 0, "SKIPPED", null);
    }
}

package com.compute.rental.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.compute.rental.common.enums.RentalOrderStatus;
import com.compute.rental.common.enums.SchedulerLogStatus;
import com.compute.rental.common.util.DateTimeUtils;
import com.compute.rental.modules.order.entity.RentalOrder;
import com.compute.rental.modules.order.mapper.RentalOrderMapper;
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
    private static final String ACTIVATION_TIMEOUT_LOCK = "scheduler:activation_timeout_cancel";
    private static final String AUTO_PAUSE_LOCK = "scheduler:auto_pause";
    private static final Duration LOCK_TTL = Duration.ofMinutes(9);

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

    @Scheduled(cron = "${app.scheduler.activation-timeout-cancel-cron:0 */10 * * * *}")
    public void scheduledActivationTimeoutCancel() {
        runActivationTimeoutCancel();
    }

    @Scheduled(cron = "${app.scheduler.auto-pause-cron:0 */5 * * * *}")
    public void scheduledAutoPause() {
        runAutoPause();
    }

    public SchedulerRunResult runActivationTimeoutCancel() {
        var result = new AtomicReference<SchedulerRunResult>();
        var acquired = schedulerLockTemplate.runWithLock(ACTIVATION_TIMEOUT_LOCK, LOCK_TTL,
                () -> result.set(doActivationTimeoutCancel()));
        if (!acquired) {
            return skipped(SchedulerTaskNames.ACTIVATION_TIMEOUT_CANCEL);
        }
        return result.get();
    }

    public SchedulerRunResult runAutoPause() {
        var result = new AtomicReference<SchedulerRunResult>();
        var acquired = schedulerLockTemplate.runWithLock(AUTO_PAUSE_LOCK, LOCK_TTL,
                () -> result.set(doAutoPause()));
        if (!acquired) {
            return skipped(SchedulerTaskNames.AUTO_PAUSE);
        }
        return result.get();
    }

    private SchedulerRunResult doActivationTimeoutCancel() {
        var taskName = SchedulerTaskNames.ACTIVATION_TIMEOUT_CANCEL;
        var schedulerLog = schedulerLogService.start(taskName);
        var successCount = 0;
        var failCount = 0;
        var errors = new ArrayList<String>();
        var timeoutMinutes = sysConfigService.getInteger(
                SysConfigDefaults.ORDER_PENDING_ACTIVATION_TIMEOUT_MINUTES, 60);
        var cutoffTime = DateTimeUtils.now().minusMinutes(timeoutMinutes);
        var orders = rentalOrderMapper.selectList(new LambdaQueryWrapper<RentalOrder>()
                .eq(RentalOrder::getOrderStatus, RentalOrderStatus.PENDING_ACTIVATION.name())
                .le(RentalOrder::getApiGeneratedAt, cutoffTime)
                .orderByAsc(RentalOrder::getId));
        for (var order : orders) {
            try {
                processor.cancelActivationTimeout(order.getId(), cutoffTime);
                successCount++;
            } catch (Exception ex) {
                failCount++;
                errors.add(order.getOrderNo() + ":" + ex.getMessage());
                log.warn("Activation timeout cancel failed, orderNo={}", order.getOrderNo(), ex);
            }
        }
        var errorMessage = errors.isEmpty() ? null : String.join("; ", errors);
        schedulerLogService.finish(schedulerLog, orders.size(), successCount, failCount, errorMessage);
        return result(taskName, orders.size(), successCount, failCount, errorMessage);
    }

    private SchedulerRunResult doAutoPause() {
        var taskName = SchedulerTaskNames.AUTO_PAUSE;
        var schedulerLog = schedulerLogService.start(taskName);
        var successCount = 0;
        var failCount = 0;
        var errors = new ArrayList<String>();
        var now = DateTimeUtils.now();
        var orders = rentalOrderMapper.selectList(new LambdaQueryWrapper<RentalOrder>()
                .eq(RentalOrder::getOrderStatus, RentalOrderStatus.ACTIVATING.name())
                .le(RentalOrder::getAutoPauseAt, now)
                .orderByAsc(RentalOrder::getId));
        for (var order : orders) {
            try {
                processor.autoPause(order.getId(), now);
                successCount++;
            } catch (Exception ex) {
                failCount++;
                errors.add(order.getOrderNo() + ":" + ex.getMessage());
                log.warn("Auto pause failed, orderNo={}", order.getOrderNo(), ex);
            }
        }
        var errorMessage = errors.isEmpty() ? null : String.join("; ", errors);
        schedulerLogService.finish(schedulerLog, orders.size(), successCount, failCount, errorMessage);
        return result(taskName, orders.size(), successCount, failCount, errorMessage);
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

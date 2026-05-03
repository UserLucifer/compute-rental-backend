package com.compute.rental.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.compute.rental.common.enums.RentalOrderStatus;
import com.compute.rental.common.enums.SchedulerLogStatus;
import com.compute.rental.common.util.DateTimeUtils;
import com.compute.rental.modules.order.entity.RentalOrder;
import com.compute.rental.modules.order.mapper.RentalOrderMapper;
import com.compute.rental.modules.system.service.SchedulerLogService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ProfitSettlementScheduler {

    private static final Logger log = LoggerFactory.getLogger(ProfitSettlementScheduler.class);
    private static final String DAILY_PROFIT_LOCK = "scheduler:daily_profit:lock";
    private static final String EXPIRE_SETTLEMENT_LOCK = "scheduler:expire_settlement:lock";
    private static final Duration LOCK_TTL = Duration.ofSeconds(3600);
    private static final long PAGE_SIZE = 100L;

    private final SchedulerLockTemplate schedulerLockTemplate;
    private final SchedulerLogService schedulerLogService;
    private final RentalOrderMapper rentalOrderMapper;
    private final ProfitSettlementSchedulerProcessor processor;

    public ProfitSettlementScheduler(
            SchedulerLockTemplate schedulerLockTemplate,
            SchedulerLogService schedulerLogService,
            RentalOrderMapper rentalOrderMapper,
            ProfitSettlementSchedulerProcessor processor
    ) {
        this.schedulerLockTemplate = schedulerLockTemplate;
        this.schedulerLogService = schedulerLogService;
        this.rentalOrderMapper = rentalOrderMapper;
        this.processor = processor;
    }

    @Scheduled(cron = "${app.scheduler.daily-profit-cron:0 5 0 * * *}")
    public void scheduledDailyProfit() {
        runDailyProfit();
    }

    @Scheduled(cron = "${app.scheduler.order-expire-settle-cron:0 10 0 * * *}")
    public void scheduledExpireSettlement() {
        runExpireSettlement();
    }

    public SchedulerRunResult runDailyProfit() {
        var result = new AtomicReference<SchedulerRunResult>();
        var acquired = schedulerLockTemplate.runWithLock(DAILY_PROFIT_LOCK, LOCK_TTL,
                () -> result.set(doDailyProfit()));
        if (!acquired) {
            return skipped(SchedulerTaskNames.DAILY_PROFIT);
        }
        return result.get();
    }

    public SchedulerRunResult runExpireSettlement() {
        var result = new AtomicReference<SchedulerRunResult>();
        var acquired = schedulerLockTemplate.runWithLock(EXPIRE_SETTLEMENT_LOCK, LOCK_TTL,
                () -> result.set(doExpireSettlement()));
        if (!acquired) {
            return skipped(SchedulerTaskNames.EXPIRE_SETTLEMENT);
        }
        return result.get();
    }

    private SchedulerRunResult doDailyProfit() {
        var taskName = SchedulerTaskNames.DAILY_PROFIT;
        var schedulerLog = schedulerLogService.start(taskName);
        var today = DateTimeUtils.today();
        var now = DateTimeUtils.now();
        var nextDayStart = today.plusDays(1).atStartOfDay();
        var successCount = 0;
        var failCount = 0;
        var totalCount = 0;
        var errors = new ArrayList<String>();
        var current = 1L;
        Page<RentalOrder> result;
        do {
            result = rentalOrderMapper.selectPage(new Page<>(current, PAGE_SIZE), new LambdaQueryWrapper<RentalOrder>()
                    .eq(RentalOrder::getOrderStatus, RentalOrderStatus.RUNNING.name())
                    .le(RentalOrder::getProfitStartAt, now)
                    .ge(RentalOrder::getProfitEndAt, nextDayStart)
                    .orderByAsc(RentalOrder::getId));
            for (var order : result.getRecords()) {
                totalCount++;
                try {
                    processor.generateDailyProfit(order.getId(), today, now);
                    successCount++;
                } catch (Exception ex) {
                    failCount++;
                    errors.add(order.getOrderNo() + ":" + ex.getMessage());
                    log.warn("Daily profit failed, orderNo={}", order.getOrderNo(), ex);
                }
            }
            current++;
        } while (result.hasNext());
        var errorMessage = errors.isEmpty() ? null : String.join("; ", errors);
        schedulerLogService.finish(schedulerLog, totalCount, successCount, failCount, errorMessage);
        return result(taskName, totalCount, successCount, failCount, errorMessage);
    }

    private SchedulerRunResult doExpireSettlement() {
        var taskName = SchedulerTaskNames.EXPIRE_SETTLEMENT;
        var schedulerLog = schedulerLogService.start(taskName);
        var now = DateTimeUtils.now();
        var successCount = 0;
        var failCount = 0;
        var totalCount = 0;
        var errors = new ArrayList<String>();
        var current = 1L;
        Page<RentalOrder> result;
        do {
            result = rentalOrderMapper.selectPage(new Page<>(current, PAGE_SIZE), new LambdaQueryWrapper<RentalOrder>()
                    .eq(RentalOrder::getOrderStatus, RentalOrderStatus.RUNNING.name())
                    .le(RentalOrder::getProfitEndAt, now)
                    .orderByAsc(RentalOrder::getId));
            for (var order : result.getRecords()) {
                totalCount++;
                try {
                    processor.expireSettle(order.getId(), now);
                    successCount++;
                } catch (Exception ex) {
                    failCount++;
                    errors.add(order.getOrderNo() + ":" + ex.getMessage());
                    log.warn("Expire settlement failed, orderNo={}", order.getOrderNo(), ex);
                }
            }
            current++;
        } while (result.hasNext());
        var errorMessage = errors.isEmpty() ? null : String.join("; ", errors);
        schedulerLogService.finish(schedulerLog, totalCount, successCount, failCount, errorMessage);
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

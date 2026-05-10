package com.compute.rental.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.compute.rental.common.enums.RentalOrderStatus;
import com.compute.rental.common.enums.SchedulerLogStatus;
import com.compute.rental.common.util.DateTimeUtils;
import com.compute.rental.modules.order.entity.RentalOrder;
import com.compute.rental.modules.order.mapper.RentalOrderMapper;
import com.compute.rental.modules.order.mapper.RentalOrderRunSegmentMapper;
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
    private final RentalOrderRunSegmentMapper runSegmentMapper;
    private final ProfitSettlementSchedulerProcessor processor;

    public ProfitSettlementScheduler(
            SchedulerLockTemplate schedulerLockTemplate,
            SchedulerLogService schedulerLogService,
            RentalOrderMapper rentalOrderMapper,
            RentalOrderRunSegmentMapper runSegmentMapper,
            ProfitSettlementSchedulerProcessor processor
    ) {
        this.schedulerLockTemplate = schedulerLockTemplate;
        this.schedulerLogService = schedulerLogService;
        this.rentalOrderMapper = rentalOrderMapper;
        this.runSegmentMapper = runSegmentMapper;
        this.processor = processor;
    }

    @Scheduled(cron = "${app.scheduler.daily-profit-cron:0 0 0 * * *}")
    public void scheduledDailyProfit() {
        runDailyProfit();
    }

    @Scheduled(cron = "${app.scheduler.order-expire-settle-cron:0 * * * * *}")
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
        var now = DateTimeUtils.now();
        var profitDate = DateTimeUtils.today().minusDays(1);
        var profitWindowStart = profitDate.atStartOfDay();
        var profitWindowEnd = profitDate.plusDays(1).atStartOfDay();
        var successCount = 0;
        var failCount = 0;
        var totalCount = 0;
        var errors = new ArrayList<String>();
        var lastOrderId = 0L;
        java.util.List<Long> orderIds;
        do {
            orderIds = runSegmentMapper.selectOverlappingOrderIds(profitWindowStart, profitWindowEnd,
                    lastOrderId, (int) PAGE_SIZE);
            for (var orderId : orderIds) {
                lastOrderId = orderId;
                totalCount++;
                try {
                    processor.generateDailyProfit(orderId, profitDate, now);
                    successCount++;
                } catch (Exception ex) {
                    failCount++;
                    errors.add(orderId + ":" + ex.getMessage());
                    log.warn("Daily profit failed, orderId={}", orderId, ex);
                }
            }
        } while (orderIds.size() == PAGE_SIZE);
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
        var lastOrderId = 0L;
        java.util.List<RentalOrder> orders;
        do {
            orders = rentalOrderMapper.selectList(new LambdaQueryWrapper<RentalOrder>()
                    .gt(RentalOrder::getId, lastOrderId)
                    .in(RentalOrder::getOrderStatus, RentalOrderStatus.RUNNING.name(), RentalOrderStatus.PAUSED.name())
                    .le(RentalOrder::getProfitEndAt, now)
                    .orderByAsc(RentalOrder::getId)
                    .last("LIMIT " + PAGE_SIZE));
            for (var order : orders) {
                lastOrderId = order.getId();
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
        } while (orders.size() == PAGE_SIZE);
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

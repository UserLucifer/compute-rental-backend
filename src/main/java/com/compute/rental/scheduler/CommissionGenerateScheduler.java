package com.compute.rental.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.compute.rental.common.enums.RecordSettleStatus;
import com.compute.rental.common.enums.SchedulerLogStatus;
import com.compute.rental.modules.commission.service.CommissionService;
import com.compute.rental.modules.order.entity.RentalProfitRecord;
import com.compute.rental.modules.order.mapper.RentalProfitRecordMapper;
import com.compute.rental.modules.system.service.SchedulerLogService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CommissionGenerateScheduler {

    private static final Logger log = LoggerFactory.getLogger(CommissionGenerateScheduler.class);
    private static final String COMMISSION_GENERATE_LOCK = "scheduler:commission_generate:lock";
    private static final Duration LOCK_TTL = Duration.ofSeconds(3600);
    private static final long PAGE_SIZE = 100L;

    private final SchedulerLockTemplate schedulerLockTemplate;
    private final SchedulerLogService schedulerLogService;
    private final RentalProfitRecordMapper profitRecordMapper;
    private final CommissionService commissionService;

    public CommissionGenerateScheduler(
            SchedulerLockTemplate schedulerLockTemplate,
            SchedulerLogService schedulerLogService,
            RentalProfitRecordMapper profitRecordMapper,
            CommissionService commissionService
    ) {
        this.schedulerLockTemplate = schedulerLockTemplate;
        this.schedulerLogService = schedulerLogService;
        this.profitRecordMapper = profitRecordMapper;
        this.commissionService = commissionService;
    }

    @Scheduled(cron = "${app.scheduler.commission-generate-cron:0 */5 * * * *}")
    public void scheduledCommissionGenerate() {
        runCommissionGenerate();
    }

    public SchedulerRunResult runCommissionGenerate() {
        var result = new AtomicReference<SchedulerRunResult>();
        var acquired = schedulerLockTemplate.runWithLock(COMMISSION_GENERATE_LOCK, LOCK_TTL,
                () -> result.set(doCommissionGenerate()));
        if (!acquired) {
            return new SchedulerRunResult(SchedulerTaskNames.COMMISSION_GENERATE, 0, 0, 0, "SKIPPED", null);
        }
        return result.get();
    }

    private SchedulerRunResult doCommissionGenerate() {
        var taskName = SchedulerTaskNames.COMMISSION_GENERATE;
        var schedulerLog = schedulerLogService.start(taskName);
        var successCount = 0;
        var failCount = 0;
        var totalCount = 0;
        var errors = new ArrayList<String>();
        var lastId = 0L;
        Page<RentalProfitRecord> result;
        do {
            result = profitRecordMapper.selectPage(new Page<>(1, PAGE_SIZE),
                    new LambdaQueryWrapper<RentalProfitRecord>()
                            .gt(RentalProfitRecord::getId, lastId)
                            .eq(RentalProfitRecord::getStatus, RecordSettleStatus.SETTLED.name())
                            .eq(RentalProfitRecord::getCommissionGenerated, 0)
                            .orderByAsc(RentalProfitRecord::getId));
            for (var record : result.getRecords()) {
                totalCount++;
                try {
                    commissionService.generateForProfit(record.getId());
                    successCount++;
                } catch (Exception ex) {
                    failCount++;
                    errors.add(record.getProfitNo() + ":" + ex.getMessage());
                    log.warn("Commission generate failed, profitNo={}", record.getProfitNo(), ex);
                }
                lastId = record.getId();
            }
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
}

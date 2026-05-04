package com.compute.rental.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.compute.rental.common.enums.SchedulerLogStatus;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.common.util.DateTimeUtils;
import com.compute.rental.modules.system.dto.SchedulerLogResponse;
import com.compute.rental.modules.system.entity.SchedulerLog;
import com.compute.rental.modules.system.mapper.SchedulerLogMapper;
import org.springframework.stereotype.Service;

@Service
public class SchedulerLogService {

    private static final int MAX_ERROR_LENGTH = 2000;

    private final SchedulerLogMapper schedulerLogMapper;

    public SchedulerLogService(SchedulerLogMapper schedulerLogMapper) {
        this.schedulerLogMapper = schedulerLogMapper;
    }

    public SchedulerLog start(String taskName) {
        var now = DateTimeUtils.now();
        var log = new SchedulerLog();
        log.setTaskName(taskName);
        log.setStartedAt(now);
        log.setTotalCount(0);
        log.setSuccessCount(0);
        log.setFailCount(0);
        log.setStatus(SchedulerLogStatus.RUNNING.name());
        log.setCreatedAt(now);
        schedulerLogMapper.insert(log);
        return log;
    }

    public void finish(SchedulerLog log, int totalCount, int successCount, int failCount, String errorMessage) {
        var status = status(totalCount, successCount, failCount);
        schedulerLogMapper.update(null, new LambdaUpdateWrapper<SchedulerLog>()
                .eq(SchedulerLog::getId, log.getId())
                .set(SchedulerLog::getFinishedAt, DateTimeUtils.now())
                .set(SchedulerLog::getTotalCount, totalCount)
                .set(SchedulerLog::getSuccessCount, successCount)
                .set(SchedulerLog::getFailCount, failCount)
                .set(SchedulerLog::getStatus, status.name())
                .set(SchedulerLog::getErrorMessage, trimError(errorMessage)));
    }

    public PageResult<SchedulerLogResponse> pageLogs(long pageNo, long pageSize, String taskName) {
        var wrapper = new LambdaQueryWrapper<SchedulerLog>()
                .eq(taskName != null && !taskName.isBlank(), SchedulerLog::getTaskName, taskName)
                .orderByDesc(SchedulerLog::getStartedAt)
                .orderByDesc(SchedulerLog::getId);
        var page = schedulerLogMapper.selectPage(Page.of(pageNo, pageSize), wrapper);
        var records = page.getRecords().stream()
                .map(this::toResponse)
                .toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    private SchedulerLogStatus status(int totalCount, int successCount, int failCount) {
        if (failCount == 0) {
            return SchedulerLogStatus.SUCCESS;
        }
        if (successCount > 0 || totalCount > failCount) {
            return SchedulerLogStatus.PARTIAL_FAIL;
        }
        return SchedulerLogStatus.FAIL;
    }

    private String trimError(String errorMessage) {
        if (errorMessage == null || errorMessage.length() <= MAX_ERROR_LENGTH) {
            return errorMessage;
        }
        return errorMessage.substring(0, MAX_ERROR_LENGTH);
    }

    private SchedulerLogResponse toResponse(SchedulerLog log) {
        return new SchedulerLogResponse(
                log.getId(),
                log.getTaskName(),
                log.getStatus(),
                log.getTotalCount(),
                log.getSuccessCount(),
                log.getFailCount(),
                log.getErrorMessage(),
                log.getStartedAt(),
                log.getFinishedAt(),
                log.getCreatedAt());
    }
}

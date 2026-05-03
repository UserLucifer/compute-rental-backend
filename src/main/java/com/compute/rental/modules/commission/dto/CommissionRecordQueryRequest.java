package com.compute.rental.modules.commission.dto;

import com.compute.rental.common.enums.RecordSettleStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

public record CommissionRecordQueryRequest(
        @Min(1)
        Integer pageNo,

        @Min(1)
        @Max(100)
        Integer pageSize,

        Integer levelNo,

        RecordSettleStatus status,

        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime startTime,

        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime endTime
) {

    public long current() {
        return pageNo == null ? 1L : pageNo.longValue();
    }

    public long size() {
        return pageSize == null ? 10L : pageSize.longValue();
    }
}

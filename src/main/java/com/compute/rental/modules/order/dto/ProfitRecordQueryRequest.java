package com.compute.rental.modules.order.dto;

import com.compute.rental.common.enums.RecordSettleStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public record ProfitRecordQueryRequest(
        @Min(1)
        Integer pageNo,

        @Min(1)
        @Max(100)
        Integer pageSize,

        Long rentalOrderId,

        String orderNo,

        @DateTimeFormat(pattern = "yyyy-MM-dd")
        LocalDate profitDate,

        RecordSettleStatus status
) {

    public long current() {
        return pageNo == null ? 1L : pageNo.longValue();
    }

    public long size() {
        return pageSize == null ? 10L : pageSize.longValue();
    }
}

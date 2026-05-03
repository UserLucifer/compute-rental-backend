package com.compute.rental.modules.order.dto;

import com.compute.rental.common.enums.RentalSettlementOrderStatus;
import com.compute.rental.common.enums.RentalSettlementType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

public record SettlementOrderQueryRequest(
        @Min(1)
        Integer pageNo,

        @Min(1)
        @Max(100)
        Integer pageSize,

        RentalSettlementType settlementType,

        RentalSettlementOrderStatus status,

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

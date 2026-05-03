package com.compute.rental.modules.recharge.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record RechargeChannelQueryRequest(
        @Min(1)
        Integer pageNo,

        @Min(1)
        @Max(100)
        Integer pageSize,

        @Min(0)
        @Max(1)
        Integer status
) {

    public long current() {
        return pageNo == null ? 1L : pageNo.longValue();
    }

    public long size() {
        return pageSize == null ? 10L : pageSize.longValue();
    }
}

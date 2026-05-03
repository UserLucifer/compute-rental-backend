package com.compute.rental.modules.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AdminSysConfigQueryRequest(
        @Min(1)
        Integer pageNo,

        @Min(1)
        @Max(100)
        Integer pageSize,

        String configKey
) {

    public long current() {
        return pageNo == null ? 1L : pageNo.longValue();
    }

    public long size() {
        return pageSize == null ? 20L : pageSize.longValue();
    }
}

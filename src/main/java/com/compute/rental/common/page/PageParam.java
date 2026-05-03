package com.compute.rental.common.page;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PageParam(
        @Min(1) Integer pageNo,
        @Min(1) @Max(100) Integer pageSize
) {

    public long current() {
        return pageNo == null ? 1L : pageNo.longValue();
    }

    public long size() {
        return pageSize == null ? 10L : pageSize.longValue();
    }
}

package com.compute.rental.modules.commission.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record TeamMemberQueryRequest(
        @Min(1)
        Integer pageNo,

        @Min(1)
        @Max(100)
        Integer pageSize,

        @Min(1)
        Integer levelDepth
) {

    public long current() {
        return pageNo == null ? 1L : pageNo.longValue();
    }

    public long size() {
        return pageSize == null ? 10L : pageSize.longValue();
    }
}

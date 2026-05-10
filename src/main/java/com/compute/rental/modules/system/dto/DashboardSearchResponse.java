package com.compute.rental.modules.system.dto;

import java.util.List;

public record DashboardSearchResponse(
        List<DashboardSearchItemResponse> records
) {
}

package com.compute.rental.modules.system.dto;

public record DashboardSearchItemResponse(
        String type,
        String title,
        String description,
        String href
) {
}

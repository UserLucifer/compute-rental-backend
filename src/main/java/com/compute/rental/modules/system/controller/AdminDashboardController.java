package com.compute.rental.modules.system.controller;

import com.compute.rental.common.api.ApiResponse;
import com.compute.rental.modules.system.dto.DashboardFinanceResponse;
import com.compute.rental.modules.system.dto.DashboardOrdersResponse;
import com.compute.rental.modules.system.dto.DashboardOverviewResponse;
import com.compute.rental.modules.system.dto.DashboardUsersResponse;
import com.compute.rental.modules.system.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Dashboard")
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    public AdminDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "Dashboard overview")
    @GetMapping("/overview")
    public ApiResponse<DashboardOverviewResponse> overview() {
        return ApiResponse.success(dashboardService.overview());
    }

    @Operation(summary = "Dashboard finance")
    @GetMapping("/finance")
    public ApiResponse<DashboardFinanceResponse> finance() {
        return ApiResponse.success(dashboardService.finance());
    }

    @Operation(summary = "Dashboard orders")
    @GetMapping("/orders")
    public ApiResponse<DashboardOrdersResponse> orders() {
        return ApiResponse.success(dashboardService.orders());
    }

    @Operation(summary = "Dashboard users")
    @GetMapping("/users")
    public ApiResponse<DashboardUsersResponse> users() {
        return ApiResponse.success(dashboardService.users());
    }
}

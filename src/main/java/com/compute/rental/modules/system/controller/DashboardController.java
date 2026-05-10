package com.compute.rental.modules.system.controller;

import com.compute.rental.common.api.ApiResponse;
import com.compute.rental.modules.system.dto.DashboardSearchResponse;
import com.compute.rental.modules.system.dto.UserDashboardOverviewResponse;
import com.compute.rental.modules.system.service.UserDashboardService;
import com.compute.rental.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dashboard")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final UserDashboardService dashboardService;

    public DashboardController(UserDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "Current user dashboard overview")
    @GetMapping("/overview")
    public ApiResponse<UserDashboardOverviewResponse> overview() {
        var currentUser = CurrentUser.required();
        return ApiResponse.success(dashboardService.overview(currentUser.id()));
    }

    @Operation(summary = "Current user dashboard search suggestions")
    @GetMapping("/search")
    public ApiResponse<DashboardSearchResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "dashboard") String scope,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage
    ) {
        var currentUser = CurrentUser.required();
        return ApiResponse.success(dashboardService.search(currentUser.id(), keyword, scope, acceptLanguage));
    }
}

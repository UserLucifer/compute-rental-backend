package com.compute.rental.modules.system.controller;

import com.compute.rental.common.api.ApiResponse;
import com.compute.rental.modules.system.dto.RedisCacheClearResponse;
import com.compute.rental.modules.system.service.AdminLogService;
import com.compute.rental.modules.system.service.RedisCacheAdminService;
import com.compute.rental.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin System Maintenance")
@RestController
@RequestMapping("/api/admin/system")
public class AdminSystemMaintenanceController {

    private final RedisCacheAdminService redisCacheAdminService;
    private final AdminLogService adminLogService;

    public AdminSystemMaintenanceController(
            RedisCacheAdminService redisCacheAdminService,
            AdminLogService adminLogService
    ) {
        this.redisCacheAdminService = redisCacheAdminService;
        this.adminLogService = adminLogService;
    }

    @Operation(summary = "Clear application Redis cache")
    @PostMapping("/cache/redis/clear")
    public ApiResponse<RedisCacheClearResponse> clearRedisCache(HttpServletRequest request) {
        var admin = CurrentUser.requiredAdmin();
        var response = redisCacheAdminService.clearApplicationCache();
        adminLogService.log(admin.id(), AdminLogService.CLEAR_REDIS_CACHE, "redis", null, null,
                "deletedCount=" + response.deletedCount(),
                "Clear Redis prefixes: " + String.join(",", response.prefixes()),
                adminLogService.clientIp(request));
        return ApiResponse.success(response);
    }
}

package com.compute.rental.modules.system.controller;

import com.compute.rental.common.api.ApiResponse;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.modules.system.dto.AdminSysConfigQueryRequest;
import com.compute.rental.modules.system.dto.AdminSysConfigResponse;
import com.compute.rental.modules.system.dto.UpdateSysConfigRequest;
import com.compute.rental.modules.system.service.AdminLogService;
import com.compute.rental.modules.system.service.SysConfigService;
import com.compute.rental.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "Admin SysConfig")
@RestController
@RequestMapping("/api/admin/sys-configs")
public class AdminSysConfigController {

    private final SysConfigService sysConfigService;
    private final AdminLogService adminLogService;

    public AdminSysConfigController(SysConfigService sysConfigService, AdminLogService adminLogService) {
        this.sysConfigService = sysConfigService;
        this.adminLogService = adminLogService;
    }

    @Operation(summary = "Admin system config list")
    @GetMapping
    public ApiResponse<PageResult<AdminSysConfigResponse>> configs(
            @Valid @ModelAttribute AdminSysConfigQueryRequest request
    ) {
        return ApiResponse.success(sysConfigService.pageAdminConfigs(request));
    }

    @Operation(summary = "Admin system config detail")
    @GetMapping("/{configKey}")
    public ApiResponse<AdminSysConfigResponse> detail(@PathVariable String configKey) {
        return ApiResponse.success(sysConfigService.getAdminConfig(configKey));
    }

    @Operation(summary = "Update system config")
    @PutMapping("/{configKey}")
    public ApiResponse<AdminSysConfigResponse> update(
            @PathVariable String configKey,
            @Valid @RequestBody UpdateSysConfigRequest request,
            HttpServletRequest httpRequest
    ) {
        var admin = CurrentUser.requiredAdmin();
        return ApiResponse.success(sysConfigService.updateAdminConfig(
                configKey,
                request,
                admin.id(),
                adminLogService.clientIp(httpRequest)
        ));
    }
}

package com.compute.rental.modules.system.controller;

import com.compute.rental.common.api.ApiResponse;
import com.compute.rental.modules.system.dto.EnumOptionResponse;
import com.compute.rental.modules.system.service.SystemEnumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "System")
@RestController
@RequestMapping("/api/system")
public class SystemEnumController {

    private final SystemEnumService systemEnumService;

    public SystemEnumController(SystemEnumService systemEnumService) {
        this.systemEnumService = systemEnumService;
    }

    @Operation(summary = "Frontend enum options")
    @GetMapping("/enums")
    public ApiResponse<Map<String, List<EnumOptionResponse>>> enums() {
        return ApiResponse.success(systemEnumService.frontendEnums());
    }
}

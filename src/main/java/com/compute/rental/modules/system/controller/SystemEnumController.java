package com.compute.rental.modules.system.controller;

import com.compute.rental.common.api.ApiResponse;
import com.compute.rental.common.i18n.LanguageResolver;
import com.compute.rental.modules.system.dto.EnumOptionResponse;
import com.compute.rental.modules.system.service.SystemEnumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "System")
@RestController
@RequestMapping("/api/system")
public class SystemEnumController {

    private final SystemEnumService systemEnumService;
    private final LanguageResolver languageResolver;

    public SystemEnumController(SystemEnumService systemEnumService, LanguageResolver languageResolver) {
        this.systemEnumService = systemEnumService;
        this.languageResolver = languageResolver;
    }

    @Operation(summary = "Frontend enum options")
    @GetMapping("/enums")
    public ApiResponse<Map<String, List<EnumOptionResponse>>> enums(
            @RequestParam(required = false) String language,
            @RequestHeader(name = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage,
            HttpServletResponse response
    ) {
        response.addHeader(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE);
        var locale = languageResolver.resolve(language, acceptLanguage);
        return ApiResponse.success(systemEnumService.frontendEnums(locale));
    }
}

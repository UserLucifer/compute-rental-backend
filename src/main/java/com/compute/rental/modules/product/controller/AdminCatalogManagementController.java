package com.compute.rental.modules.product.controller;

import com.compute.rental.common.api.ApiResponse;
import com.compute.rental.common.enums.CommonStatus;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.modules.product.dto.AdminAiModelRequest;
import com.compute.rental.modules.product.dto.AdminAiModelResponse;
import com.compute.rental.modules.product.dto.AdminGpuModelRequest;
import com.compute.rental.modules.product.dto.AdminGpuModelResponse;
import com.compute.rental.modules.product.dto.AdminProductRequest;
import com.compute.rental.modules.product.dto.AdminProductResponse;
import com.compute.rental.modules.product.dto.AdminRegionRequest;
import com.compute.rental.modules.product.dto.AdminRegionResponse;
import com.compute.rental.modules.product.dto.AdminRentalCycleRuleRequest;
import com.compute.rental.modules.product.dto.AdminRentalCycleRuleResponse;
import com.compute.rental.modules.product.service.AdminCatalogManagementService;
import com.compute.rental.modules.system.service.AdminLogService;
import com.compute.rental.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Catalog")
@RestController
@RequestMapping("/api/admin")
public class AdminCatalogManagementController {

    private final AdminCatalogManagementService adminCatalogManagementService;
    private final AdminLogService adminLogService;

    public AdminCatalogManagementController(
            AdminCatalogManagementService adminCatalogManagementService,
            AdminLogService adminLogService
    ) {
        this.adminCatalogManagementService = adminCatalogManagementService;
        this.adminLogService = adminLogService;
    }

    @Operation(summary = "Admin regions")
    @GetMapping("/regions")
    public ApiResponse<PageResult<AdminRegionResponse>> regions(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false, name = "region_code") String regionCode,
            @RequestParam(required = false) Integer status
    ) {
        return ApiResponse.success(adminCatalogManagementService.pageRegions(pageNo, pageSize, regionCode, status));
    }

    @Operation(summary = "Create region")
    @PostMapping("/regions")
    public ApiResponse<AdminRegionResponse> createRegion(
            @Valid @RequestBody AdminRegionRequest request,
            HttpServletRequest httpRequest
    ) {
        var admin = CurrentUser.requiredAdmin();
        return ApiResponse.success(adminCatalogManagementService.createRegion(request, admin.id(),
                adminLogService.clientIp(httpRequest)));
    }

    @Operation(summary = "Update region")
    @PutMapping("/regions/{id}")
    public ApiResponse<AdminRegionResponse> updateRegion(
            @PathVariable Long id,
            @Valid @RequestBody AdminRegionRequest request,
            HttpServletRequest httpRequest
    ) {
        var admin = CurrentUser.requiredAdmin();
        return ApiResponse.success(adminCatalogManagementService.updateRegion(id, request, admin.id(),
                adminLogService.clientIp(httpRequest)));
    }

    @Operation(summary = "Enable region")
    @PostMapping("/regions/{id}/enable")
    public ApiResponse<AdminRegionResponse> enableRegion(@PathVariable Long id, HttpServletRequest httpRequest) {
        var admin = CurrentUser.requiredAdmin();
        return ApiResponse.success(adminCatalogManagementService.setRegionStatus(id, CommonStatus.ENABLED.value(),
                admin.id(), adminLogService.clientIp(httpRequest)));
    }

    @Operation(summary = "Disable region")
    @PostMapping("/regions/{id}/disable")
    public ApiResponse<AdminRegionResponse> disableRegion(@PathVariable Long id, HttpServletRequest httpRequest) {
        var admin = CurrentUser.requiredAdmin();
        return ApiResponse.success(adminCatalogManagementService.setRegionStatus(id, CommonStatus.DISABLED.value(),
                admin.id(), adminLogService.clientIp(httpRequest)));
    }

    @Operation(summary = "Admin GPU models")
    @GetMapping("/gpu-models")
    public ApiResponse<PageResult<AdminGpuModelResponse>> gpuModels(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false, name = "model_code") String modelCode,
            @RequestParam(required = false) Integer status
    ) {
        return ApiResponse.success(adminCatalogManagementService.pageGpuModels(pageNo, pageSize, modelCode, status));
    }

    @Operation(summary = "Create GPU model")
    @PostMapping("/gpu-models")
    public ApiResponse<AdminGpuModelResponse> createGpuModel(
            @Valid @RequestBody AdminGpuModelRequest request,
            HttpServletRequest httpRequest
    ) {
        var admin = CurrentUser.requiredAdmin();
        return ApiResponse.success(adminCatalogManagementService.createGpuModel(request, admin.id(),
                adminLogService.clientIp(httpRequest)));
    }

    @Operation(summary = "Update GPU model")
    @PutMapping("/gpu-models/{id}")
    public ApiResponse<AdminGpuModelResponse> updateGpuModel(
            @PathVariable Long id,
            @Valid @RequestBody AdminGpuModelRequest request,
            HttpServletRequest httpRequest
    ) {
        var admin = CurrentUser.requiredAdmin();
        return ApiResponse.success(adminCatalogManagementService.updateGpuModel(id, request, admin.id(),
                adminLogService.clientIp(httpRequest)));
    }

    @Operation(summary = "Enable GPU model")
    @PostMapping("/gpu-models/{id}/enable")
    public ApiResponse<AdminGpuModelResponse> enableGpuModel(@PathVariable Long id, HttpServletRequest httpRequest) {
        var admin = CurrentUser.requiredAdmin();
        return ApiResponse.success(adminCatalogManagementService.setGpuModelStatus(id, CommonStatus.ENABLED.value(),
                admin.id(), adminLogService.clientIp(httpRequest)));
    }

    @Operation(summary = "Disable GPU model")
    @PostMapping("/gpu-models/{id}/disable")
    public ApiResponse<AdminGpuModelResponse> disableGpuModel(@PathVariable Long id, HttpServletRequest httpRequest) {
        var admin = CurrentUser.requiredAdmin();
        return ApiResponse.success(adminCatalogManagementService.setGpuModelStatus(id, CommonStatus.DISABLED.value(),
                admin.id(), adminLogService.clientIp(httpRequest)));
    }

    @Operation(summary = "Admin products")
    @GetMapping("/products")
    public ApiResponse<PageResult<AdminProductResponse>> products(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false, name = "product_code") String productCode,
            @RequestParam(required = false, name = "region_id") Long regionId,
            @RequestParam(required = false, name = "gpu_model_id") Long gpuModelId,
            @RequestParam(required = false) Integer status
    ) {
        return ApiResponse.success(adminCatalogManagementService.pageProducts(pageNo, pageSize, productCode,
                regionId, gpuModelId, status));
    }

    @Operation(summary = "Admin product detail")
    @GetMapping("/products/{productCode}")
    public ApiResponse<AdminProductResponse> product(@PathVariable String productCode) {
        return ApiResponse.success(adminCatalogManagementService.getProduct(productCode));
    }

    @Operation(summary = "Create product")
    @PostMapping("/products")
    public ApiResponse<AdminProductResponse> createProduct(
            @Valid @RequestBody AdminProductRequest request,
            HttpServletRequest httpRequest
    ) {
        var admin = CurrentUser.requiredAdmin();
        return ApiResponse.success(adminCatalogManagementService.createProduct(request, admin.id(),
                adminLogService.clientIp(httpRequest)));
    }

    @Operation(summary = "Update product")
    @PutMapping("/products/{productCode}")
    public ApiResponse<AdminProductResponse> updateProduct(
            @PathVariable String productCode,
            @Valid @RequestBody AdminProductRequest request,
            HttpServletRequest httpRequest
    ) {
        var admin = CurrentUser.requiredAdmin();
        return ApiResponse.success(adminCatalogManagementService.updateProduct(productCode, request, admin.id(),
                adminLogService.clientIp(httpRequest)));
    }

    @Operation(summary = "Enable product")
    @PostMapping("/products/{productCode}/enable")
    public ApiResponse<AdminProductResponse> enableProduct(@PathVariable String productCode, HttpServletRequest httpRequest) {
        var admin = CurrentUser.requiredAdmin();
        return ApiResponse.success(adminCatalogManagementService.setProductStatus(productCode,
                CommonStatus.ENABLED.value(), admin.id(), adminLogService.clientIp(httpRequest)));
    }

    @Operation(summary = "Disable product")
    @PostMapping("/products/{productCode}/disable")
    public ApiResponse<AdminProductResponse> disableProduct(@PathVariable String productCode, HttpServletRequest httpRequest) {
        var admin = CurrentUser.requiredAdmin();
        return ApiResponse.success(adminCatalogManagementService.setProductStatus(productCode,
                CommonStatus.DISABLED.value(), admin.id(), adminLogService.clientIp(httpRequest)));
    }

    @Operation(summary = "Admin AI models")
    @GetMapping("/ai-models")
    public ApiResponse<PageResult<AdminAiModelResponse>> aiModels(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false, name = "model_code") String modelCode,
            @RequestParam(required = false) Integer status
    ) {
        return ApiResponse.success(adminCatalogManagementService.pageAiModels(pageNo, pageSize, modelCode, status));
    }

    @Operation(summary = "Create AI model")
    @PostMapping("/ai-models")
    public ApiResponse<AdminAiModelResponse> createAiModel(
            @Valid @RequestBody AdminAiModelRequest request,
            HttpServletRequest httpRequest
    ) {
        var admin = CurrentUser.requiredAdmin();
        return ApiResponse.success(adminCatalogManagementService.createAiModel(request, admin.id(),
                adminLogService.clientIp(httpRequest)));
    }

    @Operation(summary = "Update AI model")
    @PutMapping("/ai-models/{modelCode}")
    public ApiResponse<AdminAiModelResponse> updateAiModel(
            @PathVariable String modelCode,
            @Valid @RequestBody AdminAiModelRequest request,
            HttpServletRequest httpRequest
    ) {
        var admin = CurrentUser.requiredAdmin();
        return ApiResponse.success(adminCatalogManagementService.updateAiModel(modelCode, request, admin.id(),
                adminLogService.clientIp(httpRequest)));
    }

    @Operation(summary = "Enable AI model")
    @PostMapping("/ai-models/{modelCode}/enable")
    public ApiResponse<AdminAiModelResponse> enableAiModel(@PathVariable String modelCode, HttpServletRequest httpRequest) {
        var admin = CurrentUser.requiredAdmin();
        return ApiResponse.success(adminCatalogManagementService.setAiModelStatus(modelCode,
                CommonStatus.ENABLED.value(), admin.id(), adminLogService.clientIp(httpRequest)));
    }

    @Operation(summary = "Disable AI model")
    @PostMapping("/ai-models/{modelCode}/disable")
    public ApiResponse<AdminAiModelResponse> disableAiModel(@PathVariable String modelCode, HttpServletRequest httpRequest) {
        var admin = CurrentUser.requiredAdmin();
        return ApiResponse.success(adminCatalogManagementService.setAiModelStatus(modelCode,
                CommonStatus.DISABLED.value(), admin.id(), adminLogService.clientIp(httpRequest)));
    }

    @Operation(summary = "Admin rental cycle rules")
    @GetMapping("/rental-cycle-rules")
    public ApiResponse<PageResult<AdminRentalCycleRuleResponse>> cycleRules(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false, name = "cycle_code") String cycleCode,
            @RequestParam(required = false) Integer status
    ) {
        return ApiResponse.success(adminCatalogManagementService.pageCycleRules(pageNo, pageSize, cycleCode, status));
    }

    @Operation(summary = "Create rental cycle rule")
    @PostMapping("/rental-cycle-rules")
    public ApiResponse<AdminRentalCycleRuleResponse> createCycleRule(
            @Valid @RequestBody AdminRentalCycleRuleRequest request,
            HttpServletRequest httpRequest
    ) {
        var admin = CurrentUser.requiredAdmin();
        return ApiResponse.success(adminCatalogManagementService.createCycleRule(request, admin.id(),
                adminLogService.clientIp(httpRequest)));
    }

    @Operation(summary = "Update rental cycle rule")
    @PutMapping("/rental-cycle-rules/{cycleCode}")
    public ApiResponse<AdminRentalCycleRuleResponse> updateCycleRule(
            @PathVariable String cycleCode,
            @Valid @RequestBody AdminRentalCycleRuleRequest request,
            HttpServletRequest httpRequest
    ) {
        var admin = CurrentUser.requiredAdmin();
        return ApiResponse.success(adminCatalogManagementService.updateCycleRule(cycleCode, request, admin.id(),
                adminLogService.clientIp(httpRequest)));
    }

    @Operation(summary = "Enable rental cycle rule")
    @PostMapping("/rental-cycle-rules/{cycleCode}/enable")
    public ApiResponse<AdminRentalCycleRuleResponse> enableCycleRule(
            @PathVariable String cycleCode,
            HttpServletRequest httpRequest
    ) {
        var admin = CurrentUser.requiredAdmin();
        return ApiResponse.success(adminCatalogManagementService.setCycleRuleStatus(cycleCode,
                CommonStatus.ENABLED.value(), admin.id(), adminLogService.clientIp(httpRequest)));
    }

    @Operation(summary = "Disable rental cycle rule")
    @PostMapping("/rental-cycle-rules/{cycleCode}/disable")
    public ApiResponse<AdminRentalCycleRuleResponse> disableCycleRule(
            @PathVariable String cycleCode,
            HttpServletRequest httpRequest
    ) {
        var admin = CurrentUser.requiredAdmin();
        return ApiResponse.success(adminCatalogManagementService.setCycleRuleStatus(cycleCode,
                CommonStatus.DISABLED.value(), admin.id(), adminLogService.clientIp(httpRequest)));
    }
}

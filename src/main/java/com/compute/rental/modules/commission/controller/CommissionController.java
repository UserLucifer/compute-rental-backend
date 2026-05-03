package com.compute.rental.modules.commission.controller;

import com.compute.rental.common.api.ApiResponse;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.modules.commission.dto.CommissionRecordQueryRequest;
import com.compute.rental.modules.commission.dto.CommissionRecordResponse;
import com.compute.rental.modules.commission.dto.CommissionSummaryResponse;
import com.compute.rental.modules.commission.service.CommissionService;
import com.compute.rental.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "Commission")
@RestController
@RequestMapping("/api/commission")
public class CommissionController {

    private final CommissionService commissionService;

    public CommissionController(CommissionService commissionService) {
        this.commissionService = commissionService;
    }

    @Operation(summary = "Current user commission records")
    @GetMapping("/records")
    public ApiResponse<PageResult<CommissionRecordResponse>> records(
            @Valid @ModelAttribute CommissionRecordQueryRequest request
    ) {
        var currentUser = CurrentUser.required();
        return ApiResponse.success(commissionService.pageUserRecords(currentUser.id(), request));
    }

    @Operation(summary = "Current user commission summary")
    @GetMapping("/summary")
    public ApiResponse<CommissionSummaryResponse> summary() {
        var currentUser = CurrentUser.required();
        return ApiResponse.success(commissionService.summary(currentUser.id()));
    }
}

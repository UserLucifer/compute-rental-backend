package com.compute.rental.modules.order.controller;

import com.compute.rental.common.api.ApiResponse;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.modules.order.dto.ProfitRecordQueryRequest;
import com.compute.rental.modules.order.dto.ProfitRecordResponse;
import com.compute.rental.modules.order.dto.ProfitSummaryResponse;
import com.compute.rental.modules.order.service.ProfitService;
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
@Tag(name = "Profit")
@RestController
@RequestMapping("/api/profit")
public class ProfitController {

    private final ProfitService profitService;

    public ProfitController(ProfitService profitService) {
        this.profitService = profitService;
    }

    @Operation(summary = "Current user profit records")
    @GetMapping("/records")
    public ApiResponse<PageResult<ProfitRecordResponse>> records(
            @Valid @ModelAttribute ProfitRecordQueryRequest request
    ) {
        var currentUser = CurrentUser.required();
        return ApiResponse.success(profitService.pageUserProfitRecords(currentUser.id(), request));
    }

    @Operation(summary = "Current user profit summary")
    @GetMapping("/summary")
    public ApiResponse<ProfitSummaryResponse> summary() {
        var currentUser = CurrentUser.required();
        return ApiResponse.success(profitService.summary(currentUser.id()));
    }
}

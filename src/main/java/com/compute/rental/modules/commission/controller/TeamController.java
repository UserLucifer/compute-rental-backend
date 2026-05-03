package com.compute.rental.modules.commission.controller;

import com.compute.rental.common.api.ApiResponse;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.modules.commission.dto.TeamMemberQueryRequest;
import com.compute.rental.modules.commission.dto.TeamMemberResponse;
import com.compute.rental.modules.commission.dto.TeamSummaryResponse;
import com.compute.rental.modules.commission.service.TeamService;
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
@Tag(name = "Team")
@RestController
@RequestMapping("/api/team")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @Operation(summary = "Current user team summary")
    @GetMapping("/summary")
    public ApiResponse<TeamSummaryResponse> summary() {
        var currentUser = CurrentUser.required();
        return ApiResponse.success(teamService.summary(currentUser.id()));
    }

    @Operation(summary = "Current user team members")
    @GetMapping("/members")
    public ApiResponse<PageResult<TeamMemberResponse>> members(@Valid @ModelAttribute TeamMemberQueryRequest request) {
        var currentUser = CurrentUser.required();
        return ApiResponse.success(teamService.pageMembers(currentUser.id(), request));
    }
}

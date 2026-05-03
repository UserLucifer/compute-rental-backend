package com.compute.rental.modules.user.controller;

import com.compute.rental.common.api.ApiResponse;
import com.compute.rental.modules.user.dto.UpdateAvatarRequest;
import com.compute.rental.modules.user.dto.UserMeResponse;
import com.compute.rental.modules.user.service.FrontUserService;
import com.compute.rental.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User")
@RestController
@RequestMapping("/api/user")
public class FrontUserController {

    private final FrontUserService frontUserService;

    public FrontUserController(FrontUserService frontUserService) {
        this.frontUserService = frontUserService;
    }

    @Operation(summary = "Current frontend user")
    @GetMapping("/me")
    public ApiResponse<UserMeResponse> me() {
        var currentUser = CurrentUser.required();
        return ApiResponse.success(frontUserService.getMe(currentUser.id()));
    }

    @Operation(summary = "Update current frontend user avatar")
    @PutMapping("/avatar")
    public ApiResponse<UserMeResponse> updateAvatar(@Valid @RequestBody UpdateAvatarRequest request) {
        var currentUser = CurrentUser.required();
        return ApiResponse.success(frontUserService.updateAvatar(currentUser.id(), request));
    }
}

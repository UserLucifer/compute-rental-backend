package com.compute.rental.modules.user.controller;

import com.compute.rental.common.api.ApiResponse;
import com.compute.rental.modules.user.dto.PushDeviceResponse;
import com.compute.rental.modules.user.dto.RegisterPushDeviceRequest;
import com.compute.rental.modules.user.dto.UnregisterPushDeviceRequest;
import com.compute.rental.modules.user.service.PushDeviceService;
import com.compute.rental.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Push Devices")
@RestController
@RequestMapping("/api/push-devices")
public class PushDeviceController {

    private final PushDeviceService pushDeviceService;

    public PushDeviceController(PushDeviceService pushDeviceService) {
        this.pushDeviceService = pushDeviceService;
    }

    @Operation(summary = "Register push device")
    @PostMapping("/register")
    public ApiResponse<PushDeviceResponse> register(@Valid @RequestBody RegisterPushDeviceRequest request) {
        var user = CurrentUser.required();
        return ApiResponse.success(pushDeviceService.register(user.id(), request));
    }

    @Operation(summary = "Unregister push device")
    @PostMapping("/unregister")
    public ApiResponse<Void> unregister(@Valid @RequestBody UnregisterPushDeviceRequest request) {
        var user = CurrentUser.required();
        pushDeviceService.unregister(user.id(), request.deviceToken());
        return ApiResponse.success(null);
    }

    @Operation(summary = "Current user push devices")
    @GetMapping
    public ApiResponse<List<PushDeviceResponse>> devices() {
        var user = CurrentUser.required();
        return ApiResponse.success(pushDeviceService.list(user.id()));
    }
}

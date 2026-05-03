package com.compute.rental.modules.auth.controller;

import com.compute.rental.common.api.ApiResponse;
import com.compute.rental.modules.auth.dto.LoginRequest;
import com.compute.rental.modules.auth.dto.LoginResponse;
import com.compute.rental.modules.auth.dto.ResetPasswordRequest;
import com.compute.rental.modules.auth.dto.SendEmailCodeRequest;
import com.compute.rental.modules.auth.dto.SignupRequest;
import com.compute.rental.modules.auth.dto.VerifyEmailCodeRequest;
import com.compute.rental.modules.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Send signup email code")
    @PostMapping("/signup/email-code/send")
    public ApiResponse<Void> sendSignupEmailCode(
            @Valid @RequestBody SendEmailCodeRequest request,
            HttpServletRequest servletRequest
    ) {
        authService.sendSignupEmailCode(request, servletRequest.getRemoteAddr());
        return ApiResponse.success();
    }

    @Operation(summary = "Verify signup email code")
    @PostMapping("/signup/email-code/verify")
    public ApiResponse<Void> verifySignupEmailCode(@Valid @RequestBody VerifyEmailCodeRequest request) {
        authService.verifySignupEmailCode(request);
        return ApiResponse.success();
    }

    @Operation(summary = "Signup with email code, user name and password")
    @PostMapping("/signup")
    public ApiResponse<LoginResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ApiResponse.success(authService.signup(request));
    }

    @Operation(summary = "Register with email code, user name and password")
    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody SignupRequest request) {
        return ApiResponse.success(authService.signup(request));
    }

    @Operation(summary = "Login with email and password")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @Operation(summary = "Login with email and password")
    @PostMapping("/login/password")
    public ApiResponse<LoginResponse> passwordLogin(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @Operation(summary = "Send reset password email code")
    @PostMapping("/reset-password/email-code/send")
    public ApiResponse<Void> sendResetPasswordEmailCode(
            @Valid @RequestBody SendEmailCodeRequest request,
            HttpServletRequest servletRequest
    ) {
        authService.sendResetPasswordEmailCode(request, servletRequest.getRemoteAddr());
        return ApiResponse.success();
    }

    @Operation(summary = "Verify reset password email code")
    @PostMapping("/reset-password/email-code/verify")
    public ApiResponse<Void> verifyResetPasswordEmailCode(@Valid @RequestBody VerifyEmailCodeRequest request) {
        authService.verifyResetPasswordEmailCode(request);
        return ApiResponse.success();
    }

    @Operation(summary = "Reset password with email code")
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.success();
    }

    @Operation(summary = "Reset password with email code")
    @PostMapping("/password/reset")
    public ApiResponse<Void> passwordReset(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.success();
    }
}

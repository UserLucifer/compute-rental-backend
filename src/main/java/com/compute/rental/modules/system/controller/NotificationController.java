package com.compute.rental.modules.system.controller;

import com.compute.rental.common.api.ApiResponse;
import com.compute.rental.common.i18n.LanguageResolver;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.modules.system.dto.NotificationResponse;
import com.compute.rental.modules.system.service.NotificationService;
import com.compute.rental.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notifications")
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final LanguageResolver languageResolver;

    public NotificationController(NotificationService notificationService, LanguageResolver languageResolver) {
        this.notificationService = notificationService;
        this.languageResolver = languageResolver;
    }

    @Operation(summary = "Current user notifications")
    @GetMapping
    public ApiResponse<PageResult<NotificationResponse>> notifications(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false, name = "read_status") Integer readStatus,
            @RequestParam(required = false, name = "notification_type") String notificationType,
            @RequestParam(required = false, name = "start_time")
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false, name = "end_time")
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(required = false) String language,
            @RequestHeader(name = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage,
            HttpServletResponse response
    ) {
        addLanguageVary(response);
        var locale = languageResolver.resolve(language, acceptLanguage);
        var user = CurrentUser.required();
        return ApiResponse.success(notificationService.pageUserNotifications(user.id(), pageNo, pageSize,
                readStatus, notificationType, startTime, endTime, locale));
    }

    @Operation(summary = "Current user notification detail")
    @GetMapping("/{id}")
    public ApiResponse<NotificationResponse> notification(
            @PathVariable Long id,
            @RequestParam(required = false) String language,
            @RequestHeader(name = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage,
            HttpServletResponse response
    ) {
        addLanguageVary(response);
        var locale = languageResolver.resolve(language, acceptLanguage);
        var user = CurrentUser.required();
        return ApiResponse.success(notificationService.getUserNotification(user.id(), id, locale));
    }

    @Operation(summary = "Mark notification read")
    @PostMapping("/{id}/read")
    public ApiResponse<NotificationResponse> read(
            @PathVariable Long id,
            @RequestParam(required = false) String language,
            @RequestHeader(name = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage,
            HttpServletResponse response
    ) {
        addLanguageVary(response);
        var locale = languageResolver.resolve(language, acceptLanguage);
        var user = CurrentUser.required();
        return ApiResponse.success(notificationService.markUserNotificationRead(user.id(), id, locale));
    }

    @Operation(summary = "Mark all notifications read")
    @PostMapping("/read-all")
    public ApiResponse<Long> readAll() {
        var user = CurrentUser.required();
        return ApiResponse.success(notificationService.markAllUserNotificationsRead(user.id()));
    }

    private void addLanguageVary(HttpServletResponse response) {
        response.addHeader(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE);
    }
}

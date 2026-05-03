package com.compute.rental.modules.system.controller;

import com.compute.rental.common.api.ApiResponse;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.modules.system.dto.AdminNotificationResponse;
import com.compute.rental.modules.system.dto.NotificationBroadcastRequest;
import com.compute.rental.modules.system.dto.NotificationCreateRequest;
import com.compute.rental.modules.system.service.AdminLogService;
import com.compute.rental.modules.system.service.NotificationService;
import com.compute.rental.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Notifications")
@RestController
@RequestMapping("/api/admin/notifications")
public class AdminNotificationController {

    private final NotificationService notificationService;
    private final AdminLogService adminLogService;

    public AdminNotificationController(NotificationService notificationService, AdminLogService adminLogService) {
        this.notificationService = notificationService;
        this.adminLogService = adminLogService;
    }

    @Operation(summary = "Admin notifications")
    @GetMapping
    public ApiResponse<PageResult<AdminNotificationResponse>> notifications(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false, name = "user_id") Long userId,
            @RequestParam(required = false, name = "read_status") Integer readStatus,
            @RequestParam(required = false, name = "notification_type") String notificationType,
            @RequestParam(required = false, name = "biz_type") String bizType,
            @RequestParam(required = false, name = "start_time")
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false, name = "end_time")
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime
    ) {
        return ApiResponse.success(notificationService.pageAdminNotifications(pageNo, pageSize, userId, readStatus,
                notificationType, bizType, startTime, endTime));
    }

    @Operation(summary = "Admin notification detail")
    @GetMapping("/{id}")
    public ApiResponse<AdminNotificationResponse> notification(@PathVariable Long id) {
        return ApiResponse.success(notificationService.getAdminNotification(id));
    }

    @Operation(summary = "Create notification")
    @PostMapping
    public ApiResponse<AdminNotificationResponse> create(
            @Valid @RequestBody NotificationCreateRequest request,
            HttpServletRequest httpRequest
    ) {
        var admin = CurrentUser.requiredAdmin();
        var notification = notificationService.createForUser(request);
        adminLogService.log(admin.id(), "CREATE_NOTIFICATION", "sys_notification", notification.id(),
                null, null, "userId=" + request.userId(), adminLogService.clientIp(httpRequest));
        return ApiResponse.success(notification);
    }

    @Operation(summary = "Broadcast notification")
    @PostMapping("/broadcast")
    public ApiResponse<Integer> broadcast(
            @Valid @RequestBody NotificationBroadcastRequest request,
            HttpServletRequest httpRequest
    ) {
        var admin = CurrentUser.requiredAdmin();
        var count = notificationService.broadcast(request);
        adminLogService.log(admin.id(), "BROADCAST_NOTIFICATION", "sys_notification", null,
                null, null, "count=" + count, adminLogService.clientIp(httpRequest));
        return ApiResponse.success(count);
    }

    @Operation(summary = "Cancel notification")
    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long id, HttpServletRequest httpRequest) {
        var admin = CurrentUser.requiredAdmin();
        notificationService.cancel(id);
        adminLogService.log(admin.id(), "CANCEL_NOTIFICATION", "sys_notification", id,
                null, null, "Deleted notification because table has no status field",
                adminLogService.clientIp(httpRequest));
        return ApiResponse.success(null);
    }
}

package com.compute.rental.modules.system.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.compute.rental.modules.system.dto.AdminNotificationResponse;
import com.compute.rental.modules.system.dto.NotificationCreateRequest;
import com.compute.rental.modules.system.service.AdminLogService;
import com.compute.rental.modules.system.service.NotificationService;
import com.compute.rental.security.IdentityType;
import com.compute.rental.security.JwtPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AdminNotificationControllerTest {

    @Mock
    private NotificationService notificationService;
    @Mock
    private AdminLogService adminLogService;
    @Mock
    private HttpServletRequest httpServletRequest;

    private AdminNotificationController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminNotificationController(notificationService, adminLogService);
        var principal = new JwtPrincipal(1L, "admin", "SUPER_ADMIN", IdentityType.ADMIN.name());
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(principal, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createNotificationWritesAdminLog() {
        var request = new NotificationCreateRequest(10L, "title", "content", "SYSTEM", null, null);
        var notification = new AdminNotificationResponse(
                100L,
                10L,
                null,
                "title",
                "content",
                "SYSTEM",
                null,
                null,
                0,
                null,
                null
        );
        when(notificationService.createForUser(request)).thenReturn(notification);
        when(adminLogService.clientIp(httpServletRequest)).thenReturn("127.0.0.1");

        controller.create(request, httpServletRequest);

        verify(adminLogService).log(eq(1L), eq("CREATE_NOTIFICATION"), eq("sys_notification"), eq(100L),
                isNull(), isNull(), eq("userId=10"), eq("127.0.0.1"));
    }
}

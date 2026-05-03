package com.compute.rental.modules.system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.compute.rental.common.enums.ReadStatus;
import com.compute.rental.modules.system.dto.NotificationCreateRequest;
import com.compute.rental.modules.system.entity.SysNotification;
import com.compute.rental.modules.system.mapper.SysNotificationMapper;
import com.compute.rental.modules.user.entity.AppUser;
import com.compute.rental.modules.user.mapper.AppUserMapper;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private SysNotificationMapper notificationMapper;
    @Mock
    private AppUserMapper appUserMapper;

    private NotificationService notificationService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        var configuration = new Configuration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), SysNotification.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), AppUser.class);
    }

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationMapper, appUserMapper);
    }

    @Test
    void userNotificationDetailMarksOnlyOwnNotificationRead() {
        var notification = notification(1L, 10L, ReadStatus.UNREAD.value());
        var readNotification = notification(1L, 10L, ReadStatus.READ.value());
        when(notificationMapper.selectOne(any())).thenReturn(notification);
        when(notificationMapper.update(isNull(), any())).thenReturn(1);
        when(notificationMapper.selectById(1L)).thenReturn(readNotification);

        var result = notificationService.getUserNotification(10L, 1L);

        assertThat(result.readStatus()).isEqualTo(ReadStatus.READ.value());
        assertThat(result.userId()).isEqualTo(10L);
        verify(notificationMapper).selectOne(any());
        verify(notificationMapper).update(isNull(), any());
    }

    @Test
    void markAllUserNotificationsReadUpdatesCurrentUserOnly() {
        when(notificationMapper.update(isNull(), any())).thenReturn(3);

        var count = notificationService.markAllUserNotificationsRead(10L);

        assertThat(count).isEqualTo(3);
        verify(notificationMapper).update(isNull(), any());
    }

    @Test
    void adminNotificationPageShouldReturnResponseDtos() {
        var notification = notification(1L, 10L, ReadStatus.UNREAD.value());
        notification.setUserName("user");
        notification.setBizType("ORDER");
        notification.setBizId(99L);
        var page = new Page<SysNotification>(1, 10);
        page.setRecords(List.of(notification));
        page.setTotal(1);
        when(notificationMapper.selectPage(any(), any())).thenReturn(page);

        var result = notificationService.pageAdminNotifications(1, 10, null, null,
                null, null, null, null);

        assertThat(result.records()).hasSize(1);
        var response = result.records().get(0);
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.userId()).isEqualTo(10L);
        assertThat(response.userName()).isEqualTo("user");
        assertThat(response.bizType()).isEqualTo("ORDER");
        assertThat(response.bizId()).isEqualTo(99L);
    }

    @Test
    void createForUserShouldReturnAdminResponseDto() {
        var user = new AppUser();
        user.setId(10L);
        when(appUserMapper.selectById(10L)).thenReturn(user);
        doAnswer(invocation -> {
            var notification = invocation.getArgument(0, SysNotification.class);
            notification.setId(100L);
            return 1;
        }).when(notificationMapper).insert(any(SysNotification.class));

        var result = notificationService.createForUser(
                new NotificationCreateRequest(10L, "title", "content", "SYSTEM", "ORDER", 99L));

        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.userId()).isEqualTo(10L);
        assertThat(result.title()).isEqualTo("title");
        assertThat(result.readStatus()).isEqualTo(ReadStatus.UNREAD.value());
    }

    private SysNotification notification(Long id, Long userId, Integer readStatus) {
        var notification = new SysNotification();
        notification.setId(id);
        notification.setUserId(userId);
        notification.setTitle("title");
        notification.setContent("content");
        notification.setType("SYSTEM");
        notification.setReadStatus(readStatus);
        return notification;
    }
}

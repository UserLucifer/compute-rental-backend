package com.compute.rental.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.compute.rental.common.enums.CommonStatus;
import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.enums.ReadStatus;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.common.util.DateTimeUtils;
import com.compute.rental.modules.system.dto.AdminNotificationResponse;
import com.compute.rental.modules.system.dto.NotificationBroadcastRequest;
import com.compute.rental.modules.system.dto.NotificationCreateRequest;
import com.compute.rental.modules.system.dto.NotificationResponse;
import com.compute.rental.modules.system.entity.SysNotification;
import com.compute.rental.modules.system.mapper.SysNotificationMapper;
import com.compute.rental.modules.user.entity.AppUser;
import com.compute.rental.modules.user.mapper.AppUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class NotificationService {

    private final SysNotificationMapper notificationMapper;
    private final AppUserMapper appUserMapper;

    public NotificationService(SysNotificationMapper notificationMapper, AppUserMapper appUserMapper) {
        this.notificationMapper = notificationMapper;
        this.appUserMapper = appUserMapper;
    }

    public PageResult<NotificationResponse> pageUserNotifications(
            Long userId,
            long pageNo,
            long pageSize,
            Integer readStatus,
            String notificationType,
            java.time.LocalDateTime startTime,
            java.time.LocalDateTime endTime
    ) {
        var page = new Page<SysNotification>(pageNo, pageSize);
        var wrapper = new LambdaQueryWrapper<SysNotification>()
                .eq(SysNotification::getUserId, userId)
                .eq(readStatus != null, SysNotification::getReadStatus, readStatus)
                .eq(StringUtils.hasText(notificationType), SysNotification::getType, notificationType)
                .ge(startTime != null, SysNotification::getCreatedAt, startTime)
                .le(endTime != null, SysNotification::getCreatedAt, endTime)
                .orderByDesc(SysNotification::getId);
        var result = notificationMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getRecords().stream().map(this::notificationResponse).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public NotificationResponse getUserNotification(Long userId, Long id) {
        var notification = requireUserNotification(userId, id);
        markReadIfUnread(notification);
        return notificationResponse(notificationMapper.selectById(id));
    }

    @Transactional
    public NotificationResponse markUserNotificationRead(Long userId, Long id) {
        var notification = requireUserNotification(userId, id);
        markReadIfUnread(notification);
        return notificationResponse(notificationMapper.selectById(id));
    }

    @Transactional
    public long markAllUserNotificationsRead(Long userId) {
        return notificationMapper.update(null, new LambdaUpdateWrapper<SysNotification>()
                .eq(SysNotification::getUserId, userId)
                .eq(SysNotification::getReadStatus, ReadStatus.UNREAD.value())
                .set(SysNotification::getReadStatus, ReadStatus.READ.value())
                .set(SysNotification::getReadAt, DateTimeUtils.now()));
    }

    public PageResult<AdminNotificationResponse> pageAdminNotifications(
            long pageNo,
            long pageSize,
            Long userId,
            Integer readStatus,
            String notificationType,
            String bizType,
            java.time.LocalDateTime startTime,
            java.time.LocalDateTime endTime
    ) {
        var page = new Page<SysNotification>(pageNo, pageSize);
        var wrapper = new LambdaQueryWrapper<SysNotification>()
                .eq(userId != null, SysNotification::getUserId, userId)
                .eq(readStatus != null, SysNotification::getReadStatus, readStatus)
                .eq(StringUtils.hasText(notificationType), SysNotification::getType, notificationType)
                .eq(StringUtils.hasText(bizType), SysNotification::getBizType, bizType)
                .ge(startTime != null, SysNotification::getCreatedAt, startTime)
                .le(endTime != null, SysNotification::getCreatedAt, endTime)
                .orderByDesc(SysNotification::getId);
        var result = notificationMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getRecords().stream().map(this::adminNotificationResponse).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    public AdminNotificationResponse getAdminNotification(Long id) {
        return adminNotificationResponse(requireNotification(id));
    }

    @Transactional
    public AdminNotificationResponse createForUser(NotificationCreateRequest request) {
        requireUser(request.userId());
        var notification = buildNotification(request.userId(), request.title(), request.content(),
                request.type(), request.bizType(), request.bizId());
        notificationMapper.insert(notification);
        return adminNotificationResponse(notification);
    }

    @Transactional
    public int broadcast(NotificationBroadcastRequest request) {
        var users = appUserMapper.selectList(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getStatus, CommonStatus.ENABLED.value()));
        for (var user : users) {
            notificationMapper.insert(buildNotification(user.getId(), request.title(), request.content(),
                    request.type(), request.bizType(), request.bizId()));
        }
        return users.size();
    }

    @Transactional
    public void cancel(Long id) {
        requireNotification(id);
        notificationMapper.deleteById(id);
    }

    private void markReadIfUnread(SysNotification notification) {
        if (Integer.valueOf(ReadStatus.READ.value()).equals(notification.getReadStatus())) {
            return;
        }
        notificationMapper.update(null, new LambdaUpdateWrapper<SysNotification>()
                .eq(SysNotification::getId, notification.getId())
                .eq(SysNotification::getReadStatus, ReadStatus.UNREAD.value())
                .set(SysNotification::getReadStatus, ReadStatus.READ.value())
                .set(SysNotification::getReadAt, DateTimeUtils.now()));
    }

    private SysNotification buildNotification(Long userId, String title, String content, String type,
                                              String bizType, Long bizId) {
        var now = DateTimeUtils.now();
        var notification = new SysNotification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setBizType(bizType);
        notification.setBizId(bizId);
        notification.setReadStatus(ReadStatus.UNREAD.value());
        notification.setCreatedAt(now);
        return notification;
    }

    private NotificationResponse notificationResponse(SysNotification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getUserId(),
                notification.getUserName(),
                notification.getTitle(),
                notification.getContent(),
                notification.getType(),
                notification.getBizType(),
                notification.getBizId(),
                notification.getReadStatus(),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }

    private AdminNotificationResponse adminNotificationResponse(SysNotification notification) {
        return new AdminNotificationResponse(
                notification.getId(),
                notification.getUserId(),
                notification.getUserName(),
                notification.getTitle(),
                notification.getContent(),
                notification.getType(),
                notification.getBizType(),
                notification.getBizId(),
                notification.getReadStatus(),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }

    private SysNotification requireUserNotification(Long userId, Long id) {
        var notification = notificationMapper.selectOne(new LambdaQueryWrapper<SysNotification>()
                .eq(SysNotification::getId, id)
                .eq(SysNotification::getUserId, userId)
                .last("LIMIT 1"));
        if (notification == null) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        return notification;
    }

    private SysNotification requireNotification(Long id) {
        var notification = notificationMapper.selectById(id);
        if (notification == null) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        return notification;
    }

    private void requireUser(Long userId) {
        if (appUserMapper.selectById(userId) == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }
}

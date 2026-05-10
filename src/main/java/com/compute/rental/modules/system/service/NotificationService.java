package com.compute.rental.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.compute.rental.common.enums.CommonStatus;
import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.enums.ReadStatus;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.common.i18n.LanguageResolver;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.common.util.DateTimeUtils;
import com.compute.rental.modules.system.dto.AdminNotificationResponse;
import com.compute.rental.modules.system.dto.NotificationBroadcastRequest;
import com.compute.rental.modules.system.dto.NotificationCreateRequest;
import com.compute.rental.modules.system.dto.NotificationResponse;
import com.compute.rental.modules.system.dto.NotificationTranslationRequest;
import com.compute.rental.modules.system.dto.NotificationTranslationResponse;
import com.compute.rental.modules.system.entity.SysNotification;
import com.compute.rental.modules.system.entity.SysNotificationTranslation;
import com.compute.rental.modules.system.mapper.SysNotificationMapper;
import com.compute.rental.modules.system.mapper.SysNotificationTranslationMapper;
import com.compute.rental.modules.user.entity.AppUser;
import com.compute.rental.modules.user.mapper.AppUserMapper;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class NotificationService {

    private final SysNotificationMapper notificationMapper;
    private final SysNotificationTranslationMapper notificationTranslationMapper;
    private final AppUserMapper appUserMapper;

    public NotificationService(
            SysNotificationMapper notificationMapper,
            SysNotificationTranslationMapper notificationTranslationMapper,
            AppUserMapper appUserMapper
    ) {
        this.notificationMapper = notificationMapper;
        this.notificationTranslationMapper = notificationTranslationMapper;
        this.appUserMapper = appUserMapper;
    }

    public PageResult<NotificationResponse> pageUserNotifications(
            Long userId,
            long pageNo,
            long pageSize,
            Integer readStatus,
            String notificationType,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        return pageUserNotifications(userId, pageNo, pageSize, readStatus, notificationType, startTime, endTime,
                LanguageResolver.DEFAULT_LANGUAGE);
    }

    public PageResult<NotificationResponse> pageUserNotifications(
            Long userId,
            long pageNo,
            long pageSize,
            Integer readStatus,
            String notificationType,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String locale
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
        var notifications = result.getRecords();
        var translations = notificationTranslationMap(
                notifications.stream().map(SysNotification::getId).toList(), locale);
        return new PageResult<>(notifications.stream()
                .map(notification -> notificationResponse(notification, translations.get(notification.getId()), locale))
                .toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public NotificationResponse getUserNotification(Long userId, Long id) {
        return getUserNotification(userId, id, LanguageResolver.DEFAULT_LANGUAGE);
    }

    @Transactional
    public NotificationResponse getUserNotification(Long userId, Long id, String locale) {
        var notification = requireUserNotification(userId, id);
        markReadIfUnread(notification);
        var latest = notificationMapper.selectById(id);
        return notificationResponse(latest, notificationTranslation(id, locale), locale);
    }

    @Transactional
    public NotificationResponse markUserNotificationRead(Long userId, Long id) {
        return markUserNotificationRead(userId, id, LanguageResolver.DEFAULT_LANGUAGE);
    }

    @Transactional
    public NotificationResponse markUserNotificationRead(Long userId, Long id, String locale) {
        var notification = requireUserNotification(userId, id);
        markReadIfUnread(notification);
        var latest = notificationMapper.selectById(id);
        return notificationResponse(latest, notificationTranslation(id, locale), locale);
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
            LocalDateTime startTime,
            LocalDateTime endTime
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

    public List<NotificationTranslationResponse> listNotificationTranslations(Long id) {
        var notification = requireNotification(id);
        var translations = notificationTranslationMap(List.of(id), LanguageResolver.EN_US);
        return List.of(
                new NotificationTranslationResponse(
                        id,
                        LanguageResolver.DEFAULT_LANGUAGE,
                        notification.getTitle(),
                        notification.getContent(),
                        true,
                        notification.getCreatedAt(),
                        notification.getCreatedAt()
                ),
                notificationTranslationResponse(id, LanguageResolver.EN_US, translations.get(id))
        );
    }

    @Transactional
    public NotificationTranslationResponse updateNotificationTranslation(
            Long id,
            NotificationTranslationRequest request
    ) {
        var notification = requireNotification(id);
        var locale = requireSupportedLocale(request.locale());
        var title = trimToNull(request.title());
        var content = trimToNull(request.content());
        if (LanguageResolver.DEFAULT_LANGUAGE.equals(locale)) {
            notificationMapper.update(null, new LambdaUpdateWrapper<SysNotification>()
                    .eq(SysNotification::getId, id)
                    .set(SysNotification::getTitle, title == null ? notification.getTitle() : title)
                    .set(SysNotification::getContent, content == null ? notification.getContent() : content));
            notification.setTitle(title == null ? notification.getTitle() : title);
            notification.setContent(content == null ? notification.getContent() : content);
        }
        return upsertNotificationTranslation(id, locale, notification.getTitle(), notification.getContent(),
                title, content);
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
        notificationTranslationMapper.delete(new LambdaQueryWrapper<SysNotificationTranslation>()
                .eq(SysNotificationTranslation::getNotificationId, id));
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
        return notificationResponse(notification, null, LanguageResolver.DEFAULT_LANGUAGE);
    }

    private NotificationResponse notificationResponse(SysNotification notification,
                                                      SysNotificationTranslation translation,
                                                      String requestedLocale) {
        var title = localized(notification.getTitle(), requestedLocale, translation == null ? null : translation.getTitle());
        var content = localized(notification.getContent(), requestedLocale, translation == null ? null : translation.getContent());
        var localeFallback = title.fallback() || content.fallback();
        return new NotificationResponse(
                notification.getId(),
                notification.getUserId(),
                notification.getUserName(),
                title.value(),
                content.value(),
                notification.getType(),
                notification.getBizType(),
                notification.getBizId(),
                notification.getReadStatus(),
                notification.getReadAt(),
                notification.getCreatedAt(),
                localeFallback ? LanguageResolver.DEFAULT_LANGUAGE : requestedLocale,
                requestedLocale,
                localeFallback
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

    private Map<Long, SysNotificationTranslation> notificationTranslationMap(Collection<Long> ids, String locale) {
        if (LanguageResolver.DEFAULT_LANGUAGE.equals(locale) || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return notificationTranslationMapper.selectList(new LambdaQueryWrapper<SysNotificationTranslation>()
                        .in(SysNotificationTranslation::getNotificationId, ids)
                        .eq(SysNotificationTranslation::getLocale, locale))
                .stream()
                .collect(Collectors.toMap(SysNotificationTranslation::getNotificationId, Function.identity()));
    }

    private SysNotificationTranslation notificationTranslation(Long notificationId, String locale) {
        if (LanguageResolver.DEFAULT_LANGUAGE.equals(locale) || notificationId == null) {
            return null;
        }
        return notificationTranslationMapper.selectOne(new LambdaQueryWrapper<SysNotificationTranslation>()
                .eq(SysNotificationTranslation::getNotificationId, notificationId)
                .eq(SysNotificationTranslation::getLocale, locale)
                .last("LIMIT 1"));
    }

    private NotificationTranslationResponse upsertNotificationTranslation(
            Long notificationId,
            String locale,
            String defaultTitle,
            String defaultContent,
            String title,
            String content
    ) {
        var existing = notificationTranslationMapper.selectOne(new LambdaQueryWrapper<SysNotificationTranslation>()
                .eq(SysNotificationTranslation::getNotificationId, notificationId)
                .eq(SysNotificationTranslation::getLocale, locale)
                .last("LIMIT 1"));
        var now = DateTimeUtils.now();
        var translation = existing == null ? new SysNotificationTranslation() : existing;
        translation.setNotificationId(notificationId);
        translation.setLocale(locale);
        translation.setTitle(title == null && LanguageResolver.DEFAULT_LANGUAGE.equals(locale) ? defaultTitle : title);
        translation.setContent(content == null && LanguageResolver.DEFAULT_LANGUAGE.equals(locale) ? defaultContent : content);
        if (existing == null) {
            translation.setCreatedAt(now);
            translation.setUpdatedAt(now);
            notificationTranslationMapper.insert(translation);
        } else {
            translation.setUpdatedAt(now);
            notificationTranslationMapper.updateById(translation);
        }
        return notificationTranslationResponse(notificationId, locale, translation);
    }

    private NotificationTranslationResponse notificationTranslationResponse(
            Long notificationId,
            String locale,
            SysNotificationTranslation translation
    ) {
        var configured = translation != null
                && (StringUtils.hasText(translation.getTitle()) || StringUtils.hasText(translation.getContent()));
        return new NotificationTranslationResponse(
                notificationId,
                locale,
                translation == null ? null : translation.getTitle(),
                translation == null ? null : translation.getContent(),
                configured,
                translation == null ? null : translation.getCreatedAt(),
                translation == null ? null : translation.getUpdatedAt()
        );
    }

    private String requireSupportedLocale(String locale) {
        var normalized = StringUtils.hasText(locale) ? locale.trim().replace('_', '-') : null;
        if (LanguageResolver.DEFAULT_LANGUAGE.equals(normalized) || LanguageResolver.EN_US.equals(normalized)) {
            return normalized;
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "Unsupported locale: " + locale);
    }

    private LocalizedText localized(String defaultValue, String requestedLocale, String translatedValue) {
        if (LanguageResolver.DEFAULT_LANGUAGE.equals(requestedLocale)) {
            return new LocalizedText(defaultValue, requestedLocale, false);
        }
        if (StringUtils.hasText(translatedValue)) {
            return new LocalizedText(translatedValue, requestedLocale, false);
        }
        if (!StringUtils.hasText(defaultValue)) {
            return new LocalizedText(defaultValue, requestedLocale, false);
        }
        return new LocalizedText(defaultValue, LanguageResolver.DEFAULT_LANGUAGE, true);
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

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record LocalizedText(String value, String locale, boolean fallback) {
    }
}

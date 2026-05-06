package com.compute.rental.modules.recharge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.compute.rental.common.enums.CommonStatus;
import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.enums.RechargeOrderStatus;
import com.compute.rental.common.enums.WalletBusinessType;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.common.i18n.LanguageResolver;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.common.util.DateTimeUtils;
import com.compute.rental.common.util.MoneyUtils;
import com.compute.rental.common.util.RedisKeys;
import com.compute.rental.common.util.RedisLockClient;
import com.compute.rental.common.util.RedisLockClient.RedisLock;
import com.compute.rental.modules.recharge.dto.AdminApproveRechargeRequest;
import com.compute.rental.modules.recharge.dto.AdminRechargeChannelResponse;
import com.compute.rental.modules.recharge.dto.AdminRejectRechargeRequest;
import com.compute.rental.modules.recharge.dto.CreateRechargeChannelRequest;
import com.compute.rental.modules.recharge.dto.CreateRechargeOrderRequest;
import com.compute.rental.modules.recharge.dto.RechargeChannelQueryRequest;
import com.compute.rental.modules.recharge.dto.RechargeChannelResponse;
import com.compute.rental.modules.recharge.dto.RechargeChannelTranslationRequest;
import com.compute.rental.modules.recharge.dto.RechargeChannelTranslationResponse;
import com.compute.rental.modules.recharge.dto.RechargeOrderQueryRequest;
import com.compute.rental.modules.recharge.dto.RechargeOrderResponse;
import com.compute.rental.modules.recharge.dto.UpdateRechargeChannelRequest;
import com.compute.rental.modules.system.service.SysConfigDefaults;
import com.compute.rental.modules.system.service.SysConfigService;
import com.compute.rental.modules.user.entity.AppUser;
import com.compute.rental.modules.user.mapper.AppUserMapper;
import com.compute.rental.modules.wallet.entity.RechargeChannel;
import com.compute.rental.modules.wallet.entity.RechargeChannelTranslation;
import com.compute.rental.modules.wallet.entity.RechargeOrder;
import com.compute.rental.modules.wallet.entity.UserWallet;
import com.compute.rental.modules.wallet.mapper.RechargeChannelMapper;
import com.compute.rental.modules.wallet.mapper.RechargeChannelTranslationMapper;
import com.compute.rental.modules.wallet.mapper.RechargeOrderMapper;
import com.compute.rental.modules.wallet.mapper.UserWalletMapper;
import com.compute.rental.modules.wallet.service.WalletService;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
public class RechargeService {

    private static final String CURRENCY_USDT = "USDT";
    private static final String APPROVE_ACTION = "APPROVE";
    private static final Duration RECHARGE_OPERATION_LOCK_TTL = Duration.ofMinutes(1);

    private final RechargeChannelMapper rechargeChannelMapper;
    private final RechargeChannelTranslationMapper rechargeChannelTranslationMapper;
    private final RechargeOrderMapper rechargeOrderMapper;
    private final UserWalletMapper userWalletMapper;
    private final AppUserMapper appUserMapper;
    private final SysConfigService sysConfigService;
    private final WalletService walletService;
    private final RedisLockClient redisLockClient;

    public RechargeService(
            RechargeChannelMapper rechargeChannelMapper,
            RechargeChannelTranslationMapper rechargeChannelTranslationMapper,
            RechargeOrderMapper rechargeOrderMapper,
            UserWalletMapper userWalletMapper,
            AppUserMapper appUserMapper,
            SysConfigService sysConfigService,
            WalletService walletService,
            RedisLockClient redisLockClient
    ) {
        this.rechargeChannelMapper = rechargeChannelMapper;
        this.rechargeChannelTranslationMapper = rechargeChannelTranslationMapper;
        this.rechargeOrderMapper = rechargeOrderMapper;
        this.userWalletMapper = userWalletMapper;
        this.appUserMapper = appUserMapper;
        this.sysConfigService = sysConfigService;
        this.walletService = walletService;
        this.redisLockClient = redisLockClient;
    }

    public List<RechargeChannelResponse> listEnabledChannels() {
        return listEnabledChannels(LanguageResolver.DEFAULT_LANGUAGE);
    }

    public List<RechargeChannelResponse> listEnabledChannels(String locale) {
        var channels = rechargeChannelMapper.selectList(new LambdaQueryWrapper<RechargeChannel>()
                        .eq(RechargeChannel::getStatus, CommonStatus.ENABLED.value())
                        .orderByAsc(RechargeChannel::getSortNo)
                        .orderByDesc(RechargeChannel::getId));
        var translations = rechargeChannelTranslationMap(channels.stream().map(RechargeChannel::getId).toList(), locale);
        return channels
                .stream()
                .map(channel -> toChannelResponse(channel, translations.get(channel.getId()), locale))
                .toList();
    }

    public PageResult<AdminRechargeChannelResponse> pageAdminChannels(RechargeChannelQueryRequest request) {
        var page = new Page<RechargeChannel>(request.current(), request.size());
        var wrapper = new LambdaQueryWrapper<RechargeChannel>()
                .eq(request.status() != null, RechargeChannel::getStatus, request.status())
                .orderByAsc(RechargeChannel::getSortNo)
                .orderByDesc(RechargeChannel::getId);
        var result = rechargeChannelMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getRecords().stream()
                .map(this::toAdminChannelResponse)
                .toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    public List<RechargeChannelTranslationResponse> listChannelTranslations(Long channelId) {
        var channel = requireChannel(channelId);
        var translations = rechargeChannelTranslationMap(List.of(channelId), LanguageResolver.EN_US);
        return List.of(
                new RechargeChannelTranslationResponse(
                        channelId,
                        LanguageResolver.DEFAULT_LANGUAGE,
                        channel.getChannelName(),
                        channel.getAccountName(),
                        true,
                        channel.getCreatedAt(),
                        channel.getUpdatedAt()
                ),
                rechargeChannelTranslationResponse(channelId, LanguageResolver.EN_US, translations.get(channelId))
        );
    }

    @Transactional
    public RechargeChannelTranslationResponse updateChannelTranslation(
            Long channelId,
            RechargeChannelTranslationRequest request
    ) {
        var channel = requireChannel(channelId);
        var locale = requireSupportedLocale(request.locale());
        var channelName = trimToNull(request.channelName());
        var accountName = trimToNull(request.accountName());
        if (LanguageResolver.DEFAULT_LANGUAGE.equals(locale)) {
            rechargeChannelMapper.update(null, new LambdaUpdateWrapper<RechargeChannel>()
                    .eq(RechargeChannel::getId, channelId)
                    .set(RechargeChannel::getChannelName, channelName == null ? channel.getChannelName() : channelName)
                    .set(RechargeChannel::getAccountName, accountName == null ? channel.getAccountName() : accountName)
                    .set(RechargeChannel::getUpdatedAt, DateTimeUtils.now()));
            channel.setChannelName(channelName == null ? channel.getChannelName() : channelName);
            channel.setAccountName(accountName == null ? channel.getAccountName() : accountName);
        }
        return upsertRechargeChannelTranslation(channelId, locale, channel.getChannelName(), channel.getAccountName(),
                channelName, accountName);
    }

    @Transactional
    public AdminRechargeChannelResponse createChannel(CreateRechargeChannelRequest request) {
        validateAmountRange(request.minAmount(), request.maxAmount());
        var now = DateTimeUtils.now();
        var channel = new RechargeChannel();
        applyCreateChannelRequest(channel, request);
        channel.setCreatedAt(now);
        channel.setUpdatedAt(now);
        try {
            rechargeChannelMapper.insert(channel);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.RECHARGE_CHANNEL_CODE_EXISTS);
        }
        return toAdminChannelResponse(channel);
    }

    @Transactional
    public AdminRechargeChannelResponse updateChannel(Long id, UpdateRechargeChannelRequest request) {
        requireChannel(id);
        validateAmountRange(request.minAmount(), request.maxAmount());
        int updated;
        try {
            updated = rechargeChannelMapper.update(null, new LambdaUpdateWrapper<RechargeChannel>()
                    .eq(RechargeChannel::getId, id)
                    .set(RechargeChannel::getChannelCode, trimToNull(request.channelCode()))
                    .set(RechargeChannel::getChannelName, trimToNull(request.channelName()))
                    .set(RechargeChannel::getNetwork, trimToNull(request.network()))
                    .set(RechargeChannel::getDisplayUrl, trimToNull(request.displayUrl()))
                    .set(RechargeChannel::getAccountName, trimToNull(request.accountName()))
                    .set(RechargeChannel::getAccountNo, trimToNull(request.accountNo()))
                    .set(RechargeChannel::getMinAmount, request.minAmount())
                    .set(RechargeChannel::getMaxAmount, request.maxAmount())
                    .set(RechargeChannel::getFeeRate, request.feeRate())
                    .set(RechargeChannel::getSortNo, request.sortNo())
                    .set(RechargeChannel::getStatus, request.status())
                    .set(RechargeChannel::getUpdatedAt, DateTimeUtils.now()));
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.RECHARGE_CHANNEL_CODE_EXISTS);
        }
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CONCURRENT_UPDATE_FAILED);
        }
        return toAdminChannelResponse(requireChannel(id));
    }

    @Transactional
    public void deleteChannel(Long id) {
        requireChannel(id);
        var usedCount = rechargeOrderMapper.selectCount(new LambdaQueryWrapper<RechargeOrder>()
                .eq(RechargeOrder::getChannelId, id));
        if (usedCount > 0) {
            throw new BusinessException(ErrorCode.RECHARGE_CHANNEL_IN_USE);
        }
        rechargeChannelMapper.deleteById(id);
    }

    @Transactional
    public RechargeOrderResponse createOrder(Long userId, CreateRechargeOrderRequest request) {
        return withRechargeLock(RedisKeys.rechargeCreateLock(userId), () -> doCreateOrder(userId, request));
    }

    private RechargeOrderResponse doCreateOrder(Long userId, CreateRechargeOrderRequest request) {
        var channel = requireEnabledChannel(request.channelId());
        var amount = MoneyUtils.requireNonNegative(request.applyAmount());
        if (amount.signum() <= 0) {
            throw new BusinessException(ErrorCode.RECHARGE_AMOUNT_INVALID);
        }
        var minAmount = effectiveMinAmount(channel);
        if (amount.compareTo(minAmount) < 0) {
            throw new BusinessException(ErrorCode.RECHARGE_AMOUNT_BELOW_MIN);
        }
        var externalTxNo = normalizeExternalTxNo(request.externalTxNo());
        ensureExternalTxNoAvailable(externalTxNo);
        var wallet = requireWallet(userId);
        var now = DateTimeUtils.now();

        var order = new RechargeOrder();
        order.setRechargeNo(generateRechargeNo());
        order.setUserId(userId);
        order.setWalletId(wallet.getId());
        order.setChannelId(channel.getId());
        order.setCurrency(CURRENCY_USDT);
        order.setChannelNameSnapshot(channel.getChannelName());
        order.setNetworkSnapshot(channel.getNetwork());
        order.setDisplayUrlSnapshot(channel.getDisplayUrl());
        order.setAccountNoSnapshot(channel.getAccountNo());
        order.setApplyAmount(amount);
        order.setExternalTxNo(externalTxNo);
        order.setPaymentProofUrl(trimToNull(request.paymentProofUrl()));
        order.setUserRemark(trimToNull(request.userRemark()));
        order.setStatus(RechargeOrderStatus.SUBMITTED.name());
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        try {
            rechargeOrderMapper.insert(order);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.RECHARGE_ORDER_DUPLICATE);
        }
        return toOrderResponse(order);
    }

    public PageResult<RechargeOrderResponse> pageUserOrders(Long userId, RechargeOrderQueryRequest request) {
        var page = new Page<RechargeOrder>(request.current(), request.size());
        var wrapper = baseOrderQuery()
                .eq(RechargeOrder::getUserId, userId)
                .eq(request.status() != null, RechargeOrder::getStatus,
                        request.status() == null ? null : request.status().name())
                .ge(request.startTime() != null, RechargeOrder::getCreatedAt, request.startTime())
                .le(request.endTime() != null, RechargeOrder::getCreatedAt, request.endTime())
                .orderByDesc(RechargeOrder::getId);
        var result = rechargeOrderMapper.selectPage(page, wrapper);
        var userNames = userNameMap(result.getRecords().stream().map(RechargeOrder::getUserId).toList());
        return new PageResult<>(result.getRecords().stream()
                .map(order -> toOrderResponse(order, userNames.get(order.getUserId())))
                .toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    public RechargeOrderResponse getUserOrder(Long userId, String rechargeNo) {
        return toOrderResponse(requireUserOrder(userId, rechargeNo));
    }

    @Transactional
    public void cancelUserOrder(Long userId, String rechargeNo) {
        withRechargeLock(RedisKeys.rechargeOperationLock(rechargeNo, "cancel"), () -> {
            doCancelUserOrder(userId, rechargeNo);
            return null;
        });
    }

    private void doCancelUserOrder(Long userId, String rechargeNo) {
        var order = requireUserOrder(userId, rechargeNo);
        if (!RechargeOrderStatus.SUBMITTED.name().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.RECHARGE_ORDER_NOT_CANCELABLE);
        }
        var updated = rechargeOrderMapper.update(null, new LambdaUpdateWrapper<RechargeOrder>()
                .eq(RechargeOrder::getId, order.getId())
                .eq(RechargeOrder::getStatus, RechargeOrderStatus.SUBMITTED.name())
                .set(RechargeOrder::getStatus, RechargeOrderStatus.CANCELED.name())
                .set(RechargeOrder::getUpdatedAt, DateTimeUtils.now()));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.RECHARGE_ORDER_STATUS_CHANGED);
        }
    }

    public PageResult<RechargeOrderResponse> pageAdminOrders(RechargeOrderQueryRequest request) {
        var page = new Page<RechargeOrder>(request.current(), request.size());
        var wrapper = baseOrderQuery()
                .eq(request.status() != null, RechargeOrder::getStatus,
                        request.status() == null ? null : request.status().name())
                .ge(request.startTime() != null, RechargeOrder::getCreatedAt, request.startTime())
                .le(request.endTime() != null, RechargeOrder::getCreatedAt, request.endTime())
                .orderByDesc(RechargeOrder::getId);
        var result = rechargeOrderMapper.selectPage(page, wrapper);
        var userNames = userNameMap(result.getRecords().stream().map(RechargeOrder::getUserId).toList());
        return new PageResult<>(result.getRecords().stream()
                .map(order -> toOrderResponse(order, userNames.get(order.getUserId())))
                .toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    public RechargeOrderResponse getAdminOrder(String rechargeNo) {
        return toOrderResponse(requireOrder(rechargeNo));
    }

    @Transactional
    public RechargeOrderResponse approve(String rechargeNo, Long reviewedBy, AdminApproveRechargeRequest request) {
        return withRechargeLock(RedisKeys.rechargeOperationLock(rechargeNo, "approve"),
                () -> doApprove(rechargeNo, reviewedBy, request));
    }

    private RechargeOrderResponse doApprove(String rechargeNo, Long reviewedBy, AdminApproveRechargeRequest request) {
        var order = requireOrder(rechargeNo);
        if (!RechargeOrderStatus.SUBMITTED.name().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.RECHARGE_ORDER_NOT_APPROVABLE);
        }
        var actualAmount = MoneyUtils.requireNonNegative(request.actualAmount());
        if (actualAmount.signum() <= 0) {
            throw new BusinessException(ErrorCode.RECHARGE_AMOUNT_INVALID, "实际到账金额必须大于 0");
        }
        var now = DateTimeUtils.now();
        var updated = rechargeOrderMapper.update(null, new LambdaUpdateWrapper<RechargeOrder>()
                .eq(RechargeOrder::getId, order.getId())
                .eq(RechargeOrder::getStatus, RechargeOrderStatus.SUBMITTED.name())
                .set(RechargeOrder::getStatus, RechargeOrderStatus.APPROVED.name())
                .set(RechargeOrder::getActualAmount, actualAmount)
                .set(RechargeOrder::getReviewedBy, reviewedBy)
                .set(RechargeOrder::getReviewedAt, now)
                .set(RechargeOrder::getReviewRemark, trimToNull(request.reviewRemark()))
                .set(RechargeOrder::getCreditedAt, now)
                .set(RechargeOrder::getUpdatedAt, now));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.RECHARGE_ORDER_STATUS_CHANGED);
        }

        var tx = walletService.credit(
                order.getUserId(),
                actualAmount,
                WalletBusinessType.RECHARGE,
                order.getRechargeNo(),
                APPROVE_ACTION,
                "Recharge approved"
        );
        rechargeOrderMapper.update(null, new LambdaUpdateWrapper<RechargeOrder>()
                .eq(RechargeOrder::getId, order.getId())
                .set(RechargeOrder::getWalletTxNo, tx.getTxNo())
                .set(RechargeOrder::getUpdatedAt, DateTimeUtils.now()));

        return getAdminOrder(rechargeNo);
    }

    @Transactional
    public RechargeOrderResponse reject(String rechargeNo, Long reviewedBy, AdminRejectRechargeRequest request) {
        return withRechargeLock(RedisKeys.rechargeOperationLock(rechargeNo, "reject"),
                () -> doReject(rechargeNo, reviewedBy, request));
    }

    private RechargeOrderResponse doReject(String rechargeNo, Long reviewedBy, AdminRejectRechargeRequest request) {
        var order = requireOrder(rechargeNo);
        if (!RechargeOrderStatus.SUBMITTED.name().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.RECHARGE_ORDER_NOT_REJECTABLE);
        }
        var now = DateTimeUtils.now();
        var updated = rechargeOrderMapper.update(null, new LambdaUpdateWrapper<RechargeOrder>()
                .eq(RechargeOrder::getId, order.getId())
                .eq(RechargeOrder::getStatus, RechargeOrderStatus.SUBMITTED.name())
                .set(RechargeOrder::getStatus, RechargeOrderStatus.REJECTED.name())
                .set(RechargeOrder::getReviewedBy, reviewedBy)
                .set(RechargeOrder::getReviewedAt, now)
                .set(RechargeOrder::getReviewRemark, trimToNull(request.reviewRemark()))
                .set(RechargeOrder::getUpdatedAt, now));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.RECHARGE_ORDER_STATUS_CHANGED);
        }
        return getAdminOrder(rechargeNo);
    }

    private <T> T withRechargeLock(String lockKey, Supplier<T> action) {
        var lock = redisLockClient.tryLock(lockKey, RECHARGE_OPERATION_LOCK_TTL);
        if (lock.isEmpty()) {
            throw new BusinessException(ErrorCode.RECHARGE_ORDER_PROCESSING);
        }
        var releaseRegistered = registerUnlockAfterTransaction(lock.get());
        try {
            return action.get();
        } finally {
            if (!releaseRegistered) {
                redisLockClient.unlock(lock.get());
            }
        }
    }

    private boolean registerUnlockAfterTransaction(RedisLock lock) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return false;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                redisLockClient.unlock(lock);
            }
        });
        return true;
    }

    private RechargeChannel requireEnabledChannel(Long channelId) {
        var channel = rechargeChannelMapper.selectById(channelId);
        if (channel == null || !Integer.valueOf(CommonStatus.ENABLED.value()).equals(channel.getStatus())) {
            throw new BusinessException(ErrorCode.RECHARGE_CHANNEL_DISABLED);
        }
        return channel;
    }

    private RechargeChannel requireChannel(Long channelId) {
        var channel = rechargeChannelMapper.selectById(channelId);
        if (channel == null) {
            throw new BusinessException(ErrorCode.RECHARGE_CHANNEL_NOT_FOUND);
        }
        return channel;
    }

    private UserWallet requireWallet(Long userId) {
        var wallet = userWalletMapper.selectOne(new LambdaQueryWrapper<UserWallet>()
                .eq(UserWallet::getUserId, userId)
                .last("LIMIT 1"));
        if (wallet == null) {
            throw new BusinessException(ErrorCode.WALLET_NOT_FOUND);
        }
        if (!Integer.valueOf(CommonStatus.ENABLED.value()).equals(wallet.getStatus())) {
            throw new BusinessException(ErrorCode.WALLET_DISABLED);
        }
        return wallet;
    }

    private BigDecimal effectiveMinAmount(RechargeChannel channel) {
        var globalMin = sysConfigService.getBigDecimal(SysConfigDefaults.RECHARGE_MIN_AMOUNT, new BigDecimal("500"));
        if (channel.getMinAmount() == null) {
            return globalMin;
        }
        return globalMin.max(channel.getMinAmount());
    }

    private void ensureExternalTxNoAvailable(String externalTxNo) {
        if (!StringUtils.hasText(externalTxNo)) {
            return;
        }
        var existing = rechargeOrderMapper.selectOne(new LambdaQueryWrapper<RechargeOrder>()
                .eq(RechargeOrder::getExternalTxNo, externalTxNo)
                .last("LIMIT 1"));
        if (existing != null) {
            throw new BusinessException(ErrorCode.RECHARGE_EXTERNAL_TX_NO_EXISTS);
        }
    }

    private RechargeOrder requireUserOrder(Long userId, String rechargeNo) {
        var order = rechargeOrderMapper.selectOne(baseOrderQuery()
                .eq(RechargeOrder::getUserId, userId)
                .eq(RechargeOrder::getRechargeNo, rechargeNo)
                .last("LIMIT 1"));
        if (order == null) {
            throw new BusinessException(ErrorCode.RECHARGE_ORDER_NOT_FOUND);
        }
        return order;
    }

    private RechargeOrder requireOrder(String rechargeNo) {
        var order = rechargeOrderMapper.selectOne(baseOrderQuery()
                .eq(RechargeOrder::getRechargeNo, rechargeNo)
                .last("LIMIT 1"));
        if (order == null) {
            throw new BusinessException(ErrorCode.RECHARGE_ORDER_NOT_FOUND);
        }
        return order;
    }

    private LambdaQueryWrapper<RechargeOrder> baseOrderQuery() {
        return new LambdaQueryWrapper<>();
    }

    private RechargeChannelResponse toChannelResponse(RechargeChannel channel) {
        return toChannelResponse(channel, null, LanguageResolver.DEFAULT_LANGUAGE);
    }

    private RechargeChannelResponse toChannelResponse(
            RechargeChannel channel,
            RechargeChannelTranslation translation,
            String requestedLocale
    ) {
        var channelName = localized(channel.getChannelName(), requestedLocale,
                translation == null ? null : translation.getChannelName());
        var accountName = localized(channel.getAccountName(), requestedLocale,
                translation == null ? null : translation.getAccountName());
        var localeFallback = channelName.fallback() || accountName.fallback();
        return new RechargeChannelResponse(
                channel.getId(),
                channel.getChannelCode(),
                channelName.value(),
                channel.getNetwork(),
                channel.getDisplayUrl(),
                accountName.value(),
                channel.getAccountNo(),
                channel.getMinAmount(),
                channel.getMaxAmount(),
                channel.getFeeRate(),
                channel.getSortNo(),
                localeFallback ? LanguageResolver.DEFAULT_LANGUAGE : requestedLocale,
                requestedLocale,
                localeFallback
        );
    }

    private AdminRechargeChannelResponse toAdminChannelResponse(RechargeChannel channel) {
        return new AdminRechargeChannelResponse(
                channel.getId(),
                channel.getChannelCode(),
                channel.getChannelName(),
                channel.getNetwork(),
                channel.getDisplayUrl(),
                channel.getAccountName(),
                channel.getAccountNo(),
                channel.getMinAmount(),
                channel.getMaxAmount(),
                channel.getFeeRate(),
                channel.getSortNo(),
                channel.getStatus(),
                channel.getCreatedAt(),
                channel.getUpdatedAt()
        );
    }

    private void applyCreateChannelRequest(RechargeChannel channel, CreateRechargeChannelRequest request) {
        channel.setChannelCode(trimToNull(request.channelCode()));
        channel.setChannelName(trimToNull(request.channelName()));
        channel.setNetwork(trimToNull(request.network()));
        channel.setDisplayUrl(trimToNull(request.displayUrl()));
        channel.setAccountName(trimToNull(request.accountName()));
        channel.setAccountNo(trimToNull(request.accountNo()));
        channel.setMinAmount(request.minAmount());
        channel.setMaxAmount(request.maxAmount());
        channel.setFeeRate(request.feeRate());
        channel.setSortNo(request.sortNo());
        channel.setStatus(request.status());
    }

    private void validateAmountRange(BigDecimal minAmount, BigDecimal maxAmount) {
        if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
            throw new BusinessException(ErrorCode.RECHARGE_AMOUNT_RANGE_INVALID);
        }
    }

    private RechargeOrderResponse toOrderResponse(RechargeOrder order) {
        return toOrderResponse(order, userName(order.getUserId()));
    }

    private RechargeOrderResponse toOrderResponse(RechargeOrder order, String userName) {
        return new RechargeOrderResponse(
                order.getRechargeNo(),
                userName,
                order.getChannelId(),
                order.getCurrency(),
                order.getChannelNameSnapshot(),
                order.getNetworkSnapshot(),
                order.getDisplayUrlSnapshot(),
                order.getAccountNoSnapshot(),
                order.getApplyAmount(),
                order.getActualAmount(),
                order.getExternalTxNo(),
                order.getPaymentProofUrl(),
                order.getUserRemark(),
                order.getStatus(),
                order.getReviewedBy(),
                order.getReviewedAt(),
                order.getReviewRemark(),
                order.getCreditedAt(),
                order.getWalletTxNo(),
                order.getCreatedAt()
        );
    }

    private String normalizeExternalTxNo(String externalTxNo) {
        return trimToNull(externalTxNo);
    }

    private String userName(Long userId) {
        var user = userId == null ? null : appUserMapper.selectById(userId);
        return user == null ? null : user.getUserName();
    }

    private Map<Long, String> userNameMap(List<Long> userIds) {
        var ids = userIds.stream().filter(id -> id != null).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        var userNames = new HashMap<Long, String>();
        for (var user : appUserMapper.selectBatchIds(ids)) {
            userNames.put(user.getId(), user.getUserName());
        }
        return userNames;
    }

    private Map<Long, RechargeChannelTranslation> rechargeChannelTranslationMap(Collection<Long> ids, String locale) {
        if (LanguageResolver.DEFAULT_LANGUAGE.equals(locale) || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return rechargeChannelTranslationMapper.selectList(new LambdaQueryWrapper<RechargeChannelTranslation>()
                        .in(RechargeChannelTranslation::getChannelId, ids)
                        .eq(RechargeChannelTranslation::getLocale, locale))
                .stream()
                .collect(Collectors.toMap(RechargeChannelTranslation::getChannelId, Function.identity()));
    }

    private RechargeChannelTranslationResponse upsertRechargeChannelTranslation(
            Long channelId,
            String locale,
            String defaultChannelName,
            String defaultAccountName,
            String channelName,
            String accountName
    ) {
        var existing = rechargeChannelTranslationMapper.selectOne(new LambdaQueryWrapper<RechargeChannelTranslation>()
                .eq(RechargeChannelTranslation::getChannelId, channelId)
                .eq(RechargeChannelTranslation::getLocale, locale)
                .last("LIMIT 1"));
        var now = DateTimeUtils.now();
        var translation = existing == null ? new RechargeChannelTranslation() : existing;
        translation.setChannelId(channelId);
        translation.setLocale(locale);
        translation.setChannelName(channelName == null && LanguageResolver.DEFAULT_LANGUAGE.equals(locale)
                ? defaultChannelName : channelName);
        translation.setAccountName(accountName == null && LanguageResolver.DEFAULT_LANGUAGE.equals(locale)
                ? defaultAccountName : accountName);
        if (existing == null) {
            translation.setCreatedAt(now);
            translation.setUpdatedAt(now);
            rechargeChannelTranslationMapper.insert(translation);
        } else {
            translation.setUpdatedAt(now);
            rechargeChannelTranslationMapper.updateById(translation);
        }
        return rechargeChannelTranslationResponse(channelId, locale, translation);
    }

    private RechargeChannelTranslationResponse rechargeChannelTranslationResponse(
            Long channelId,
            String locale,
            RechargeChannelTranslation translation
    ) {
        var configured = translation != null
                && (StringUtils.hasText(translation.getChannelName())
                || StringUtils.hasText(translation.getAccountName()));
        return new RechargeChannelTranslationResponse(
                channelId,
                locale,
                translation == null ? null : translation.getChannelName(),
                translation == null ? null : translation.getAccountName(),
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

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String generateRechargeNo() {
        return "RC" + DateTimeUtils.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private record LocalizedText(String value, String locale, boolean fallback) {
    }
}

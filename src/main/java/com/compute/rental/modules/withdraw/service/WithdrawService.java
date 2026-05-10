package com.compute.rental.modules.withdraw.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.compute.rental.common.enums.CommonStatus;
import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.enums.WalletBusinessType;
import com.compute.rental.common.enums.WithdrawOrderStatus;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.common.util.DateTimeUtils;
import com.compute.rental.common.util.MoneyUtils;
import com.compute.rental.common.util.RedisKeys;
import com.compute.rental.common.util.RedisLockClient;
import com.compute.rental.common.util.RedisLockClient.RedisLock;
import com.compute.rental.modules.system.service.SysConfigDefaults;
import com.compute.rental.modules.system.service.SysConfigService;
import com.compute.rental.modules.user.entity.AppUser;
import com.compute.rental.modules.user.mapper.AppUserMapper;
import com.compute.rental.modules.user.support.AppUserSearchSupport;
import com.compute.rental.modules.wallet.entity.UserWallet;
import com.compute.rental.modules.wallet.entity.WithdrawOrder;
import com.compute.rental.modules.wallet.mapper.UserWalletMapper;
import com.compute.rental.modules.wallet.mapper.WithdrawOrderMapper;
import com.compute.rental.modules.wallet.service.WalletService;
import com.compute.rental.modules.withdraw.dto.AdminApproveWithdrawRequest;
import com.compute.rental.modules.withdraw.dto.AdminPaidWithdrawRequest;
import com.compute.rental.modules.withdraw.dto.AdminRejectWithdrawRequest;
import com.compute.rental.modules.withdraw.dto.CreateWithdrawOrderRequest;
import com.compute.rental.modules.withdraw.dto.WithdrawOrderQueryRequest;
import com.compute.rental.modules.withdraw.dto.WithdrawOrderResponse;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
public class WithdrawService {

    private static final String CURRENCY_USDT = "USDT";
    private static final String WITHDRAW_METHOD_USDT = "USDT";
    private static final String FREEZE_ACTION = "FREEZE";
    private static final String UNFREEZE_ACTION = "UNFREEZE";
    private static final String PAID_ACTION = "PAID";
    private static final Duration WITHDRAW_OPERATION_LOCK_TTL = Duration.ofMinutes(1);
    private static final List<String> DAILY_LIMIT_STATUSES = List.of(
            WithdrawOrderStatus.PENDING_REVIEW.name(),
            WithdrawOrderStatus.APPROVED.name(),
            WithdrawOrderStatus.PAID.name()
    );

    private final WithdrawOrderMapper withdrawOrderMapper;
    private final UserWalletMapper userWalletMapper;
    private final AppUserMapper appUserMapper;
    private final SysConfigService sysConfigService;
    private final WalletService walletService;
    private final WithdrawAddressValidator addressValidator;
    private final RedisLockClient redisLockClient;

    public WithdrawService(
            WithdrawOrderMapper withdrawOrderMapper,
            UserWalletMapper userWalletMapper,
            AppUserMapper appUserMapper,
            SysConfigService sysConfigService,
            WalletService walletService,
            WithdrawAddressValidator addressValidator,
            RedisLockClient redisLockClient
    ) {
        this.withdrawOrderMapper = withdrawOrderMapper;
        this.userWalletMapper = userWalletMapper;
        this.appUserMapper = appUserMapper;
        this.sysConfigService = sysConfigService;
        this.walletService = walletService;
        this.addressValidator = addressValidator;
        this.redisLockClient = redisLockClient;
    }

    @Transactional
    public WithdrawOrderResponse createOrder(Long userId, CreateWithdrawOrderRequest request) {
        return withWithdrawLock(RedisKeys.withdrawCreateLock(userId), () -> doCreateOrder(userId, request));
    }

    private WithdrawOrderResponse doCreateOrder(Long userId, CreateWithdrawOrderRequest request) {
        var clientRequestId = requireClientRequestId(request.clientRequestId());
        var existing = findOrderByClientRequestId(userId, clientRequestId);
        if (existing != null) {
            validateCreateOrderIdempotency(existing, request);
            return toResponse(existing);
        }
        var amount = requirePositiveAmount(request.applyAmount());
        var minAmount = sysConfigService.getBigDecimal(SysConfigDefaults.WITHDRAW_MIN_AMOUNT, new BigDecimal("10"));
        if (amount.compareTo(minAmount) < 0) {
            throw new BusinessException(ErrorCode.WITHDRAW_AMOUNT_BELOW_MIN);
        }
        var maxDailyAmount = sysConfigService.getBigDecimal(SysConfigDefaults.WITHDRAW_MAX_DAILY_AMOUNT, new BigDecimal("100000"));
        var todayAmount = todayActiveWithdrawAmount(userId);
        if (todayAmount.add(amount).compareTo(maxDailyAmount) > 0) {
            throw new BusinessException(ErrorCode.WITHDRAW_DAILY_LIMIT_EXCEEDED);
        }
        var network = addressValidator.requireValid(request.network(), request.accountNo());
        var feeAmount = calculateFee(amount);
        var actualAmount = amount.subtract(feeAmount);
        var wallet = requireWallet(userId);
        var now = DateTimeUtils.now();

        var order = new WithdrawOrder();
        order.setWithdrawNo(generateWithdrawNo());
        order.setClientRequestId(clientRequestId);
        order.setUserId(userId);
        order.setWalletId(wallet.getId());
        order.setCurrency(CURRENCY_USDT);
        order.setWithdrawMethod(WITHDRAW_METHOD_USDT);
        order.setNetwork(network);
        order.setAccountName(trimToNull(request.accountName()));
        order.setAccountNo(request.accountNo().trim());
        order.setApplyAmount(amount);
        order.setFeeAmount(feeAmount);
        order.setActualAmount(actualAmount);
        order.setStatus(WithdrawOrderStatus.PENDING_REVIEW.name());
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        try {
            withdrawOrderMapper.insert(order);
        } catch (DuplicateKeyException ex) {
            var created = findOrderByClientRequestId(userId, clientRequestId);
            if (created != null) {
                validateCreateOrderIdempotency(created, request);
                return toResponse(created);
            }
            throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT, "客户端请求号已被其他提现订单使用");
        }

        var tx = walletService.freeze(
                userId,
                amount,
                WalletBusinessType.WITHDRAW,
                order.getWithdrawNo(),
                FREEZE_ACTION,
                "提现冻结"
        );
        withdrawOrderMapper.update(null, new LambdaUpdateWrapper<WithdrawOrder>()
                .eq(WithdrawOrder::getId, order.getId())
                .set(WithdrawOrder::getFreezeTxNo, tx.getTxNo())
                .set(WithdrawOrder::getUpdatedAt, DateTimeUtils.now()));

        return getUserOrder(userId, order.getWithdrawNo());
    }

    public PageResult<WithdrawOrderResponse> pageUserOrders(Long userId, WithdrawOrderQueryRequest request) {
        var page = new Page<WithdrawOrder>(request.current(), request.size());
        var wrapper = baseOrderQuery()
                .eq(WithdrawOrder::getUserId, userId)
                .eq(request.status() != null, WithdrawOrder::getStatus,
                        request.status() == null ? null : request.status().name())
                .ge(request.startTime() != null, WithdrawOrder::getCreatedAt, request.startTime())
                .le(request.endTime() != null, WithdrawOrder::getCreatedAt, request.endTime())
                .orderByDesc(WithdrawOrder::getId);
        var result = withdrawOrderMapper.selectPage(page, wrapper);
        var userIdentities = userIdentityMap(result.getRecords().stream().map(WithdrawOrder::getUserId).toList());
        return new PageResult<>(result.getRecords().stream()
                .map(order -> toResponse(order, userIdentities.get(order.getUserId())))
                .toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    public WithdrawOrderResponse getUserOrder(Long userId, String withdrawNo) {
        return toResponse(requireUserOrder(userId, withdrawNo));
    }

    @Transactional
    public void cancelUserOrder(Long userId, String withdrawNo) {
        withWithdrawLock(RedisKeys.withdrawOperationLock(withdrawNo, "cancel"), () -> {
            doCancelUserOrder(userId, withdrawNo);
            return null;
        });
    }

    private void doCancelUserOrder(Long userId, String withdrawNo) {
        var order = requireUserOrder(userId, withdrawNo);
        if (!WithdrawOrderStatus.PENDING_REVIEW.name().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.WITHDRAW_ORDER_NOT_CANCELABLE);
        }
        updateStatus(order, WithdrawOrderStatus.CANCELED, null, null, null);
        var tx = walletService.unfreeze(order.getUserId(), order.getApplyAmount(), WalletBusinessType.WITHDRAW,
                order.getWithdrawNo(), UNFREEZE_ACTION, "提现取消解冻");
        updateUnfreezeTxNo(order.getId(), tx.getTxNo());
    }

    public PageResult<WithdrawOrderResponse> pageAdminOrders(WithdrawOrderQueryRequest request) {
        var keywordUserIds = userIdsByKeyword(request.keyword());
        if (StringUtils.hasText(request.keyword()) && keywordUserIds.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0, request.current(), request.size());
        }
        var page = new Page<WithdrawOrder>(request.current(), request.size());
        var wrapper = baseOrderQuery()
                .in(!keywordUserIds.isEmpty(), WithdrawOrder::getUserId, keywordUserIds)
                .eq(request.status() != null, WithdrawOrder::getStatus,
                        request.status() == null ? null : request.status().name())
                .ge(request.startTime() != null, WithdrawOrder::getCreatedAt, request.startTime())
                .le(request.endTime() != null, WithdrawOrder::getCreatedAt, request.endTime())
                .orderByDesc(WithdrawOrder::getId);
        var result = withdrawOrderMapper.selectPage(page, wrapper);
        var userIdentities = userIdentityMap(result.getRecords().stream().map(WithdrawOrder::getUserId).toList());
        return new PageResult<>(result.getRecords().stream()
                .map(order -> toResponse(order, userIdentities.get(order.getUserId())))
                .toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    public WithdrawOrderResponse getAdminOrder(String withdrawNo) {
        return toResponse(requireOrder(withdrawNo));
    }

    @Transactional
    public WithdrawOrderResponse approve(String withdrawNo, Long reviewedBy, AdminApproveWithdrawRequest request) {
        return withWithdrawLock(RedisKeys.withdrawOperationLock(withdrawNo, "approve"),
                () -> doApprove(withdrawNo, reviewedBy, request));
    }

    private WithdrawOrderResponse doApprove(String withdrawNo, Long reviewedBy, AdminApproveWithdrawRequest request) {
        var order = requireOrder(withdrawNo);
        if (!WithdrawOrderStatus.PENDING_REVIEW.name().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.WITHDRAW_ORDER_NOT_APPROVABLE);
        }
        updateStatus(order, WithdrawOrderStatus.APPROVED, reviewedBy, request.reviewRemark(), null);
        return getAdminOrder(withdrawNo);
    }

    @Transactional
    public WithdrawOrderResponse reject(String withdrawNo, Long reviewedBy, AdminRejectWithdrawRequest request) {
        return withWithdrawLock(RedisKeys.withdrawOperationLock(withdrawNo, "reject"),
                () -> doReject(withdrawNo, reviewedBy, request));
    }

    private WithdrawOrderResponse doReject(String withdrawNo, Long reviewedBy, AdminRejectWithdrawRequest request) {
        var order = requireOrder(withdrawNo);
        if (!WithdrawOrderStatus.PENDING_REVIEW.name().equals(order.getStatus())
                && !WithdrawOrderStatus.APPROVED.name().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.WITHDRAW_ORDER_NOT_REJECTABLE);
        }
        updateStatus(order, WithdrawOrderStatus.REJECTED, reviewedBy, request.reviewRemark(), null);
        var tx = walletService.unfreeze(order.getUserId(), order.getApplyAmount(), WalletBusinessType.WITHDRAW,
                order.getWithdrawNo(), UNFREEZE_ACTION, "提现驳回解冻");
        updateUnfreezeTxNo(order.getId(), tx.getTxNo());
        return getAdminOrder(withdrawNo);
    }

    @Transactional
    public WithdrawOrderResponse paid(String withdrawNo, AdminPaidWithdrawRequest request) {
        return withWithdrawLock(RedisKeys.withdrawOperationLock(withdrawNo, "paid"), () -> doPaid(withdrawNo, request));
    }

    private WithdrawOrderResponse doPaid(String withdrawNo, AdminPaidWithdrawRequest request) {
        var order = requireOrder(withdrawNo);
        if (!WithdrawOrderStatus.APPROVED.name().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.WITHDRAW_ORDER_NOT_PAYABLE);
        }
        var now = DateTimeUtils.now();
        var updated = withdrawOrderMapper.update(null, new LambdaUpdateWrapper<WithdrawOrder>()
                .eq(WithdrawOrder::getId, order.getId())
                .eq(WithdrawOrder::getStatus, WithdrawOrderStatus.APPROVED.name())
                .set(WithdrawOrder::getStatus, WithdrawOrderStatus.PAID.name())
                .set(WithdrawOrder::getPaidAt, now)
                .set(WithdrawOrder::getPayProofNo, trimToNull(request.payProofNo()))
                .set(WithdrawOrder::getUpdatedAt, now));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.WITHDRAW_ORDER_STATUS_CHANGED);
        }
        var tx = walletService.deductFrozen(order.getUserId(), order.getApplyAmount(), order.getActualAmount(),
                WalletBusinessType.WITHDRAW, order.getWithdrawNo(), PAID_ACTION, "提现已打款");
        withdrawOrderMapper.update(null, new LambdaUpdateWrapper<WithdrawOrder>()
                .eq(WithdrawOrder::getId, order.getId())
                .set(WithdrawOrder::getPaidTxNo, tx.getTxNo())
                .set(WithdrawOrder::getUpdatedAt, DateTimeUtils.now()));
        return getAdminOrder(withdrawNo);
    }

    private BigDecimal calculateFee(BigDecimal applyAmount) {
        var threshold = sysConfigService.getBigDecimal(SysConfigDefaults.WITHDRAW_FEE_FREE_THRESHOLD, new BigDecimal("100"));
        if (applyAmount.compareTo(threshold) >= 0) {
            return MoneyUtils.ZERO;
        }
        var rate = sysConfigService.getBigDecimal(SysConfigDefaults.WITHDRAW_FEE_RATE, new BigDecimal("0.05"));
        return MoneyUtils.scale(applyAmount.multiply(rate));
    }

    private BigDecimal todayActiveWithdrawAmount(Long userId) {
        var start = DateTimeUtils.today().atStartOfDay();
        var end = start.plusDays(1);
        return withdrawOrderMapper.selectList(new LambdaQueryWrapper<WithdrawOrder>()
                        .eq(WithdrawOrder::getUserId, userId)
                        .in(WithdrawOrder::getStatus, DAILY_LIMIT_STATUSES)
                        .ge(WithdrawOrder::getCreatedAt, start)
                        .lt(WithdrawOrder::getCreatedAt, end))
                .stream()
                .map(WithdrawOrder::getApplyAmount)
                .reduce(MoneyUtils.ZERO, BigDecimal::add);
    }

    private void updateStatus(WithdrawOrder order, WithdrawOrderStatus nextStatus, Long reviewedBy, String reviewRemark, String payProofNo) {
        var now = DateTimeUtils.now();
        var update = new LambdaUpdateWrapper<WithdrawOrder>()
                .eq(WithdrawOrder::getId, order.getId())
                .eq(WithdrawOrder::getStatus, order.getStatus())
                .set(WithdrawOrder::getStatus, nextStatus.name())
                .set(WithdrawOrder::getUpdatedAt, now);
        if (reviewedBy != null || reviewRemark != null || nextStatus == WithdrawOrderStatus.APPROVED
                || nextStatus == WithdrawOrderStatus.REJECTED) {
            update.set(WithdrawOrder::getReviewedBy, reviewedBy)
                    .set(WithdrawOrder::getReviewedAt, now)
                    .set(WithdrawOrder::getReviewRemark, trimToNull(reviewRemark));
        }
        if (payProofNo != null) {
            update.set(WithdrawOrder::getPayProofNo, trimToNull(payProofNo));
        }
        var updated = withdrawOrderMapper.update(null, update);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.WITHDRAW_ORDER_STATUS_CHANGED);
        }
    }

    private void updateUnfreezeTxNo(Long orderId, String txNo) {
        withdrawOrderMapper.update(null, new LambdaUpdateWrapper<WithdrawOrder>()
                .eq(WithdrawOrder::getId, orderId)
                .set(WithdrawOrder::getUnfreezeTxNo, txNo)
                .set(WithdrawOrder::getUpdatedAt, DateTimeUtils.now()));
    }

    private <T> T withWithdrawLock(String lockKey, Supplier<T> action) {
        var lock = redisLockClient.tryLock(lockKey, WITHDRAW_OPERATION_LOCK_TTL);
        if (lock.isEmpty()) {
            throw new BusinessException(ErrorCode.WITHDRAW_ORDER_PROCESSING);
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

    private BigDecimal requirePositiveAmount(BigDecimal amount) {
        var scaled = MoneyUtils.scale(amount);
        if (scaled.signum() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT, "金额必须大于 0");
        }
        return scaled;
    }

    private WithdrawOrder requireUserOrder(Long userId, String withdrawNo) {
        var order = withdrawOrderMapper.selectOne(baseOrderQuery()
                .eq(WithdrawOrder::getUserId, userId)
                .eq(WithdrawOrder::getWithdrawNo, withdrawNo)
                .last("LIMIT 1"));
        if (order == null) {
            throw new BusinessException(ErrorCode.WITHDRAW_ORDER_NOT_FOUND);
        }
        return order;
    }

    private WithdrawOrder findOrderByClientRequestId(Long userId, String clientRequestId) {
        return withdrawOrderMapper.selectOne(baseOrderQuery()
                .eq(WithdrawOrder::getUserId, userId)
                .eq(WithdrawOrder::getClientRequestId, clientRequestId)
                .last("LIMIT 1"));
    }

    private void validateCreateOrderIdempotency(WithdrawOrder existing, CreateWithdrawOrderRequest request) {
        var requestNetwork = trimToNull(request.network());
        if (!equalsIgnoreCase(existing.getNetwork(), requestNetwork)
                || !java.util.Objects.equals(existing.getAccountName(), trimToNull(request.accountName()))
                || !java.util.Objects.equals(existing.getAccountNo(), request.accountNo() == null ? null : request.accountNo().trim())
                || requirePositiveAmount(request.applyAmount()).compareTo(existing.getApplyAmount()) != 0) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT, "客户端请求号已被其他提现订单使用");
        }
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left == null ? right == null : right != null && left.equalsIgnoreCase(right);
    }

    private String requireClientRequestId(String clientRequestId) {
        var normalized = trimToNull(clientRequestId);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "客户端请求幂等号不能为空");
        }
        return normalized;
    }

    private WithdrawOrder requireOrder(String withdrawNo) {
        var order = withdrawOrderMapper.selectOne(baseOrderQuery()
                .eq(WithdrawOrder::getWithdrawNo, withdrawNo)
                .last("LIMIT 1"));
        if (order == null) {
            throw new BusinessException(ErrorCode.WITHDRAW_ORDER_NOT_FOUND);
        }
        return order;
    }

    private LambdaQueryWrapper<WithdrawOrder> baseOrderQuery() {
        return new LambdaQueryWrapper<>();
    }

    private WithdrawOrderResponse toResponse(WithdrawOrder order) {
        return toResponse(order, userIdentity(order.getUserId()));
    }

    private WithdrawOrderResponse toResponse(WithdrawOrder order, UserIdentity userIdentity) {
        return new WithdrawOrderResponse(
                order.getWithdrawNo(),
                userIdentity == null ? null : userIdentity.userName(),
                userIdentity == null ? null : userIdentity.email(),
                order.getCurrency(),
                order.getWithdrawMethod(),
                order.getNetwork(),
                order.getAccountName(),
                order.getAccountNo(),
                order.getApplyAmount(),
                order.getFeeAmount(),
                order.getActualAmount(),
                order.getStatus(),
                order.getFreezeTxNo(),
                order.getUnfreezeTxNo(),
                order.getPaidTxNo(),
                order.getReviewedBy(),
                order.getReviewedAt(),
                order.getReviewRemark(),
                order.getPaidAt(),
                order.getPayProofNo(),
                order.getCreatedAt()
        );
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private UserIdentity userIdentity(Long userId) {
        var user = userId == null ? null : appUserMapper.selectById(userId);
        return user == null ? null : new UserIdentity(user.getUserName(), user.getEmail());
    }

    private Map<Long, UserIdentity> userIdentityMap(List<Long> userIds) {
        var ids = userIds.stream().filter(id -> id != null).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        var userIdentities = new HashMap<Long, UserIdentity>();
        for (var user : appUserMapper.selectBatchIds(ids)) {
            userIdentities.put(user.getId(), new UserIdentity(user.getUserName(), user.getEmail()));
        }
        return userIdentities;
    }

    private List<Long> userIdsByKeyword(String keyword) {
        var normalizedKeyword = AppUserSearchSupport.normalize(keyword);
        if (!AppUserSearchSupport.hasText(normalizedKeyword)) {
            return Collections.emptyList();
        }
        return appUserMapper.selectList(AppUserSearchSupport.idQuery(normalizedKeyword))
                .stream()
                .map(AppUser::getId)
                .toList();
    }

    private String generateWithdrawNo() {
        return "WD" + DateTimeUtils.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private record UserIdentity(String userName, String email) {
    }
}

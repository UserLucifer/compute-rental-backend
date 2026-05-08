package com.compute.rental.modules.wallet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.compute.rental.common.enums.CommonStatus;
import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.enums.WalletBusinessType;
import com.compute.rental.common.enums.WalletTransactionType;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.common.util.DateTimeUtils;
import com.compute.rental.common.util.MoneyUtils;
import com.compute.rental.common.util.WalletRemarkUtils;
import com.compute.rental.modules.user.entity.AppUser;
import com.compute.rental.modules.user.mapper.AppUserMapper;
import com.compute.rental.modules.wallet.dto.WalletMeResponse;
import com.compute.rental.modules.wallet.dto.WalletTransactionQueryRequest;
import com.compute.rental.modules.wallet.dto.WalletTransactionResponse;
import com.compute.rental.modules.wallet.entity.UserWallet;
import com.compute.rental.modules.wallet.entity.WalletTransaction;
import com.compute.rental.modules.wallet.mapper.UserWalletMapper;
import com.compute.rental.modules.wallet.mapper.WalletTransactionMapper;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class WalletService {

    private static final String CURRENCY_USDT = "USDT";

    private final UserWalletMapper userWalletMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final AppUserMapper appUserMapper;

    public WalletService(
            UserWalletMapper userWalletMapper,
            WalletTransactionMapper walletTransactionMapper,
            AppUserMapper appUserMapper
    ) {
        this.userWalletMapper = userWalletMapper;
        this.walletTransactionMapper = walletTransactionMapper;
        this.appUserMapper = appUserMapper;
    }

    public WalletMeResponse getCurrentUserWallet(Long userId) {
        var wallet = requireEnabledWallet(userId);
        return new WalletMeResponse(
                wallet.getCurrency(),
                wallet.getAvailableBalance(),
                wallet.getFrozenBalance(),
                wallet.getTotalRecharge(),
                wallet.getTotalWithdraw(),
                wallet.getTotalProfit(),
                wallet.getTotalCommission()
        );
    }

    public PageResult<WalletTransactionResponse> pageCurrentUserTransactions(
            Long userId,
            WalletTransactionQueryRequest request
    ) {
        var page = new Page<WalletTransaction>(request.current(), request.size());
        var wrapper = new LambdaQueryWrapper<WalletTransaction>()
                .eq(WalletTransaction::getUserId, userId)
                .eq(request.bizType() != null, WalletTransaction::getBizType,
                        request.bizType() == null ? null : request.bizType().name())
                .eq(request.txType() != null, WalletTransaction::getTxType,
                        request.txType() == null ? null : request.txType().name())
                .ge(request.startTime() != null, WalletTransaction::getCreatedAt, request.startTime())
                .le(request.endTime() != null, WalletTransaction::getCreatedAt, request.endTime())
                .orderByDesc(WalletTransaction::getId);

        var result = walletTransactionMapper.selectPage(page, wrapper);
        var userNames = userNameMap(result.getRecords().stream().map(WalletTransaction::getUserId).toList());
        var records = result.getRecords().stream()
                .map(transaction -> toTransactionResponse(transaction, userNames.get(transaction.getUserId())))
                .toList();
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public WalletTransaction credit(
            Long userId,
            BigDecimal amount,
            WalletBusinessType bizType,
            String bizOrderNo,
            String remark
    ) {
        return credit(userId, amount, bizType, bizOrderNo, WalletChangeAction.IN.name(), remark);
    }

    @Transactional
    public WalletTransaction credit(
            Long userId,
            BigDecimal amount,
            WalletBusinessType bizType,
            String bizOrderNo,
            String idempotencyAction,
            String remark
    ) {
        return change(userId, amount, WalletTransactionType.IN, WalletChangeAction.IN, bizType, bizOrderNo,
                idempotencyAction, null, remark);
    }

    @Transactional
    public WalletTransaction creditWithIdempotencyKey(
            Long userId,
            BigDecimal amount,
            WalletBusinessType bizType,
            String bizOrderNo,
            String idempotencyKey,
            String remark
    ) {
        return change(userId, amount, WalletTransactionType.IN, WalletChangeAction.IN, bizType, bizOrderNo,
                null, idempotencyKey, null, remark);
    }

    @Transactional
    public WalletTransaction debit(
            Long userId,
            BigDecimal amount,
            WalletBusinessType bizType,
            String bizOrderNo,
            String remark
    ) {
        return change(userId, amount, WalletTransactionType.OUT, WalletChangeAction.OUT, bizType, bizOrderNo,
                WalletChangeAction.OUT.name(), null, remark);
    }

    @Transactional
    public WalletTransaction debit(
            Long userId,
            BigDecimal amount,
            WalletBusinessType bizType,
            String bizOrderNo,
            String idempotencyAction,
            String remark
    ) {
        return change(userId, amount, WalletTransactionType.OUT, WalletChangeAction.OUT, bizType, bizOrderNo,
                idempotencyAction, null, remark);
    }

    @Transactional
    public WalletTransaction freeze(
            Long userId,
            BigDecimal amount,
            WalletBusinessType bizType,
            String bizOrderNo,
            String remark
    ) {
        return freeze(userId, amount, bizType, bizOrderNo, WalletChangeAction.FREEZE.name(), remark);
    }

    @Transactional
    public WalletTransaction freeze(
            Long userId,
            BigDecimal amount,
            WalletBusinessType bizType,
            String bizOrderNo,
            String idempotencyAction,
            String remark
    ) {
        return change(userId, amount, WalletTransactionType.FREEZE, WalletChangeAction.FREEZE, bizType, bizOrderNo,
                idempotencyAction, null, remark);
    }

    @Transactional
    public WalletTransaction unfreeze(
            Long userId,
            BigDecimal amount,
            WalletBusinessType bizType,
            String bizOrderNo,
            String remark
    ) {
        return unfreeze(userId, amount, bizType, bizOrderNo, WalletChangeAction.UNFREEZE.name(), remark);
    }

    @Transactional
    public WalletTransaction unfreeze(
            Long userId,
            BigDecimal amount,
            WalletBusinessType bizType,
            String bizOrderNo,
            String idempotencyAction,
            String remark
    ) {
        return change(userId, amount, WalletTransactionType.UNFREEZE, WalletChangeAction.UNFREEZE, bizType, bizOrderNo,
                idempotencyAction, null, remark);
    }

    @Transactional
    public WalletTransaction deductFrozen(
            Long userId,
            BigDecimal amount,
            WalletBusinessType bizType,
            String bizOrderNo,
            String remark
    ) {
        return deductFrozen(userId, amount, null, bizType, bizOrderNo, WalletChangeAction.OUT_FROM_FROZEN.name(), remark);
    }

    @Transactional
    public WalletTransaction deductFrozen(
            Long userId,
            BigDecimal amount,
            BigDecimal totalWithdrawIncrease,
            WalletBusinessType bizType,
            String bizOrderNo,
            String idempotencyAction,
            String remark
    ) {
        return change(userId, amount, WalletTransactionType.OUT, WalletChangeAction.OUT_FROM_FROZEN, bizType, bizOrderNo,
                idempotencyAction, totalWithdrawIncrease, remark);
    }

    @Transactional
    public WalletTransaction recordNoBalanceChange(
            Long userId,
            BigDecimal amount,
            WalletTransactionType txType,
            WalletBusinessType bizType,
            String bizOrderNo,
            String idempotencyKey,
            String remark
    ) {
        var scaledAmount = requirePositiveAmount(amount);
        var existing = findByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            return existing;
        }
        var wallet = requireEnabledWallet(userId);
        var now = DateTimeUtils.now();
        var balanceChange = new WalletBalanceChange(
                wallet.getAvailableBalance(),
                wallet.getAvailableBalance(),
                wallet.getFrozenBalance(),
                wallet.getFrozenBalance()
        );
        var transaction = buildTransaction(wallet, scaledAmount, txType, bizType, bizOrderNo, remark,
                idempotencyKey, balanceChange, now);
        try {
            walletTransactionMapper.insert(transaction);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.WALLET_IDEMPOTENCY_KEY_DUPLICATE);
        }
        return transaction;
    }

    private WalletTransaction change(
            Long userId,
            BigDecimal rawAmount,
            WalletTransactionType txType,
            WalletChangeAction action,
            WalletBusinessType bizType,
            String bizOrderNo,
            String idempotencyAction,
            BigDecimal totalWithdrawIncrease,
            String remark
    ) {
        return change(userId, rawAmount, txType, action, bizType, bizOrderNo, idempotencyAction, null,
                totalWithdrawIncrease, remark);
    }

    private WalletTransaction change(
            Long userId,
            BigDecimal rawAmount,
            WalletTransactionType txType,
            WalletChangeAction action,
            WalletBusinessType bizType,
            String bizOrderNo,
            String idempotencyAction,
            String explicitIdempotencyKey,
            BigDecimal totalWithdrawIncrease,
            String remark
    ) {
        var amount = requirePositiveAmount(rawAmount);
        var idempotencyKey = StringUtils.hasText(explicitIdempotencyKey)
                ? explicitIdempotencyKey
                : buildIdempotencyKey(bizType, bizOrderNo, idempotencyAction);
        var existing = findByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            return existing;
        }

        var wallet = requireEnabledWallet(userId);
        var balanceChange = WalletBalanceCalculator.calculate(wallet, amount, action);
        var now = DateTimeUtils.now();
        var updateWrapper = new LambdaUpdateWrapper<UserWallet>()
                .eq(UserWallet::getId, wallet.getId())
                .eq(UserWallet::getVersionNo, wallet.getVersionNo())
                .set(UserWallet::getAvailableBalance, balanceChange.afterAvailableBalance())
                .set(UserWallet::getFrozenBalance, balanceChange.afterFrozenBalance())
                .set(UserWallet::getVersionNo, wallet.getVersionNo() + 1)
                .set(UserWallet::getUpdatedAt, now);
        if (action == WalletChangeAction.IN && bizType == WalletBusinessType.RECHARGE) {
            updateWrapper.set(UserWallet::getTotalRecharge, wallet.getTotalRecharge().add(amount));
        }
        if (action == WalletChangeAction.IN && bizType == WalletBusinessType.RENT_PROFIT) {
            updateWrapper.set(UserWallet::getTotalProfit, wallet.getTotalProfit().add(amount));
        }
        if (action == WalletChangeAction.IN && bizType == WalletBusinessType.COMMISSION_PROFIT) {
            updateWrapper.set(UserWallet::getTotalCommission, wallet.getTotalCommission().add(amount));
        }
        if (action == WalletChangeAction.OUT_FROM_FROZEN && bizType == WalletBusinessType.WITHDRAW
                && totalWithdrawIncrease != null) {
            updateWrapper.set(UserWallet::getTotalWithdraw, wallet.getTotalWithdraw().add(MoneyUtils.scale(totalWithdrawIncrease)));
        }
        var updated = userWalletMapper.update(null, updateWrapper);
        if (updated == 0) {
            handleUpdateFailure(wallet.getId(), amount, action);
        }

        var transaction = buildTransaction(
                wallet,
                amount,
                txType,
                bizType,
                bizOrderNo,
                remark,
                idempotencyKey,
                balanceChange,
                now
        );
        try {
            walletTransactionMapper.insert(transaction);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.WALLET_IDEMPOTENCY_KEY_DUPLICATE);
        }
        return transaction;
    }

    private void handleUpdateFailure(Long walletId, BigDecimal amount, WalletChangeAction action) {
        var latest = userWalletMapper.selectById(walletId);
        if (latest == null) {
            throw new BusinessException(ErrorCode.WALLET_NOT_FOUND);
        }
        if (!Integer.valueOf(CommonStatus.ENABLED.value()).equals(latest.getStatus())) {
            throw new BusinessException(ErrorCode.WALLET_DISABLED);
        }
        if (isBalanceInsufficient(latest, amount, action)) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        throw new BusinessException(ErrorCode.CONCURRENT_UPDATE_FAILED);
    }

    private boolean isBalanceInsufficient(UserWallet wallet, BigDecimal amount, WalletChangeAction action) {
        return switch (action) {
            case OUT, FREEZE -> wallet.getAvailableBalance().compareTo(amount) < 0;
            case UNFREEZE, OUT_FROM_FROZEN -> wallet.getFrozenBalance().compareTo(amount) < 0;
            case IN -> false;
        };
    }

    private UserWallet requireEnabledWallet(Long userId) {
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

    private WalletTransaction findByIdempotencyKey(String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey)) {
            return null;
        }
        return walletTransactionMapper.selectOne(new LambdaQueryWrapper<WalletTransaction>()
                .eq(WalletTransaction::getIdempotencyKey, idempotencyKey)
                .last("LIMIT 1"));
    }

    private String buildIdempotencyKey(WalletBusinessType bizType, String bizOrderNo, String action) {
        if (bizType == null || !StringUtils.hasText(bizOrderNo)) {
            throw new BusinessException(ErrorCode.WALLET_BUSINESS_REFERENCE_REQUIRED);
        }
        return bizType.name() + ":" + bizOrderNo.trim() + ":" + action;
    }

    private WalletTransaction buildTransaction(
            UserWallet wallet,
            BigDecimal amount,
            WalletTransactionType txType,
            WalletBusinessType bizType,
            String bizOrderNo,
            String remark,
            String idempotencyKey,
            WalletBalanceChange balanceChange,
            java.time.LocalDateTime now
    ) {
        var transaction = new WalletTransaction();
        transaction.setTxNo(generateTxNo());
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setUserId(wallet.getUserId());
        transaction.setWalletId(wallet.getId());
        transaction.setCurrency(StringUtils.hasText(wallet.getCurrency()) ? wallet.getCurrency() : CURRENCY_USDT);
        transaction.setTxType(txType.name());
        transaction.setAmount(amount);
        transaction.setBeforeAvailableBalance(balanceChange.beforeAvailableBalance());
        transaction.setAfterAvailableBalance(balanceChange.afterAvailableBalance());
        transaction.setBeforeFrozenBalance(balanceChange.beforeFrozenBalance());
        transaction.setAfterFrozenBalance(balanceChange.afterFrozenBalance());
        transaction.setBizType(bizType.name());
        transaction.setBizOrderNo(bizOrderNo);
        transaction.setRemark(remark);
        transaction.setCreatedAt(now);
        return transaction;
    }

    private WalletTransactionResponse toTransactionResponse(WalletTransaction transaction) {
        return toTransactionResponse(transaction, userName(transaction.getUserId()));
    }

    private WalletTransactionResponse toTransactionResponse(WalletTransaction transaction, String userName) {
        return new WalletTransactionResponse(
                transaction.getTxNo(),
                userName,
                transaction.getTxType(),
                transaction.getAmount(),
                transaction.getBeforeAvailableBalance(),
                transaction.getAfterAvailableBalance(),
                transaction.getBeforeFrozenBalance(),
                transaction.getAfterFrozenBalance(),
                transaction.getBizType(),
                transaction.getBizOrderNo(),
                WalletRemarkUtils.toChinese(transaction.getRemark()),
                transaction.getCreatedAt()
        );
    }

    private String generateTxNo() {
        return "WT" + DateTimeUtils.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT);
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
}

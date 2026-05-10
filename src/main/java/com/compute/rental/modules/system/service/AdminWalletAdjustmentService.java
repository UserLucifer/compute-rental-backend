package com.compute.rental.modules.system.service;

import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.enums.WalletBusinessType;
import com.compute.rental.common.enums.WalletTransactionType;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.common.util.MoneyUtils;
import com.compute.rental.modules.system.dto.AdminWalletAdjustRequest;
import com.compute.rental.modules.system.dto.AdminWalletAdjustResponse;
import com.compute.rental.modules.user.mapper.AppUserMapper;
import com.compute.rental.modules.wallet.entity.WalletTransaction;
import com.compute.rental.modules.wallet.service.WalletService;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminWalletAdjustmentService {

    private static final String IDEMPOTENCY_PREFIX = "ADJUST:";

    private final AppUserMapper appUserMapper;
    private final WalletService walletService;

    public AdminWalletAdjustmentService(AppUserMapper appUserMapper, WalletService walletService) {
        this.appUserMapper = appUserMapper;
        this.walletService = walletService;
    }

    @Transactional
    public AdminWalletAdjustResponse adjust(Long userId, AdminWalletAdjustRequest request) {
        requireUser(userId);
        if (request.txType() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "调账方向不能为空");
        }
        var amount = MoneyUtils.scale(request.amount());
        if (amount.signum() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT, "调账金额必须大于 0");
        }
        var adjustNo = trimToNull(request.adjustNo());
        if (!StringUtils.hasText(adjustNo)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "调账单号不能为空");
        }
        var reason = trimToNull(request.reason());
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "调账原因不能为空");
        }
        var idempotencyKey = IDEMPOTENCY_PREFIX + adjustNo;
        var transaction = switch (request.txType()) {
            case IN -> walletService.creditWithIdempotencyKey(userId, amount, WalletBusinessType.ADJUST, adjustNo,
                    idempotencyKey, reason);
            case OUT -> walletService.debitWithIdempotencyKey(userId, amount, WalletBusinessType.ADJUST, adjustNo,
                    idempotencyKey, reason);
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "调账方向仅支持 IN 或 OUT");
        };
        validateIdempotentTransaction(transaction, userId, request.txType(), amount, adjustNo);
        return toResponse(transaction);
    }

    private void requireUser(Long userId) {
        if (userId == null || appUserMapper.selectById(userId) == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private void validateIdempotentTransaction(
            WalletTransaction transaction,
            Long userId,
            WalletTransactionType txType,
            BigDecimal amount,
            String adjustNo
    ) {
        if (!userId.equals(transaction.getUserId())
                || !txType.name().equals(transaction.getTxType())
                || amount.compareTo(transaction.getAmount()) != 0
                || !WalletBusinessType.ADJUST.name().equals(transaction.getBizType())
                || !adjustNo.equals(transaction.getBizOrderNo())) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT, "调账单号已被其他调账使用");
        }
    }

    private AdminWalletAdjustResponse toResponse(WalletTransaction transaction) {
        return new AdminWalletAdjustResponse(
                transaction.getId(),
                transaction.getTxNo(),
                transaction.getUserId(),
                transaction.getWalletId(),
                transaction.getCurrency(),
                transaction.getTxType(),
                transaction.getAmount(),
                transaction.getBeforeAvailableBalance(),
                transaction.getAfterAvailableBalance(),
                transaction.getBeforeFrozenBalance(),
                transaction.getAfterFrozenBalance(),
                transaction.getBizType(),
                transaction.getBizOrderNo(),
                transaction.getRemark(),
                transaction.getCreatedAt()
        );
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}

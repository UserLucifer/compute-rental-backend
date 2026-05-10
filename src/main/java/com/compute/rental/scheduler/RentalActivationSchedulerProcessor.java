package com.compute.rental.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.compute.rental.common.enums.ApiTokenStatus;
import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.enums.ProfitStatus;
import com.compute.rental.common.enums.RentalOrderStatus;
import com.compute.rental.common.enums.RunSegmentCloseReason;
import com.compute.rental.common.enums.WalletBusinessType;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.common.util.DateTimeUtils;
import com.compute.rental.modules.order.entity.ApiCredential;
import com.compute.rental.modules.order.entity.RentalOrder;
import com.compute.rental.modules.order.mapper.ApiCredentialMapper;
import com.compute.rental.modules.order.mapper.RentalOrderMapper;
import com.compute.rental.modules.order.service.RentalOrderRunSegmentService;
import com.compute.rental.modules.wallet.service.WalletService;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RentalActivationSchedulerProcessor {

    private static final String DEPLOY_FEE_TIMEOUT_ACTION = "DEPLOY_FEE_TIMEOUT";

    private final RentalOrderMapper rentalOrderMapper;
    private final ApiCredentialMapper apiCredentialMapper;
    private final WalletService walletService;
    private final RentalOrderRunSegmentService runSegmentService;

    public RentalActivationSchedulerProcessor(
            RentalOrderMapper rentalOrderMapper,
            ApiCredentialMapper apiCredentialMapper,
            WalletService walletService,
            RentalOrderRunSegmentService runSegmentService
    ) {
        this.rentalOrderMapper = rentalOrderMapper;
        this.apiCredentialMapper = apiCredentialMapper;
        this.walletService = walletService;
        this.runSegmentService = runSegmentService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelDeployFeeTimeout(Long orderId, LocalDateTime cutoffTime) {
        var order = rentalOrderMapper.selectById(orderId);
        if (order == null || !RentalOrderStatus.PENDING_ACTIVATION.name().equals(order.getOrderStatus())
                || order.getApiGeneratedAt() == null || order.getApiGeneratedAt().isAfter(cutoffTime)) {
            return;
        }
        var now = DateTimeUtils.now();
        var updated = rentalOrderMapper.update(null, new LambdaUpdateWrapper<RentalOrder>()
                .eq(RentalOrder::getId, order.getId())
                .eq(RentalOrder::getOrderStatus, RentalOrderStatus.PENDING_ACTIVATION.name())
                .set(RentalOrder::getOrderStatus, RentalOrderStatus.CANCELED.name())
                .set(RentalOrder::getCanceledAt, now)
                .set(RentalOrder::getUpdatedAt, now));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CONCURRENT_UPDATE_FAILED, "租赁订单状态已变化");
        }
        walletService.credit(
                order.getUserId(),
                order.getOrderAmount(),
                WalletBusinessType.REFUND,
                order.getOrderNo(),
                DEPLOY_FEE_TIMEOUT_ACTION,
                "部署费超时未支付退款"
        );
        apiCredentialMapper.update(null, new LambdaUpdateWrapper<ApiCredential>()
                .eq(ApiCredential::getRentalOrderId, order.getId())
                .ne(ApiCredential::getTokenStatus, ApiTokenStatus.REVOKED.name())
                .set(ApiCredential::getTokenStatus, ApiTokenStatus.REVOKED.name())
                .set(ApiCredential::getRevokedAt, now)
                .set(ApiCredential::getUpdatedAt, now));
        // TODO: create sys_notification ORDER_CANCELED after notification service is implemented.
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void autoPause(Long orderId, LocalDateTime now) {
        var order = rentalOrderMapper.selectById(orderId);
        if (order == null || !isAutoPauseCandidate(order.getOrderStatus())
                || order.getAutoPauseAt() == null || order.getAutoPauseAt().isAfter(now)) {
            return;
        }
        var credential = apiCredentialMapper.selectOne(new LambdaQueryWrapper<ApiCredential>()
                .eq(ApiCredential::getRentalOrderId, order.getId())
                .last("LIMIT 1"));
        if (credential == null || !isAutoPauseCredential(credential.getTokenStatus())) {
            throw new BusinessException(ErrorCode.API_CREDENTIAL_NOT_ACTIVE);
        }
        var updatedOrder = rentalOrderMapper.update(null, new LambdaUpdateWrapper<RentalOrder>()
                .eq(RentalOrder::getId, order.getId())
                .in(RentalOrder::getOrderStatus, RentalOrderStatus.RUNNING.name(), RentalOrderStatus.ACTIVATING.name())
                .set(RentalOrder::getOrderStatus, RentalOrderStatus.PAUSED.name())
                .set(RentalOrder::getProfitStatus, ProfitStatus.PAUSED.name())
                .set(RentalOrder::getPausedAt, now)
                .set(RentalOrder::getUpdatedAt, now));
        if (updatedOrder == 0) {
            throw new BusinessException(ErrorCode.CONCURRENT_UPDATE_FAILED, "租赁订单状态已变化");
        }
        if (RentalOrderStatus.RUNNING.name().equals(order.getOrderStatus())) {
            runSegmentService.closeOpenSegment(order.getId(), now, RunSegmentCloseReason.AUTO_PAUSE);
        }
        var updatedCredential = apiCredentialMapper.update(null, new LambdaUpdateWrapper<ApiCredential>()
                .eq(ApiCredential::getId, credential.getId())
                .in(ApiCredential::getTokenStatus, ApiTokenStatus.ACTIVE.name(), ApiTokenStatus.ACTIVATING.name())
                .set(ApiCredential::getTokenStatus, ApiTokenStatus.PAUSED.name())
                .set(ApiCredential::getPausedAt, now)
                .set(ApiCredential::getUpdatedAt, now));
        if (updatedCredential == 0) {
            throw new BusinessException(ErrorCode.API_CREDENTIAL_STATUS_CHANGED);
        }
    }

    private boolean isAutoPauseCandidate(String orderStatus) {
        return RentalOrderStatus.RUNNING.name().equals(orderStatus)
                || RentalOrderStatus.ACTIVATING.name().equals(orderStatus);
    }

    private boolean isAutoPauseCredential(String tokenStatus) {
        return ApiTokenStatus.ACTIVE.name().equals(tokenStatus)
                || ApiTokenStatus.ACTIVATING.name().equals(tokenStatus);
    }
}

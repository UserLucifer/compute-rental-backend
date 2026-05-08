package com.compute.rental.modules.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.compute.rental.common.enums.ApiTokenStatus;
import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.enums.ProfitStatus;
import com.compute.rental.common.enums.RecordSettleStatus;
import com.compute.rental.common.enums.RentalOrderSettlementStatus;
import com.compute.rental.common.enums.RentalOrderStatus;
import com.compute.rental.common.enums.RentalSettlementOrderStatus;
import com.compute.rental.common.enums.RentalSettlementType;
import com.compute.rental.common.enums.WalletBusinessType;
import com.compute.rental.common.enums.WalletTransactionType;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.common.util.DateTimeUtils;
import com.compute.rental.common.util.MoneyUtils;
import com.compute.rental.modules.order.dto.SettlementOrderQueryRequest;
import com.compute.rental.modules.order.dto.SettlementOrderResponse;
import com.compute.rental.modules.order.entity.ApiCredential;
import com.compute.rental.modules.order.entity.RentalOrder;
import com.compute.rental.modules.order.entity.RentalProfitRecord;
import com.compute.rental.modules.order.entity.RentalSettlementOrder;
import com.compute.rental.modules.order.mapper.ApiCredentialMapper;
import com.compute.rental.modules.order.mapper.RentalOrderMapper;
import com.compute.rental.modules.order.mapper.RentalProfitRecordMapper;
import com.compute.rental.modules.order.mapper.RentalSettlementOrderMapper;
import com.compute.rental.modules.wallet.service.WalletService;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettlementService {

    private static final String CURRENCY_USDT = "USDT";

    private final RentalSettlementOrderMapper settlementOrderMapper;
    private final RentalOrderMapper rentalOrderMapper;
    private final RentalProfitRecordMapper profitRecordMapper;
    private final ApiCredentialMapper apiCredentialMapper;
    private final WalletService walletService;

    public SettlementService(
            RentalSettlementOrderMapper settlementOrderMapper,
            RentalOrderMapper rentalOrderMapper,
            RentalProfitRecordMapper profitRecordMapper,
            ApiCredentialMapper apiCredentialMapper,
            WalletService walletService
    ) {
        this.settlementOrderMapper = settlementOrderMapper;
        this.rentalOrderMapper = rentalOrderMapper;
        this.profitRecordMapper = profitRecordMapper;
        this.apiCredentialMapper = apiCredentialMapper;
        this.walletService = walletService;
    }

    public PageResult<SettlementOrderResponse> pageUserSettlementOrders(Long userId,
                                                                        SettlementOrderQueryRequest request) {
        var page = new Page<RentalSettlementOrder>(request.current(), request.size());
        var wrapper = new LambdaQueryWrapper<RentalSettlementOrder>()
                .eq(RentalSettlementOrder::getUserId, userId)
                .eq(request.settlementType() != null, RentalSettlementOrder::getSettlementType,
                        request.settlementType() == null ? null : request.settlementType().name())
                .eq(request.status() != null, RentalSettlementOrder::getStatus,
                        request.status() == null ? null : request.status().name())
                .ge(request.startTime() != null, RentalSettlementOrder::getCreatedAt, request.startTime())
                .le(request.endTime() != null, RentalSettlementOrder::getCreatedAt, request.endTime())
                .orderByDesc(RentalSettlementOrder::getId);
        var result = settlementOrderMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getRecords().stream().map(this::toResponse).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    public SettlementOrderResponse getUserSettlementOrder(Long userId, String settlementNo) {
        var settlement = settlementOrderMapper.selectOne(new LambdaQueryWrapper<RentalSettlementOrder>()
                .eq(RentalSettlementOrder::getUserId, userId)
                .eq(RentalSettlementOrder::getSettlementNo, settlementNo)
                .last("LIMIT 1"));
        if (settlement == null) {
            throw new BusinessException(ErrorCode.SETTLEMENT_ORDER_NOT_FOUND);
        }
        return toResponse(settlement);
    }

    @Transactional
    public SettlementOrderResponse settleEarly(Long userId, String orderNo) {
        var order = requireUserOrder(userId, orderNo);
        var existing = findSettlement(order.getId(), RentalSettlementType.EARLY_TERMINATE);
        if (existing != null) {
            return toResponse(existing);
        }
        if (!RentalOrderStatus.RUNNING.name().equals(order.getOrderStatus())
                && !RentalOrderStatus.PAUSED.name().equals(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.RENTAL_ORDER_NOT_SETTLEABLE);
        }
        var now = DateTimeUtils.now();
        var locked = rentalOrderMapper.update(null, new LambdaUpdateWrapper<RentalOrder>()
                .eq(RentalOrder::getId, order.getId())
                .in(RentalOrder::getOrderStatus, RentalOrderStatus.RUNNING.name(), RentalOrderStatus.PAUSED.name())
                .set(RentalOrder::getOrderStatus, RentalOrderStatus.SETTLING.name())
                .set(RentalOrder::getSettlementStatus, RentalOrderSettlementStatus.SETTLING.name())
                .set(RentalOrder::getUpdatedAt, now));
        if (locked == 0) {
            throw new BusinessException(ErrorCode.CONCURRENT_UPDATE_FAILED, "租赁订单状态已变化");
        }
        var profitAmount = settledProfitAmount(order.getId());
        var penaltyAmount = MoneyUtils.scale(order.getOrderAmount().multiply(order.getEarlyPenaltyRateSnapshot()));
        var actualSettleAmount = MoneyUtils.scale(order.getOrderAmount().subtract(penaltyAmount));
        var settlement = buildSettlement(order, RentalSettlementType.EARLY_TERMINATE, profitAmount,
                penaltyAmount, actualSettleAmount, now);
        settlementOrderMapper.insert(settlement);

        if (penaltyAmount.signum() > 0) {
            walletService.recordNoBalanceChange(
                    userId,
                    penaltyAmount,
                    WalletTransactionType.OUT,
                    WalletBusinessType.EARLY_PENALTY,
                    settlement.getSettlementNo(),
                    "EARLY_PENALTY:" + order.getOrderNo() + ":EARLY_TERMINATE",
                    "提前结算违约金从本金中扣除"
            );
        }
        var tx = walletService.creditWithIdempotencyKey(
                userId,
                actualSettleAmount,
                WalletBusinessType.SETTLEMENT,
                settlement.getSettlementNo(),
                "SETTLEMENT:" + order.getOrderNo() + ":EARLY_TERMINATE",
                "提前结算本金返还"
        );
        settlementOrderMapper.update(null, new LambdaUpdateWrapper<RentalSettlementOrder>()
                .eq(RentalSettlementOrder::getId, settlement.getId())
                .set(RentalSettlementOrder::getWalletTxNo, tx.getTxNo())
                .set(RentalSettlementOrder::getUpdatedAt, DateTimeUtils.now()));
        var updated = rentalOrderMapper.update(null, new LambdaUpdateWrapper<RentalOrder>()
                .eq(RentalOrder::getId, order.getId())
                .eq(RentalOrder::getOrderStatus, RentalOrderStatus.SETTLING.name())
                .set(RentalOrder::getOrderStatus, RentalOrderStatus.EARLY_CLOSED.name())
                .set(RentalOrder::getProfitStatus, ProfitStatus.FINISHED.name())
                .set(RentalOrder::getSettlementStatus, RentalOrderSettlementStatus.SETTLED.name())
                .set(RentalOrder::getFinishedAt, now)
                .set(RentalOrder::getUpdatedAt, now));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CONCURRENT_UPDATE_FAILED, "租赁订单状态已变化");
        }
        apiCredentialMapper.update(null, new LambdaUpdateWrapper<ApiCredential>()
                .eq(ApiCredential::getRentalOrderId, order.getId())
                .set(ApiCredential::getTokenStatus, ApiTokenStatus.REVOKED.name())
                .set(ApiCredential::getRevokedAt, now)
                .set(ApiCredential::getUpdatedAt, now));
        return toResponse(getSettlement(settlement.getSettlementNo()));
    }

    @Transactional
    public RentalSettlementOrder expireSettle(RentalOrder order) {
        var existing = findSettlement(order.getId(), RentalSettlementType.EXPIRE);
        if (existing != null) {
            return existing;
        }
        if (!RentalOrderStatus.RUNNING.name().equals(order.getOrderStatus())) {
            return null;
        }
        var now = DateTimeUtils.now();
        var locked = rentalOrderMapper.update(null, new LambdaUpdateWrapper<RentalOrder>()
                .eq(RentalOrder::getId, order.getId())
                .eq(RentalOrder::getOrderStatus, RentalOrderStatus.RUNNING.name())
                .set(RentalOrder::getOrderStatus, RentalOrderStatus.SETTLING.name())
                .set(RentalOrder::getSettlementStatus, RentalOrderSettlementStatus.SETTLING.name())
                .set(RentalOrder::getUpdatedAt, now));
        if (locked == 0) {
            throw new BusinessException(ErrorCode.CONCURRENT_UPDATE_FAILED, "租赁订单状态已变化");
        }
        var profitAmount = settledProfitAmount(order.getId());
        var settlement = buildSettlement(order, RentalSettlementType.EXPIRE, profitAmount,
                MoneyUtils.ZERO, MoneyUtils.scale(order.getOrderAmount()), now);
        settlementOrderMapper.insert(settlement);
        var tx = walletService.creditWithIdempotencyKey(
                order.getUserId(),
                settlement.getActualSettleAmount(),
                WalletBusinessType.SETTLEMENT,
                settlement.getSettlementNo(),
                "SETTLEMENT:" + order.getOrderNo() + ":EXPIRE",
                "租赁到期本金返还"
        );
        settlementOrderMapper.update(null, new LambdaUpdateWrapper<RentalSettlementOrder>()
                .eq(RentalSettlementOrder::getId, settlement.getId())
                .set(RentalSettlementOrder::getWalletTxNo, tx.getTxNo())
                .set(RentalSettlementOrder::getUpdatedAt, DateTimeUtils.now()));
        var updated = rentalOrderMapper.update(null, new LambdaUpdateWrapper<RentalOrder>()
                .eq(RentalOrder::getId, order.getId())
                .eq(RentalOrder::getOrderStatus, RentalOrderStatus.SETTLING.name())
                .set(RentalOrder::getOrderStatus, RentalOrderStatus.EXPIRED.name())
                .set(RentalOrder::getProfitStatus, ProfitStatus.FINISHED.name())
                .set(RentalOrder::getSettlementStatus, RentalOrderSettlementStatus.SETTLED.name())
                .set(RentalOrder::getExpiredAt, now)
                .set(RentalOrder::getFinishedAt, now)
                .set(RentalOrder::getUpdatedAt, now));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CONCURRENT_UPDATE_FAILED, "租赁订单状态已变化");
        }
        apiCredentialMapper.update(null, new LambdaUpdateWrapper<ApiCredential>()
                .eq(ApiCredential::getRentalOrderId, order.getId())
                .set(ApiCredential::getTokenStatus, ApiTokenStatus.EXPIRED.name())
                .set(ApiCredential::getExpiredAt, now)
                .set(ApiCredential::getUpdatedAt, now));
        // TODO: create sys_notification ORDER_EXPIRED after notification service is implemented.
        return getSettlement(settlement.getSettlementNo());
    }

    private RentalSettlementOrder buildSettlement(
            RentalOrder order,
            RentalSettlementType settlementType,
            BigDecimal profitAmount,
            BigDecimal penaltyAmount,
            BigDecimal actualSettleAmount,
            java.time.LocalDateTime now
    ) {
        var settlement = new RentalSettlementOrder();
        settlement.setSettlementNo(generateSettlementNo());
        settlement.setUserId(order.getUserId());
        settlement.setRentalOrderId(order.getId());
        settlement.setSettlementType(settlementType.name());
        settlement.setCurrency(CURRENCY_USDT);
        settlement.setPrincipalAmount(MoneyUtils.scale(order.getOrderAmount()));
        settlement.setProfitAmount(MoneyUtils.scale(profitAmount));
        settlement.setPenaltyAmount(MoneyUtils.scale(penaltyAmount));
        settlement.setActualSettleAmount(MoneyUtils.scale(actualSettleAmount));
        settlement.setStatus(RentalSettlementOrderStatus.SETTLED.name());
        settlement.setSettledAt(now);
        settlement.setCreatedAt(now);
        settlement.setUpdatedAt(now);
        return settlement;
    }

    private BigDecimal settledProfitAmount(Long orderId) {
        return MoneyUtils.scale(profitRecordMapper.selectList(new LambdaQueryWrapper<RentalProfitRecord>()
                        .eq(RentalProfitRecord::getRentalOrderId, orderId)
                        .eq(RentalProfitRecord::getStatus, RecordSettleStatus.SETTLED.name()))
                .stream()
                .map(RentalProfitRecord::getFinalProfitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private RentalSettlementOrder findSettlement(Long orderId, RentalSettlementType settlementType) {
        return settlementOrderMapper.selectOne(new LambdaQueryWrapper<RentalSettlementOrder>()
                .eq(RentalSettlementOrder::getRentalOrderId, orderId)
                .eq(RentalSettlementOrder::getSettlementType, settlementType.name())
                .last("LIMIT 1"));
    }

    private RentalSettlementOrder getSettlement(String settlementNo) {
        return settlementOrderMapper.selectOne(new LambdaQueryWrapper<RentalSettlementOrder>()
                .eq(RentalSettlementOrder::getSettlementNo, settlementNo)
                .last("LIMIT 1"));
    }

    private RentalOrder requireUserOrder(Long userId, String orderNo) {
        var order = rentalOrderMapper.selectOne(new LambdaQueryWrapper<RentalOrder>()
                .eq(RentalOrder::getUserId, userId)
                .eq(RentalOrder::getOrderNo, orderNo)
                .last("LIMIT 1"));
        if (order == null) {
            throw new BusinessException(ErrorCode.RENTAL_ORDER_NOT_FOUND);
        }
        return order;
    }

    private SettlementOrderResponse toResponse(RentalSettlementOrder settlement) {
        var order = rentalOrderMapper.selectById(settlement.getRentalOrderId());
        return new SettlementOrderResponse(
                settlement.getSettlementNo(),
                order == null ? null : order.getOrderNo(),
                settlement.getSettlementType(),
                settlement.getCurrency(),
                settlement.getPrincipalAmount(),
                settlement.getProfitAmount(),
                settlement.getPenaltyAmount(),
                settlement.getActualSettleAmount(),
                settlement.getStatus(),
                settlement.getReviewedBy(),
                settlement.getReviewedAt(),
                settlement.getSettledAt(),
                settlement.getWalletTxNo(),
                settlement.getRemark(),
                settlement.getCreatedAt()
        );
    }

    private String generateSettlementNo() {
        return "ST" + DateTimeUtils.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }
}

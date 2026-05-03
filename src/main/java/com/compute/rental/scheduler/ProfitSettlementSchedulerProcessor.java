package com.compute.rental.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.compute.rental.common.enums.RecordSettleStatus;
import com.compute.rental.common.enums.RentalOrderStatus;
import com.compute.rental.common.enums.WalletBusinessType;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.common.util.DateTimeUtils;
import com.compute.rental.common.util.MoneyUtils;
import com.compute.rental.modules.order.entity.RentalOrder;
import com.compute.rental.modules.order.entity.RentalProfitRecord;
import com.compute.rental.modules.order.mapper.RentalOrderMapper;
import com.compute.rental.modules.order.mapper.RentalProfitRecordMapper;
import com.compute.rental.modules.order.service.SettlementService;
import com.compute.rental.modules.wallet.service.WalletService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfitSettlementSchedulerProcessor {

    private final RentalOrderMapper rentalOrderMapper;
    private final RentalProfitRecordMapper profitRecordMapper;
    private final WalletService walletService;
    private final SettlementService settlementService;

    public ProfitSettlementSchedulerProcessor(
            RentalOrderMapper rentalOrderMapper,
            RentalProfitRecordMapper profitRecordMapper,
            WalletService walletService,
            SettlementService settlementService
    ) {
        this.rentalOrderMapper = rentalOrderMapper;
        this.profitRecordMapper = profitRecordMapper;
        this.walletService = walletService;
        this.settlementService = settlementService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateDailyProfit(Long orderId, LocalDate profitDate, LocalDateTime now) {
        var order = rentalOrderMapper.selectById(orderId);
        if (!isDailyProfitTarget(order, profitDate, now)) {
            return;
        }
        var existing = profitRecordMapper.selectOne(new LambdaQueryWrapper<RentalProfitRecord>()
                .eq(RentalProfitRecord::getRentalOrderId, orderId)
                .eq(RentalProfitRecord::getProfitDate, profitDate)
                .last("LIMIT 1"));
        if (existing != null) {
            return;
        }
        var baseProfit = MoneyUtils.scale(BigDecimal.valueOf(order.getTokenOutputPerDaySnapshot())
                .multiply(order.getTokenUnitPriceSnapshot()));
        var finalProfit = MoneyUtils.scale(baseProfit.multiply(order.getYieldMultiplierSnapshot()));
        var record = new RentalProfitRecord();
        record.setProfitNo(generateProfitNo());
        record.setUserId(order.getUserId());
        record.setRentalOrderId(order.getId());
        record.setProfitDate(profitDate);
        record.setGpuDailyTokenSnapshot(order.getTokenOutputPerDaySnapshot());
        record.setTokenPriceSnapshot(order.getTokenUnitPriceSnapshot());
        record.setYieldMultiplierSnapshot(order.getYieldMultiplierSnapshot());
        record.setBaseProfitAmount(baseProfit);
        record.setFinalProfitAmount(finalProfit);
        record.setStatus(RecordSettleStatus.PENDING.name());
        record.setCommissionGenerated(0);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        try {
            profitRecordMapper.insert(record);
        } catch (DuplicateKeyException ex) {
            return;
        }
        var tx = walletService.creditWithIdempotencyKey(
                order.getUserId(),
                finalProfit,
                WalletBusinessType.RENT_PROFIT,
                record.getProfitNo(),
                "RENT_PROFIT:" + order.getOrderNo() + ":" + profitDate,
                "Daily rental profit"
        );
        profitRecordMapper.update(null, new LambdaUpdateWrapper<RentalProfitRecord>()
                .eq(RentalProfitRecord::getId, record.getId())
                .eq(RentalProfitRecord::getStatus, RecordSettleStatus.PENDING.name())
                .set(RentalProfitRecord::getStatus, RecordSettleStatus.SETTLED.name())
                .set(RentalProfitRecord::getWalletTxNo, tx.getTxNo())
                .set(RentalProfitRecord::getSettledAt, DateTimeUtils.now())
                .set(RentalProfitRecord::getUpdatedAt, DateTimeUtils.now()));
        // TODO: trigger commission generation in the next phase. commission_generated remains 0.
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expireSettle(Long orderId, LocalDateTime now) {
        var order = rentalOrderMapper.selectById(orderId);
        if (order == null || !RentalOrderStatus.RUNNING.name().equals(order.getOrderStatus())
                || order.getProfitEndAt() == null || order.getProfitEndAt().isAfter(now)) {
            return;
        }
        settlementService.expireSettle(order);
    }

    private boolean isDailyProfitTarget(RentalOrder order, LocalDate profitDate, LocalDateTime now) {
        return order != null
                && RentalOrderStatus.RUNNING.name().equals(order.getOrderStatus())
                && order.getProfitStartAt() != null
                && !order.getProfitStartAt().isAfter(now)
                && order.getProfitEndAt() != null
                && profitDate.isBefore(order.getProfitEndAt().toLocalDate());
    }

    private String generateProfitNo() {
        return "PF" + DateTimeUtils.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }
}

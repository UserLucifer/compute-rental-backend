package com.compute.rental.modules.scheduler.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.compute.rental.common.enums.RecordSettleStatus;
import com.compute.rental.common.enums.WalletBusinessType;
import com.compute.rental.common.util.DateTimeUtils;
import com.compute.rental.common.util.MoneyUtils;
import com.compute.rental.modules.order.entity.RentalOrder;
import com.compute.rental.modules.order.entity.RentalOrderRunSegment;
import com.compute.rental.modules.order.mapper.RentalOrderRunSegmentMapper;
import com.compute.rental.modules.order.entity.RentalProfitRecord;
import com.compute.rental.modules.order.mapper.RentalProfitRecordMapper;
import com.compute.rental.modules.wallet.service.WalletService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RentalProfitGenerateService {

    private static final long MINUTES_PER_DAY = 24L * 60L;

    private final RentalProfitRecordMapper profitRecordMapper;
    private final RentalOrderRunSegmentMapper runSegmentMapper;
    private final WalletService walletService;

    @Transactional
    public void generateProfitForDate(RentalOrder order, LocalDate profitDate, LocalDateTime now) {
        if (order == null || order.getId() == null || profitDate == null || order.getProfitStartAt() == null
                || order.getProfitEndAt() == null) {
            return;
        }
        if (existsProfitRecord(order.getId(), profitDate)) {
            return;
        }

        var windowStart = profitDate.atStartOfDay();
        var windowEnd = profitDate.plusDays(1).atStartOfDay();
        var orderStart = order.getProfitStartAt();
        var orderEnd = order.getProfitEndAt();
        var effectiveWindowStart = max(windowStart, orderStart);
        var effectiveWindowEnd = min(windowEnd, orderEnd);
        if (!effectiveWindowEnd.isAfter(effectiveWindowStart)) {
            return;
        }

        var segments = runSegmentMapper.selectList(new LambdaQueryWrapper<RentalOrderRunSegment>()
                .eq(RentalOrderRunSegment::getRentalOrderId, order.getId())
                .lt(RentalOrderRunSegment::getSegmentStartAt, effectiveWindowEnd)
                .and(wrapper -> wrapper.isNull(RentalOrderRunSegment::getSegmentEndAt)
                        .or()
                        .gt(RentalOrderRunSegment::getSegmentEndAt, effectiveWindowStart))
                .orderByAsc(RentalOrderRunSegment::getSegmentStartAt));

        long effectiveMinutes = 0L;
        LocalDateTime periodStartAt = null;
        LocalDateTime periodEndAt = null;
        for (var segment : segments) {
            var segmentStart = max(segment.getSegmentStartAt(), effectiveWindowStart);
            var segmentEnd = segment.getSegmentEndAt();
            if (segmentEnd == null) {
                segmentEnd = min(now, effectiveWindowEnd);
            } else {
                segmentEnd = min(segmentEnd, effectiveWindowEnd);
            }
            if (!segmentEnd.isAfter(segmentStart)) {
                continue;
            }
            var minutes = Duration.between(segmentStart, segmentEnd).toMinutes();
            if (minutes <= 0L) {
                continue;
            }
            effectiveMinutes += minutes;
            periodStartAt = periodStartAt == null || segmentStart.isBefore(periodStartAt) ? segmentStart : periodStartAt;
            periodEndAt = periodEndAt == null || segmentEnd.isAfter(periodEndAt) ? segmentEnd : periodEndAt;
        }

        effectiveMinutes = Math.min(effectiveMinutes, MINUTES_PER_DAY);
        if (effectiveMinutes <= 0L || periodStartAt == null || periodEndAt == null) {
            return;
        }

        var tokenOutputPerDay = order.getTokenOutputPerDaySnapshot() == null ? 0L : order.getTokenOutputPerDaySnapshot();
        var tokenPrice = defaultZero(order.getTokenUnitPriceSnapshot());
        var profitRate = defaultOne(order.getYieldMultiplierSnapshot());
        var dailyBaseProfit = MoneyUtils.scale(BigDecimal.valueOf(tokenOutputPerDay).multiply(tokenPrice));
        var baseProfit = scaleByMinutes(dailyBaseProfit, effectiveMinutes);
        var finalProfit = scaleByMinutes(dailyBaseProfit.multiply(profitRate), effectiveMinutes);
        if (finalProfit.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        var record = new RentalProfitRecord();
        record.setRentalOrderId(order.getId());
        record.setProfitNo(generateProfitNo());
        record.setUserId(order.getUserId());
        record.setProfitDate(profitDate);
        record.setGpuDailyTokenSnapshot(tokenOutputPerDay);
        record.setTokenPriceSnapshot(tokenPrice);
        record.setBaseProfitAmount(baseProfit);
        record.setYieldMultiplierSnapshot(profitRate);
        record.setFinalProfitAmount(finalProfit);
        record.setEffectiveMinutes((int) effectiveMinutes);
        record.setPeriodStartAt(periodStartAt);
        record.setPeriodEndAt(periodEndAt);
        record.setStatus(RecordSettleStatus.PENDING.name());
        record.setCommissionGenerated(0);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        try {
            profitRecordMapper.insert(record);
        } catch (DuplicateKeyException ex) {
            return;
        }

        var idempotentKey = "RENT_PROFIT:" + order.getOrderNo() + ":" + profitDate;
        var tx = walletService.creditWithIdempotencyKey(order.getUserId(), finalProfit,
                WalletBusinessType.RENT_PROFIT, record.getProfitNo(), idempotentKey, "每日租赁收益");

        profitRecordMapper.update(null, new LambdaUpdateWrapper<RentalProfitRecord>()
                .eq(RentalProfitRecord::getId, record.getId())
                .eq(RentalProfitRecord::getStatus, RecordSettleStatus.PENDING.name())
                .set(RentalProfitRecord::getStatus, RecordSettleStatus.SETTLED.name())
                .set(RentalProfitRecord::getWalletTxNo, tx.getTxNo())
                .set(RentalProfitRecord::getSettledAt, DateTimeUtils.now())
                .set(RentalProfitRecord::getUpdatedAt, DateTimeUtils.now()));
    }

    @Transactional
    public void generateProfitUpTo(RentalOrder order, LocalDateTime cutoffAt, LocalDateTime now) {
        if (order == null || order.getProfitStartAt() == null || cutoffAt == null
                || !cutoffAt.isAfter(order.getProfitStartAt())) {
            return;
        }
        var endDate = finalProfitDate(cutoffAt);
        var date = order.getProfitStartAt().toLocalDate();
        while (!date.isAfter(endDate)) {
            generateProfitForDate(order, date, now);
            date = date.plusDays(1);
        }
    }

    private boolean existsProfitRecord(Long orderId, LocalDate profitDate) {
        return profitRecordMapper.selectCount(new LambdaQueryWrapper<RentalProfitRecord>()
                .eq(RentalProfitRecord::getRentalOrderId, orderId)
                .eq(RentalProfitRecord::getProfitDate, profitDate)) > 0;
    }

    private LocalDate finalProfitDate(LocalDateTime cutoffAt) {
        if (cutoffAt.toLocalTime().equals(LocalTime.MIN)) {
            return cutoffAt.toLocalDate().minusDays(1);
        }
        return cutoffAt.toLocalDate();
    }

    private BigDecimal scaleByMinutes(BigDecimal dailyAmount, long minutes) {
        return MoneyUtils.scale(dailyAmount.multiply(BigDecimal.valueOf(minutes))
                .divide(BigDecimal.valueOf(MINUTES_PER_DAY), MoneyUtils.SCALE + 4, RoundingMode.HALF_DOWN));
    }

    private LocalDateTime max(LocalDateTime left, LocalDateTime right) {
        return left.isAfter(right) ? left : right;
    }

    private LocalDateTime min(LocalDateTime left, LocalDateTime right) {
        return left.isBefore(right) ? left : right;
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal defaultOne(BigDecimal value) {
        return value == null ? BigDecimal.ONE : value;
    }

    private String generateProfitNo() {
        return "PF" + DateTimeUtils.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }
}

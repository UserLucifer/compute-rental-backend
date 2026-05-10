package com.compute.rental.modules.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.enums.RecordSettleStatus;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.common.util.DateTimeUtils;
import com.compute.rental.common.util.MoneyUtils;
import com.compute.rental.modules.order.dto.ProfitRecordQueryRequest;
import com.compute.rental.modules.order.dto.ProfitRecordResponse;
import com.compute.rental.modules.order.dto.ProfitSummaryResponse;
import com.compute.rental.modules.order.dto.ProfitTrendGroupBy;
import com.compute.rental.modules.order.dto.ProfitTrendRecordResponse;
import com.compute.rental.modules.order.dto.ProfitTrendResponse;
import com.compute.rental.modules.order.entity.RentalOrder;
import com.compute.rental.modules.order.entity.RentalProfitRecord;
import com.compute.rental.modules.order.mapper.RentalOrderMapper;
import com.compute.rental.modules.order.mapper.RentalProfitRecordMapper;
import com.compute.rental.modules.wallet.entity.UserWallet;
import com.compute.rental.modules.wallet.mapper.UserWalletMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class ProfitService {

    private final RentalProfitRecordMapper rentalProfitRecordMapper;
    private final RentalOrderMapper rentalOrderMapper;
    private final UserWalletMapper userWalletMapper;

    public ProfitService(
            RentalProfitRecordMapper rentalProfitRecordMapper,
            RentalOrderMapper rentalOrderMapper,
            UserWalletMapper userWalletMapper
    ) {
        this.rentalProfitRecordMapper = rentalProfitRecordMapper;
        this.rentalOrderMapper = rentalOrderMapper;
        this.userWalletMapper = userWalletMapper;
    }

    public PageResult<ProfitRecordResponse> pageUserProfitRecords(Long userId, ProfitRecordQueryRequest request) {
        var orderId = resolveOrderId(userId, request);
        var page = new Page<RentalProfitRecord>(request.current(), request.size());
        var wrapper = new LambdaQueryWrapper<RentalProfitRecord>()
                .eq(RentalProfitRecord::getUserId, userId)
                .eq(orderId != null, RentalProfitRecord::getRentalOrderId, orderId)
                .eq(request.profitDate() != null, RentalProfitRecord::getProfitDate, request.profitDate())
                .eq(request.status() != null, RentalProfitRecord::getStatus,
                        request.status() == null ? null : request.status().name())
                .orderByDesc(RentalProfitRecord::getProfitDate)
                .orderByDesc(RentalProfitRecord::getId);
        var result = rentalProfitRecordMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getRecords().stream().map(this::toResponse).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    public PageResult<ProfitRecordResponse> pageOrderProfitRecords(Long userId, String orderNo,
                                                                   ProfitRecordQueryRequest request) {
        var order = requireUserOrder(userId, orderNo);
        var page = new Page<RentalProfitRecord>(request.current(), request.size());
        var wrapper = new LambdaQueryWrapper<RentalProfitRecord>()
                .eq(RentalProfitRecord::getUserId, userId)
                .eq(RentalProfitRecord::getRentalOrderId, order.getId())
                .orderByDesc(RentalProfitRecord::getProfitDate)
                .orderByDesc(RentalProfitRecord::getId);
        var result = rentalProfitRecordMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getRecords().stream().map(record -> toResponse(record, order)).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    public ProfitSummaryResponse summary(Long userId) {
        var today = DateTimeUtils.today();
        var yesterday = today.minusDays(1);
        var monthStart = today.withDayOfMonth(1);
        var aggregate = rentalProfitRecordMapper.userSummaryAggregate(
                userId,
                RecordSettleStatus.SETTLED.name(),
                today,
                yesterday,
                monthStart);
        return new ProfitSummaryResponse(
                walletTotalProfit(userId),
                MoneyUtils.scale(aggregate == null ? null : aggregate.getTodayProfit()),
                MoneyUtils.scale(aggregate == null ? null : aggregate.getYesterdayProfit()),
                MoneyUtils.scale(aggregate == null ? null : aggregate.getCurrentMonthProfit()),
                aggregate == null || aggregate.getSettledProfitCount() == null ? 0L : aggregate.getSettledProfitCount()
        );
    }

    public ProfitTrendResponse trend(Long userId, LocalDate startDate, LocalDate endDate, ProfitTrendGroupBy groupBy) {
        if (startDate == null || endDate == null || groupBy == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "startDate、endDate、groupBy 不能为空");
        }
        if (startDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "startDate 不能晚于 endDate");
        }
        var rows = rentalProfitRecordMapper.userTrendByDate(
                userId,
                RecordSettleStatus.SETTLED.name(),
                startDate,
                endDate);
        return new ProfitTrendResponse(rows.stream()
                .map(row -> new ProfitTrendRecordResponse(
                        row.getProfitDate(),
                        MoneyUtils.scale(row.getFinalProfitAmount()),
                        row.getRecordCount() == null ? 0L : row.getRecordCount()))
                .toList());
    }

    private Long resolveOrderId(Long userId, ProfitRecordQueryRequest request) {
        if (request.orderNo() != null && !request.orderNo().isBlank()) {
            return requireUserOrder(userId, request.orderNo().trim()).getId();
        }
        if (request.rentalOrderId() != null) {
            var order = rentalOrderMapper.selectById(request.rentalOrderId());
            if (order == null || !userId.equals(order.getUserId())) {
                throw new BusinessException(ErrorCode.RENTAL_ORDER_NOT_FOUND);
            }
            return order.getId();
        }
        return null;
    }

    private BigDecimal walletTotalProfit(Long userId) {
        var wallet = userWalletMapper.selectOne(new LambdaQueryWrapper<UserWallet>()
                .eq(UserWallet::getUserId, userId)
                .last("LIMIT 1"));
        return wallet == null ? MoneyUtils.ZERO : MoneyUtils.scale(wallet.getTotalProfit());
    }

    private ProfitRecordResponse toResponse(RentalProfitRecord record) {
        return toResponse(record, rentalOrderMapper.selectById(record.getRentalOrderId()));
    }

    private ProfitRecordResponse toResponse(RentalProfitRecord record, RentalOrder order) {
        return new ProfitRecordResponse(
                record.getProfitNo(),
                order == null ? null : order.getOrderNo(),
                order == null ? null : order.getProductNameSnapshot(),
                order == null ? null : order.getAiModelNameSnapshot(),
                record.getProfitDate(),
                record.getEffectiveMinutes(),
                record.getPeriodStartAt(),
                record.getPeriodEndAt(),
                record.getGpuDailyTokenSnapshot(),
                record.getTokenPriceSnapshot(),
                record.getYieldMultiplierSnapshot(),
                record.getBaseProfitAmount(),
                record.getFinalProfitAmount(),
                record.getStatus(),
                record.getWalletTxNo(),
                record.getCommissionGenerated(),
                record.getSettledAt()
        );
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
}

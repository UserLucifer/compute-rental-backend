package com.compute.rental.scheduler;

import com.compute.rental.common.enums.RentalOrderStatus;
import com.compute.rental.modules.order.mapper.RentalOrderMapper;
import com.compute.rental.modules.order.service.SettlementService;
import com.compute.rental.modules.scheduler.service.RentalProfitGenerateService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfitSettlementSchedulerProcessor {

    private final RentalOrderMapper rentalOrderMapper;
    private final RentalProfitGenerateService profitGenerateService;
    private final SettlementService settlementService;

    public ProfitSettlementSchedulerProcessor(
            RentalOrderMapper rentalOrderMapper,
            RentalProfitGenerateService profitGenerateService,
            SettlementService settlementService
    ) {
        this.rentalOrderMapper = rentalOrderMapper;
        this.profitGenerateService = profitGenerateService;
        this.settlementService = settlementService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateDailyProfit(Long orderId, LocalDate profitDate, LocalDateTime now) {
        var order = rentalOrderMapper.selectById(orderId);
        if (order == null || profitDate == null || profitDate.plusDays(1).atStartOfDay().isAfter(now)) {
            return;
        }
        profitGenerateService.generateProfitForDate(order, profitDate, now);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expireSettle(Long orderId, LocalDateTime now) {
        var order = rentalOrderMapper.selectById(orderId);
        if (order == null || !isExpireSettlementTarget(order.getOrderStatus())
                || order.getProfitEndAt() == null || order.getProfitEndAt().isAfter(now)) {
            return;
        }
        settlementService.expireSettle(order);
    }

    private boolean isExpireSettlementTarget(String orderStatus) {
        return RentalOrderStatus.RUNNING.name().equals(orderStatus)
                || RentalOrderStatus.PAUSED.name().equals(orderStatus);
    }
}

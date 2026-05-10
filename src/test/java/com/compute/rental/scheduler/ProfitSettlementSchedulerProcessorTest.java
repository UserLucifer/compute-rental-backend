package com.compute.rental.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.compute.rental.common.enums.ProfitStatus;
import com.compute.rental.common.enums.RentalOrderSettlementStatus;
import com.compute.rental.common.enums.RentalOrderStatus;
import com.compute.rental.modules.order.entity.RentalOrder;
import com.compute.rental.modules.order.mapper.RentalOrderMapper;
import com.compute.rental.modules.order.service.SettlementService;
import com.compute.rental.modules.scheduler.service.RentalProfitGenerateService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfitSettlementSchedulerProcessorTest {

    @Mock
    private RentalOrderMapper rentalOrderMapper;

    @Mock
    private RentalProfitGenerateService profitGenerateService;

    @Mock
    private SettlementService settlementService;

    @InjectMocks
    private ProfitSettlementSchedulerProcessor processor;

    @Test
    void completedProfitDateShouldDelegateToProfitGenerateService() {
        var profitDate = LocalDate.of(2026, 5, 9);
        var now = LocalDateTime.of(2026, 5, 10, 0, 0);
        var order = order(RentalOrderStatus.PAUSED,
                LocalDateTime.of(2026, 5, 9, 17, 0),
                LocalDateTime.of(2026, 5, 16, 17, 0));
        when(rentalOrderMapper.selectById(1L)).thenReturn(order);

        processor.generateDailyProfit(1L, profitDate, now);

        verify(profitGenerateService).generateProfitForDate(order, profitDate, now);
    }

    @Test
    void unfinishedProfitDateShouldNotGenerateProfit() {
        var profitDate = LocalDate.of(2026, 5, 9);
        var now = LocalDateTime.of(2026, 5, 9, 23, 59);
        when(rentalOrderMapper.selectById(1L)).thenReturn(order(RentalOrderStatus.RUNNING,
                LocalDateTime.of(2026, 5, 9, 17, 0),
                LocalDateTime.of(2026, 5, 16, 17, 0)));

        processor.generateDailyProfit(1L, profitDate, now);

        verify(profitGenerateService, never()).generateProfitForDate(any(), any(), any());
    }

    @Test
    void dueRunningOrderShouldExpireSettle() {
        var now = LocalDateTime.of(2026, 5, 16, 17, 25);
        var order = order(RentalOrderStatus.RUNNING,
                LocalDateTime.of(2026, 5, 9, 17, 24, 55),
                LocalDateTime.of(2026, 5, 16, 17, 24, 55));
        when(rentalOrderMapper.selectById(1L)).thenReturn(order);

        processor.expireSettle(1L, now);

        verify(settlementService).expireSettle(order);
    }

    @Test
    void duePausedOrderShouldExpireSettle() {
        var now = LocalDateTime.of(2026, 5, 16, 17, 25);
        var order = order(RentalOrderStatus.PAUSED,
                LocalDateTime.of(2026, 5, 9, 17, 24, 55),
                LocalDateTime.of(2026, 5, 16, 17, 24, 55));
        when(rentalOrderMapper.selectById(1L)).thenReturn(order);

        processor.expireSettle(1L, now);

        verify(settlementService).expireSettle(order);
    }

    @Test
    void notDueOrderShouldNotExpireSettle() {
        var now = LocalDateTime.of(2026, 5, 16, 17, 24);
        when(rentalOrderMapper.selectById(1L)).thenReturn(order(RentalOrderStatus.RUNNING,
                LocalDateTime.of(2026, 5, 9, 17, 24, 55),
                LocalDateTime.of(2026, 5, 16, 17, 24, 55)));

        processor.expireSettle(1L, now);

        verify(settlementService, never()).expireSettle(any());
    }

    @Test
    void nonActiveOrderShouldNotExpireSettle() {
        var now = LocalDateTime.of(2026, 5, 16, 17, 25);
        when(rentalOrderMapper.selectById(1L)).thenReturn(order(RentalOrderStatus.CANCELED,
                LocalDateTime.of(2026, 5, 9, 17, 24, 55),
                LocalDateTime.of(2026, 5, 16, 17, 24, 55)));

        processor.expireSettle(1L, now);

        verify(settlementService, never()).expireSettle(any());
    }

    private RentalOrder order(RentalOrderStatus status, LocalDateTime startAt, LocalDateTime endAt) {
        var order = new RentalOrder();
        order.setId(1L);
        order.setOrderNo("RO001");
        order.setUserId(10L);
        order.setOrderStatus(status.name());
        order.setProfitStatus(ProfitStatus.RUNNING.name());
        order.setSettlementStatus(RentalOrderSettlementStatus.UNSETTLED.name());
        order.setProfitStartAt(startAt);
        order.setProfitEndAt(endAt);
        return order;
    }
}

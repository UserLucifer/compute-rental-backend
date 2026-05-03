package com.compute.rental.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.compute.rental.common.enums.ProfitStatus;
import com.compute.rental.common.enums.RentalOrderSettlementStatus;
import com.compute.rental.common.enums.RentalOrderStatus;
import com.compute.rental.common.enums.WalletBusinessType;
import com.compute.rental.modules.order.entity.RentalOrder;
import com.compute.rental.modules.order.entity.RentalProfitRecord;
import com.compute.rental.modules.order.mapper.RentalOrderMapper;
import com.compute.rental.modules.order.mapper.RentalProfitRecordMapper;
import com.compute.rental.modules.order.service.SettlementService;
import com.compute.rental.modules.wallet.entity.WalletTransaction;
import com.compute.rental.modules.wallet.service.WalletService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
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
    private RentalProfitRecordMapper profitRecordMapper;

    @Mock
    private WalletService walletService;

    @Mock
    private SettlementService settlementService;

    @InjectMocks
    private ProfitSettlementSchedulerProcessor processor;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        var configuration = new Configuration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), RentalOrder.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), RentalProfitRecord.class);
    }

    @Test
    void runningOrderShouldGenerateDailyProfit() {
        var today = LocalDate.now();
        var now = LocalDateTime.now();
        when(rentalOrderMapper.selectById(1L)).thenReturn(runningOrder(now.minusDays(1), now.plusDays(2)));
        when(profitRecordMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(profitRecordMapper.insert(any(RentalProfitRecord.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, RentalProfitRecord.class).setId(10L);
            return 1;
        });
        var tx = new WalletTransaction();
        tx.setTxNo("WT001");
        when(walletService.creditWithIdempotencyKey(eq(10L), any(BigDecimal.class), eq(WalletBusinessType.RENT_PROFIT),
                any(), eq("RENT_PROFIT:RO001:" + today), any())).thenReturn(tx);

        processor.generateDailyProfit(1L, today, now);

        verify(profitRecordMapper).insert(any(RentalProfitRecord.class));
        verify(walletService).creditWithIdempotencyKey(eq(10L), any(BigDecimal.class),
                eq(WalletBusinessType.RENT_PROFIT), any(), eq("RENT_PROFIT:RO001:" + today), any());
        verify(profitRecordMapper).update(any(), any(Wrapper.class));
    }

    @Test
    void nonRunningOrderShouldNotGenerateProfit() {
        var now = LocalDateTime.now();
        var order = runningOrder(now.minusDays(1), now.plusDays(2));
        order.setOrderStatus(RentalOrderStatus.PAUSED.name());
        when(rentalOrderMapper.selectById(1L)).thenReturn(order);

        processor.generateDailyProfit(1L, LocalDate.now(), now);

        verify(profitRecordMapper, never()).insert(any(RentalProfitRecord.class));
        verify(walletService, never()).creditWithIdempotencyKey(any(), any(), any(), any(), any(), any());
    }

    @Test
    void futureProfitStartShouldNotGenerateProfit() {
        var now = LocalDateTime.now();
        when(rentalOrderMapper.selectById(1L)).thenReturn(runningOrder(now.plusMinutes(1), now.plusDays(2)));

        processor.generateDailyProfit(1L, LocalDate.now(), now);

        verify(profitRecordMapper, never()).insert(any(RentalProfitRecord.class));
    }

    @Test
    void lastDayShouldNotGenerateDailyProfit() {
        var now = LocalDateTime.now();
        when(rentalOrderMapper.selectById(1L)).thenReturn(runningOrder(now.minusDays(1), now));

        processor.generateDailyProfit(1L, LocalDate.now(), now);

        verify(profitRecordMapper, never()).insert(any(RentalProfitRecord.class));
    }

    @Test
    void existingProfitRecordShouldNotCreditAgain() {
        var today = LocalDate.now();
        var now = LocalDateTime.now();
        when(rentalOrderMapper.selectById(1L)).thenReturn(runningOrder(now.minusDays(1), now.plusDays(2)));
        when(profitRecordMapper.selectOne(any(Wrapper.class))).thenReturn(new RentalProfitRecord());

        processor.generateDailyProfit(1L, today, now);

        verify(walletService, never()).creditWithIdempotencyKey(any(), any(), any(), any(), any(), any());
    }

    @Test
    void dueRunningOrderShouldExpireSettle() {
        var now = LocalDateTime.now();
        var order = runningOrder(now.minusDays(5), now.minusSeconds(1));
        when(rentalOrderMapper.selectById(1L)).thenReturn(order);

        processor.expireSettle(1L, now);

        verify(settlementService).expireSettle(order);
    }

    private RentalOrder runningOrder(LocalDateTime startAt, LocalDateTime endAt) {
        var order = new RentalOrder();
        order.setId(1L);
        order.setOrderNo("RO001");
        order.setUserId(10L);
        order.setOrderStatus(RentalOrderStatus.RUNNING.name());
        order.setProfitStatus(ProfitStatus.RUNNING.name());
        order.setSettlementStatus(RentalOrderSettlementStatus.UNSETTLED.name());
        order.setProfitStartAt(startAt);
        order.setProfitEndAt(endAt);
        order.setTokenOutputPerDaySnapshot(1000L);
        order.setTokenUnitPriceSnapshot(new BigDecimal("0.01000000"));
        order.setYieldMultiplierSnapshot(new BigDecimal("1.2000"));
        return order;
    }
}

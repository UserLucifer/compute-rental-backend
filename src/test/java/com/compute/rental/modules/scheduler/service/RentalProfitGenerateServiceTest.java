package com.compute.rental.modules.scheduler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.compute.rental.common.enums.WalletBusinessType;
import com.compute.rental.modules.order.entity.RentalOrder;
import com.compute.rental.modules.order.entity.RentalOrderRunSegment;
import com.compute.rental.modules.order.entity.RentalProfitRecord;
import com.compute.rental.modules.order.mapper.RentalOrderRunSegmentMapper;
import com.compute.rental.modules.order.mapper.RentalProfitRecordMapper;
import com.compute.rental.modules.wallet.entity.WalletTransaction;
import com.compute.rental.modules.wallet.service.WalletService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RentalProfitGenerateServiceTest {

    @Mock
    private RentalProfitRecordMapper profitRecordMapper;

    @Mock
    private RentalOrderRunSegmentMapper runSegmentMapper;

    @Mock
    private WalletService walletService;

    @InjectMocks
    private RentalProfitGenerateService service;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        var configuration = new Configuration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), RentalProfitRecord.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), RentalOrderRunSegment.class);
    }

    @Test
    void shouldGenerateProfitFromCompleteRunningMinutes() {
        var profitDate = LocalDate.of(2026, 5, 9);
        var now = LocalDateTime.of(2026, 5, 10, 0, 0);
        var order = order(LocalDateTime.of(2026, 5, 9, 17, 0),
                LocalDateTime.of(2026, 5, 16, 17, 0));
        when(profitRecordMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(runSegmentMapper.selectList(any(Wrapper.class))).thenReturn(List.of(segment(
                LocalDateTime.of(2026, 5, 9, 17, 0), null)));
        when(profitRecordMapper.insert(any(RentalProfitRecord.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, RentalProfitRecord.class).setId(10L);
            return 1;
        });
        var tx = new WalletTransaction();
        tx.setTxNo("WT001");
        when(walletService.creditWithIdempotencyKey(eq(10L), eq(new BigDecimal("3.50000000")),
                eq(WalletBusinessType.RENT_PROFIT), any(), eq("RENT_PROFIT:RO001:" + profitDate), any()))
                .thenReturn(tx);

        service.generateProfitForDate(order, profitDate, now);

        var recordCaptor = ArgumentCaptor.forClass(RentalProfitRecord.class);
        verify(profitRecordMapper).insert(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getEffectiveMinutes()).isEqualTo(420);
        assertThat(recordCaptor.getValue().getPeriodStartAt()).isEqualTo(LocalDateTime.of(2026, 5, 9, 17, 0));
        assertThat(recordCaptor.getValue().getPeriodEndAt()).isEqualTo(LocalDateTime.of(2026, 5, 10, 0, 0));
        assertThat(recordCaptor.getValue().getBaseProfitAmount()).isEqualByComparingTo("2.91666667");
        assertThat(recordCaptor.getValue().getFinalProfitAmount()).isEqualByComparingTo("3.50000000");
        verify(profitRecordMapper).update(any(), any(Wrapper.class));
    }

    @Test
    void subMinuteSegmentShouldNotGenerateProfit() {
        var profitDate = LocalDate.of(2026, 5, 9);
        var now = LocalDateTime.of(2026, 5, 10, 0, 0);
        var order = order(LocalDateTime.of(2026, 5, 9, 23, 59, 21),
                LocalDateTime.of(2026, 5, 10, 0, 0));
        when(profitRecordMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(runSegmentMapper.selectList(any(Wrapper.class))).thenReturn(List.of(segment(
                LocalDateTime.of(2026, 5, 9, 23, 59, 21),
                LocalDateTime.of(2026, 5, 10, 0, 0))));

        service.generateProfitForDate(order, profitDate, now);

        verify(profitRecordMapper, never()).insert(any(RentalProfitRecord.class));
        verify(walletService, never()).creditWithIdempotencyKey(any(), any(), any(), any(), any(), any());
    }

    private RentalOrder order(LocalDateTime startAt, LocalDateTime endAt) {
        var order = new RentalOrder();
        order.setId(1L);
        order.setOrderNo("RO001");
        order.setUserId(10L);
        order.setProfitStartAt(startAt);
        order.setProfitEndAt(endAt);
        order.setTokenOutputPerDaySnapshot(1000L);
        order.setTokenUnitPriceSnapshot(new BigDecimal("0.01000000"));
        order.setYieldMultiplierSnapshot(new BigDecimal("1.2000"));
        return order;
    }

    private RentalOrderRunSegment segment(LocalDateTime startAt, LocalDateTime endAt) {
        var segment = new RentalOrderRunSegment();
        segment.setId(1L);
        segment.setRentalOrderId(1L);
        segment.setUserId(10L);
        segment.setSegmentStartAt(startAt);
        segment.setSegmentEndAt(endAt);
        return segment;
    }
}

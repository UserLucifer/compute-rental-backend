package com.compute.rental.modules.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.compute.rental.common.enums.ApiTokenStatus;
import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.enums.ProfitStatus;
import com.compute.rental.common.enums.RentalOrderSettlementStatus;
import com.compute.rental.common.enums.RentalOrderStatus;
import com.compute.rental.common.enums.RentalSettlementType;
import com.compute.rental.common.enums.RunSegmentCloseReason;
import com.compute.rental.common.enums.WalletBusinessType;
import com.compute.rental.common.enums.WalletTransactionType;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.modules.order.entity.ApiCredential;
import com.compute.rental.modules.order.entity.RentalOrder;
import com.compute.rental.modules.order.entity.RentalProfitRecord;
import com.compute.rental.modules.order.entity.RentalSettlementOrder;
import com.compute.rental.modules.order.mapper.ApiCredentialMapper;
import com.compute.rental.modules.order.mapper.RentalOrderMapper;
import com.compute.rental.modules.order.mapper.RentalProfitRecordMapper;
import com.compute.rental.modules.order.mapper.RentalSettlementOrderMapper;
import com.compute.rental.modules.scheduler.service.RentalProfitGenerateService;
import com.compute.rental.modules.wallet.entity.WalletTransaction;
import com.compute.rental.modules.wallet.service.WalletService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    private RentalSettlementOrderMapper settlementOrderMapper;

    @Mock
    private RentalOrderMapper rentalOrderMapper;

    @Mock
    private RentalProfitRecordMapper profitRecordMapper;

    @Mock
    private ApiCredentialMapper apiCredentialMapper;

    @Mock
    private WalletService walletService;

    @Mock
    private RentalOrderRunSegmentService runSegmentService;

    @Mock
    private RentalProfitGenerateService profitGenerateService;

    @Captor
    private ArgumentCaptor<RentalSettlementOrder> settlementCaptor;

    @InjectMocks
    private SettlementService settlementService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        var configuration = new Configuration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), RentalSettlementOrder.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), RentalOrder.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), RentalProfitRecord.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), ApiCredential.class);
    }

    @Test
    void expireSettleShouldCreateSettlementAndReturnPrincipal() {
        var order = order(RentalOrderStatus.RUNNING);
        when(settlementOrderMapper.selectOne(any(Wrapper.class))).thenReturn(null, settlement(RentalSettlementType.EXPIRE));
        when(rentalOrderMapper.update(any(), any(Wrapper.class))).thenReturn(1);
        when(profitRecordMapper.selectList(any(Wrapper.class))).thenReturn(List.of(profit("12.00000000")));
        when(settlementOrderMapper.insert(any(RentalSettlementOrder.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, RentalSettlementOrder.class).setId(20L);
            return 1;
        });
        when(walletService.creditWithIdempotencyKey(eq(10L), any(BigDecimal.class), eq(WalletBusinessType.SETTLEMENT),
                any(), eq("SETTLEMENT:RO001:EXPIRE"), any())).thenReturn(tx());

        var response = settlementService.expireSettle(order);

        verify(settlementOrderMapper).insert(settlementCaptor.capture());
        assertThat(settlementCaptor.getValue().getSettlementType()).isEqualTo(RentalSettlementType.EXPIRE.name());
        assertThat(settlementCaptor.getValue().getActualSettleAmount()).isEqualByComparingTo("1000.00000000");
        verify(runSegmentService).closeOpenSegment(eq(1L), eq(order.getProfitEndAt()), eq(RunSegmentCloseReason.EXPIRE));
        verify(profitGenerateService).generateProfitUpTo(eq(order), eq(order.getProfitEndAt()), any());
        verify(walletService).creditWithIdempotencyKey(eq(10L), any(BigDecimal.class),
                eq(WalletBusinessType.SETTLEMENT), any(), eq("SETTLEMENT:RO001:EXPIRE"), any());
        verify(apiCredentialMapper).update(any(), any(Wrapper.class));
        assertThat(response.getSettlementType()).isEqualTo(RentalSettlementType.EXPIRE.name());
    }

    @Test
    void existingExpireSettlementShouldNotReturnPrincipalAgain() {
        var existing = settlement(RentalSettlementType.EXPIRE);
        when(settlementOrderMapper.selectOne(any(Wrapper.class))).thenReturn(existing);

        var response = settlementService.expireSettle(order(RentalOrderStatus.RUNNING));

        assertThat(response).isSameAs(existing);
        verify(walletService, never()).creditWithIdempotencyKey(any(), any(), any(), any(), any(), any());
    }

    @Test
    void settleEarlyShouldCalculatePenaltyAndRevokeCredential() {
        when(rentalOrderMapper.selectOne(any(Wrapper.class))).thenReturn(order(RentalOrderStatus.RUNNING));
        when(settlementOrderMapper.selectOne(any(Wrapper.class))).thenReturn(null, settlement(RentalSettlementType.EARLY_TERMINATE));
        when(rentalOrderMapper.update(any(), any(Wrapper.class))).thenReturn(1);
        when(profitRecordMapper.selectList(any(Wrapper.class))).thenReturn(List.of(profit("12.00000000")));
        when(settlementOrderMapper.insert(any(RentalSettlementOrder.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, RentalSettlementOrder.class).setId(30L);
            return 1;
        });
        when(walletService.recordNoBalanceChange(eq(10L), any(BigDecimal.class), eq(WalletTransactionType.OUT),
                eq(WalletBusinessType.EARLY_PENALTY), any(), eq("EARLY_PENALTY:RO001:EARLY_TERMINATE"), any()))
                .thenReturn(tx());
        when(walletService.creditWithIdempotencyKey(eq(10L), any(BigDecimal.class), eq(WalletBusinessType.SETTLEMENT),
                any(), eq("SETTLEMENT:RO001:EARLY_TERMINATE"), any())).thenReturn(tx());

        settlementService.settleEarly(10L, "RO001");

        verify(settlementOrderMapper).insert(settlementCaptor.capture());
        assertThat(settlementCaptor.getValue().getPenaltyAmount()).isEqualByComparingTo("10.00000000");
        assertThat(settlementCaptor.getValue().getActualSettleAmount()).isEqualByComparingTo("990.00000000");
        verify(runSegmentService).closeOpenSegment(eq(1L), any(), eq(RunSegmentCloseReason.EARLY_SETTLE));
        verify(profitGenerateService).generateProfitUpTo(any(RentalOrder.class), any(), any());
        verify(apiCredentialMapper).update(any(), any(Wrapper.class));
    }

    @Test
    void userCannotSettleOtherUsersOrder() {
        when(rentalOrderMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> settlementService.settleEarly(99L, "RO001"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RENTAL_ORDER_NOT_FOUND);
    }

    private RentalOrder order(RentalOrderStatus status) {
        var order = new RentalOrder();
        order.setId(1L);
        order.setOrderNo("RO001");
        order.setUserId(10L);
        order.setOrderStatus(status.name());
        order.setProfitStatus(ProfitStatus.RUNNING.name());
        order.setSettlementStatus(RentalOrderSettlementStatus.UNSETTLED.name());
        order.setOrderAmount(new BigDecimal("1000.00000000"));
        order.setEarlyPenaltyRateSnapshot(new BigDecimal("0.0100"));
        order.setProfitStartAt(LocalDateTime.now().minusDays(7));
        order.setProfitEndAt(LocalDateTime.now().minusMinutes(1));
        return order;
    }

    private RentalProfitRecord profit(String amount) {
        var profit = new RentalProfitRecord();
        profit.setFinalProfitAmount(new BigDecimal(amount));
        return profit;
    }

    private RentalSettlementOrder settlement(RentalSettlementType type) {
        var settlement = new RentalSettlementOrder();
        settlement.setId(2L);
        settlement.setSettlementNo("ST001");
        settlement.setUserId(10L);
        settlement.setRentalOrderId(1L);
        settlement.setSettlementType(type.name());
        settlement.setPrincipalAmount(new BigDecimal("1000.00000000"));
        settlement.setProfitAmount(new BigDecimal("12.00000000"));
        settlement.setPenaltyAmount(type == RentalSettlementType.EXPIRE ? BigDecimal.ZERO : new BigDecimal("10.00000000"));
        settlement.setActualSettleAmount(type == RentalSettlementType.EXPIRE
                ? new BigDecimal("1000.00000000") : new BigDecimal("990.00000000"));
        return settlement;
    }

    private WalletTransaction tx() {
        var tx = new WalletTransaction();
        tx.setTxNo("WT001");
        return tx;
    }
}

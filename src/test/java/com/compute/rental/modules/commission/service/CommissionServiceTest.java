package com.compute.rental.modules.commission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.compute.rental.common.enums.RecordSettleStatus;
import com.compute.rental.common.enums.WalletBusinessType;
import com.compute.rental.modules.commission.entity.CommissionRecord;
import com.compute.rental.modules.commission.entity.CommissionRule;
import com.compute.rental.modules.commission.mapper.CommissionRecordMapper;
import com.compute.rental.modules.commission.mapper.CommissionRuleMapper;
import com.compute.rental.modules.order.entity.RentalProfitRecord;
import com.compute.rental.modules.order.mapper.RentalProfitRecordMapper;
import com.compute.rental.modules.user.entity.UserReferralRelation;
import com.compute.rental.modules.user.mapper.UserReferralRelationMapper;
import com.compute.rental.modules.wallet.entity.UserWallet;
import com.compute.rental.modules.wallet.entity.WalletTransaction;
import com.compute.rental.modules.wallet.mapper.UserWalletMapper;
import com.compute.rental.modules.wallet.service.WalletService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
class CommissionServiceTest {

    @Mock
    private CommissionRuleMapper commissionRuleMapper;

    @Mock
    private CommissionRecordMapper commissionRecordMapper;

    @Mock
    private RentalProfitRecordMapper profitRecordMapper;

    @Mock
    private UserReferralRelationMapper referralRelationMapper;

    @Mock
    private UserWalletMapper userWalletMapper;

    @Mock
    private WalletService walletService;

    @Captor
    private ArgumentCaptor<CommissionRecord> commissionRecordCaptor;

    @InjectMocks
    private CommissionService commissionService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        var configuration = new Configuration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), CommissionRule.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), CommissionRecord.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), RentalProfitRecord.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), UserReferralRelation.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), UserWallet.class);
    }

    @Test
    void shouldGenerateThreeLevelCommissionsWithDefaultRates() {
        when(profitRecordMapper.selectById(1L)).thenReturn(settledProfit(1L, 100L, new BigDecimal("100.00000000")));
        when(referralRelationMapper.selectOne(any(Wrapper.class))).thenReturn(referral(11L, 12L, 13L));
        when(commissionRuleMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(commissionRecordMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(commissionRecordMapper.insert(any(CommissionRecord.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, CommissionRecord.class).setId(1000L);
            return 1;
        });
        var tx = new WalletTransaction();
        tx.setTxNo("WT001");
        when(walletService.creditWithIdempotencyKey(any(), any(), eq(WalletBusinessType.COMMISSION_PROFIT),
                any(), any(), any())).thenReturn(tx);

        commissionService.generateForProfit(1L);

        verify(commissionRecordMapper, org.mockito.Mockito.times(3)).insert(commissionRecordCaptor.capture());
        var recordsByLevel = commissionRecordCaptor.getAllValues().stream()
                .collect(Collectors.toMap(CommissionRecord::getLevelNo, record -> record));
        assertThat(recordsByLevel.get(1).getBenefitUserId()).isEqualTo(11L);
        assertThat(recordsByLevel.get(1).getCommissionRateSnapshot()).isEqualByComparingTo("0.2000");
        assertThat(recordsByLevel.get(1).getCommissionAmount()).isEqualByComparingTo("20.00000000");
        assertThat(recordsByLevel.get(2).getBenefitUserId()).isEqualTo(12L);
        assertThat(recordsByLevel.get(2).getCommissionRateSnapshot()).isEqualByComparingTo("0.1000");
        assertThat(recordsByLevel.get(2).getCommissionAmount()).isEqualByComparingTo("10.00000000");
        assertThat(recordsByLevel.get(3).getBenefitUserId()).isEqualTo(13L);
        assertThat(recordsByLevel.get(3).getCommissionRateSnapshot()).isEqualByComparingTo("0.0500");
        assertThat(recordsByLevel.get(3).getCommissionAmount()).isEqualByComparingTo("5.00000000");
        verify(walletService).creditWithIdempotencyKey(eq(11L), any(), eq(WalletBusinessType.COMMISSION_PROFIT),
                any(), eq("COMMISSION_PROFIT:1:1"), any());
        verify(walletService).creditWithIdempotencyKey(eq(12L), any(), eq(WalletBusinessType.COMMISSION_PROFIT),
                any(), eq("COMMISSION_PROFIT:1:2"), any());
        verify(walletService).creditWithIdempotencyKey(eq(13L), any(), eq(WalletBusinessType.COMMISSION_PROFIT),
                any(), eq("COMMISSION_PROFIT:1:3"), any());
        verify(profitRecordMapper).update(any(), any(Wrapper.class));
    }

    @Test
    void shouldUseEnabledRuleRatesAsSnapshots() {
        when(profitRecordMapper.selectById(1L)).thenReturn(settledProfit(1L, 100L, new BigDecimal("100.00000000")));
        when(referralRelationMapper.selectOne(any(Wrapper.class))).thenReturn(referral(11L, null, null));
        when(commissionRuleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(rule(1, "0.2500")));
        when(commissionRecordMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(commissionRecordMapper.insert(any(CommissionRecord.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, CommissionRecord.class).setId(1000L);
            return 1;
        });
        var tx = new WalletTransaction();
        tx.setTxNo("WT001");
        when(walletService.creditWithIdempotencyKey(any(), any(), eq(WalletBusinessType.COMMISSION_PROFIT),
                any(), any(), any())).thenReturn(tx);

        commissionService.generateForProfit(1L);

        verify(commissionRecordMapper).insert(commissionRecordCaptor.capture());
        assertThat(commissionRecordCaptor.getValue().getCommissionRateSnapshot()).isEqualByComparingTo("0.2500");
        assertThat(commissionRecordCaptor.getValue().getCommissionAmount()).isEqualByComparingTo("25.00000000");
    }

    @Test
    void shouldMarkGeneratedWithoutCommissionWhenNoUpperUser() {
        when(profitRecordMapper.selectById(1L)).thenReturn(settledProfit(1L, 100L, new BigDecimal("100.00000000")));
        when(referralRelationMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        commissionService.generateForProfit(1L);

        verify(commissionRecordMapper, never()).insert(any(CommissionRecord.class));
        verify(walletService, never()).creditWithIdempotencyKey(any(), any(), any(), any(), any(), any());
        verify(profitRecordMapper).update(any(), any(Wrapper.class));
    }

    @Test
    void shouldSkipAlreadyGeneratedProfit() {
        var profit = settledProfit(1L, 100L, new BigDecimal("100.00000000"));
        profit.setCommissionGenerated(1);
        when(profitRecordMapper.selectById(1L)).thenReturn(profit);

        commissionService.generateForProfit(1L);

        verify(referralRelationMapper, never()).selectOne(any(Wrapper.class));
        verify(walletService, never()).creditWithIdempotencyKey(any(), any(), any(), any(), any(), any());
    }

    private RentalProfitRecord settledProfit(Long id, Long userId, BigDecimal amount) {
        var profit = new RentalProfitRecord();
        profit.setId(id);
        profit.setUserId(userId);
        profit.setRentalOrderId(200L);
        profit.setFinalProfitAmount(amount);
        profit.setStatus(RecordSettleStatus.SETTLED.name());
        profit.setCommissionGenerated(0);
        return profit;
    }

    private UserReferralRelation referral(Long level1UserId, Long level2UserId, Long level3UserId) {
        var referral = new UserReferralRelation();
        referral.setUserId(100L);
        referral.setLevel1UserId(level1UserId);
        referral.setLevel2UserId(level2UserId);
        referral.setLevel3UserId(level3UserId);
        return referral;
    }

    private CommissionRule rule(Integer levelNo, String rate) {
        var rule = new CommissionRule();
        rule.setLevelNo(levelNo);
        rule.setCommissionRate(new BigDecimal(rate));
        return rule;
    }
}

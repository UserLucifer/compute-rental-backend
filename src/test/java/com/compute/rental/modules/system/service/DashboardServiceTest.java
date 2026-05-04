package com.compute.rental.modules.system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.compute.rental.common.enums.ProfitStatus;
import com.compute.rental.common.enums.RentalOrderStatus;
import com.compute.rental.common.query.StatusCountRow;
import com.compute.rental.modules.commission.entity.CommissionRecord;
import com.compute.rental.modules.commission.mapper.CommissionRecordMapper;
import com.compute.rental.modules.order.entity.RentalOrder;
import com.compute.rental.modules.order.entity.RentalProfitRecord;
import com.compute.rental.modules.order.mapper.RentalOrderMapper;
import com.compute.rental.modules.order.mapper.RentalProfitRecordMapper;
import com.compute.rental.modules.user.entity.AppUser;
import com.compute.rental.modules.user.entity.UserReferralRelation;
import com.compute.rental.modules.user.mapper.AppUserMapper;
import com.compute.rental.modules.user.mapper.UserReferralRelationMapper;
import com.compute.rental.modules.system.dto.DashboardStatusCountResponse;
import com.compute.rental.modules.wallet.entity.RechargeOrder;
import com.compute.rental.modules.wallet.entity.UserWallet;
import com.compute.rental.modules.wallet.entity.WithdrawOrder;
import com.compute.rental.modules.wallet.mapper.RechargeOrderMapper;
import com.compute.rental.modules.wallet.mapper.UserWalletMapper;
import com.compute.rental.modules.wallet.mapper.WithdrawOrderMapper;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private AppUserMapper appUserMapper;
    @Mock
    private UserReferralRelationMapper referralRelationMapper;
    @Mock
    private UserWalletMapper userWalletMapper;
    @Mock
    private RechargeOrderMapper rechargeOrderMapper;
    @Mock
    private WithdrawOrderMapper withdrawOrderMapper;
    @Mock
    private RentalOrderMapper rentalOrderMapper;
    @Mock
    private RentalProfitRecordMapper profitRecordMapper;
    @Mock
    private CommissionRecordMapper commissionRecordMapper;

    private DashboardService dashboardService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        var configuration = new Configuration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), AppUser.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), UserReferralRelation.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), RentalOrder.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), RentalProfitRecord.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), CommissionRecord.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), RechargeOrder.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), WithdrawOrder.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), UserWallet.class);
    }

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(appUserMapper, referralRelationMapper, userWalletMapper,
                rechargeOrderMapper, withdrawOrderMapper, rentalOrderMapper, profitRecordMapper, commissionRecordMapper);
    }

    @Test
    void overviewDoesNotExposeSensitiveFields() {
        when(appUserMapper.selectCount(any())).thenReturn(10L);
        when(rechargeOrderMapper.sumActualAmountByStatus(any())).thenReturn(new BigDecimal("100.12000000"));
        when(withdrawOrderMapper.sumActualAmountByStatus(any())).thenReturn(new BigDecimal("20.01000000"));
        when(rentalOrderMapper.sumPaidAmount()).thenReturn(new BigDecimal("300.00000000"));
        when(profitRecordMapper.sumFinalProfitAmountByStatus(any())).thenReturn(new BigDecimal("40.00000000"));
        when(commissionRecordMapper.sumCommissionAmountByStatus(any())).thenReturn(new BigDecimal("5.00000000"));
        when(rechargeOrderMapper.selectCount(any())).thenReturn(1L);
        when(withdrawOrderMapper.selectCount(any())).thenReturn(2L);
        when(rentalOrderMapper.selectCount(any())).thenReturn(3L);

        var result = dashboardService.overview();

        assertThat(result.totalUsers()).isEqualTo(10L);
        assertThat(result.runningOrderCount()).isEqualTo(3L);
        assertThat(result.totalRechargeAmount()).isEqualByComparingTo("100.12000000");

        verify(rechargeOrderMapper, never()).selectList(any());
        verify(withdrawOrderMapper, never()).selectList(any());
        verify(rentalOrderMapper, never()).selectList(any());
        verify(profitRecordMapper, never()).selectList(any());
        verify(commissionRecordMapper, never()).selectList(any());
    }

    @Test
    void ordersShouldUseGroupedStatusCounts() {
        when(rentalOrderMapper.countByOrderStatusGroup())
                .thenReturn(List.of(statusCount(RentalOrderStatus.RUNNING.name(), 2L)));
        when(rentalOrderMapper.countByProfitStatusGroup())
                .thenReturn(List.of(statusCount(ProfitStatus.RUNNING.name(), 3L)));
        when(rentalOrderMapper.selectCount(any())).thenReturn(0L);

        var result = dashboardService.orders();

        assertThat(result.orderStatusCounts())
                .filteredOn(count -> count.status().equals(RentalOrderStatus.RUNNING.name()))
                .singleElement()
                .extracting(DashboardStatusCountResponse::count)
                .isEqualTo(2L);
        assertThat(result.orderStatusCounts())
                .filteredOn(count -> count.status().equals(RentalOrderStatus.PENDING_PAY.name()))
                .singleElement()
                .extracting(DashboardStatusCountResponse::count)
                .isEqualTo(0L);
        assertThat(result.profitStatusCounts())
                .filteredOn(count -> count.status().equals(ProfitStatus.RUNNING.name()))
                .singleElement()
                .extracting(DashboardStatusCountResponse::count)
                .isEqualTo(3L);

        verify(rentalOrderMapper).countByOrderStatusGroup();
        verify(rentalOrderMapper).countByProfitStatusGroup();
        verify(rentalOrderMapper, never()).selectList(any());
    }

    private StatusCountRow statusCount(String status, Long total) {
        var row = new StatusCountRow();
        row.setStatus(status);
        row.setTotal(total);
        return row;
    }
}

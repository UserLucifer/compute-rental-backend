package com.compute.rental.modules.system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.compute.rental.modules.commission.mapper.CommissionRecordMapper;
import com.compute.rental.modules.order.entity.RentalOrder;
import com.compute.rental.modules.order.mapper.RentalOrderMapper;
import com.compute.rental.modules.order.mapper.RentalProfitRecordMapper;
import com.compute.rental.modules.user.entity.AppUser;
import com.compute.rental.modules.user.entity.UserReferralRelation;
import com.compute.rental.modules.user.mapper.AppUserMapper;
import com.compute.rental.modules.user.mapper.UserReferralRelationMapper;
import com.compute.rental.modules.wallet.mapper.RechargeOrderMapper;
import com.compute.rental.modules.wallet.mapper.UserWalletMapper;
import com.compute.rental.modules.wallet.mapper.WithdrawOrderMapper;
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
    }

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(appUserMapper, referralRelationMapper, userWalletMapper,
                rechargeOrderMapper, withdrawOrderMapper, rentalOrderMapper, profitRecordMapper, commissionRecordMapper);
    }

    @Test
    void overviewDoesNotExposeSensitiveFields() {
        when(appUserMapper.selectCount(any())).thenReturn(10L);
        when(rechargeOrderMapper.selectList(any())).thenReturn(List.of());
        when(withdrawOrderMapper.selectList(any())).thenReturn(List.of());
        when(rentalOrderMapper.selectList(any())).thenReturn(List.of());
        when(profitRecordMapper.selectList(any())).thenReturn(List.of());
        when(commissionRecordMapper.selectList(any())).thenReturn(List.of());
        when(rechargeOrderMapper.selectCount(any())).thenReturn(1L);
        when(withdrawOrderMapper.selectCount(any())).thenReturn(2L);
        when(rentalOrderMapper.selectCount(any())).thenReturn(3L);

        var result = dashboardService.overview();

        assertThat(result.totalUsers()).isEqualTo(10L);
        assertThat(result.runningOrderCount()).isEqualTo(3L);
        assertThat(result.totalRechargeAmount()).isNotNull();
    }
}

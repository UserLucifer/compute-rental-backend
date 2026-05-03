package com.compute.rental.modules.order.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.modules.order.dto.ProfitRecordQueryRequest;
import com.compute.rental.modules.order.entity.RentalOrder;
import com.compute.rental.modules.order.entity.RentalProfitRecord;
import com.compute.rental.modules.order.mapper.RentalOrderMapper;
import com.compute.rental.modules.order.mapper.RentalProfitRecordMapper;
import com.compute.rental.modules.wallet.entity.UserWallet;
import com.compute.rental.modules.wallet.mapper.UserWalletMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfitServiceTest {

    @Mock
    private RentalProfitRecordMapper rentalProfitRecordMapper;

    @Mock
    private RentalOrderMapper rentalOrderMapper;

    @Mock
    private UserWalletMapper userWalletMapper;

    @InjectMocks
    private ProfitService profitService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        var configuration = new Configuration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), RentalProfitRecord.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), RentalOrder.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), UserWallet.class);
    }

    @Test
    void userCannotQueryOtherUsersOrderProfitRecordsByOrderNo() {
        when(rentalOrderMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> profitService.pageUserProfitRecords(99L,
                new ProfitRecordQueryRequest(1, 10, null, "RO001", null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RENTAL_ORDER_NOT_FOUND);
        verify(rentalProfitRecordMapper, never()).selectPage(any(), any(Wrapper.class));
    }

    @Test
    void userCannotQueryOtherUsersOrderProfitRecordsByOrderId() {
        var order = new RentalOrder();
        order.setId(1L);
        order.setUserId(10L);
        when(rentalOrderMapper.selectById(1L)).thenReturn(order);

        assertThatThrownBy(() -> profitService.pageUserProfitRecords(99L,
                new ProfitRecordQueryRequest(1, 10, 1L, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RENTAL_ORDER_NOT_FOUND);
        verify(rentalProfitRecordMapper, never()).selectPage(any(), any(Wrapper.class));
    }
}

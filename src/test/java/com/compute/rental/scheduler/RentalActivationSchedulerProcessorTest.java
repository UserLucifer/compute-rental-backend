package com.compute.rental.scheduler;

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
import com.compute.rental.common.enums.RentalOrderStatus;
import com.compute.rental.common.enums.WalletBusinessType;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.modules.order.entity.ApiCredential;
import com.compute.rental.modules.order.entity.RentalOrder;
import com.compute.rental.modules.order.mapper.ApiCredentialMapper;
import com.compute.rental.modules.order.mapper.RentalOrderMapper;
import com.compute.rental.modules.wallet.service.WalletService;
import java.math.BigDecimal;
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
class RentalActivationSchedulerProcessorTest {

    @Mock
    private RentalOrderMapper rentalOrderMapper;

    @Mock
    private ApiCredentialMapper apiCredentialMapper;

    @Mock
    private WalletService walletService;

    @InjectMocks
    private RentalActivationSchedulerProcessor processor;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        var configuration = new Configuration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), RentalOrder.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), ApiCredential.class);
    }

    @Test
    void timeoutOrderShouldCancelAndRefund() {
        var now = LocalDateTime.now();
        when(rentalOrderMapper.selectById(1L)).thenReturn(order(RentalOrderStatus.PENDING_ACTIVATION, now.minusHours(2)));
        when(rentalOrderMapper.update(any(), any(Wrapper.class))).thenReturn(1);

        processor.cancelActivationTimeout(1L, now.minusHours(1));

        verify(walletService).credit(eq(10L), any(BigDecimal.class), eq(WalletBusinessType.REFUND),
                eq("RO001"), eq("ACTIVATION_TIMEOUT"), any());
        verify(apiCredentialMapper).update(any(), any(Wrapper.class));
    }

    @Test
    void notTimedOutOrderShouldNotProcess() {
        var now = LocalDateTime.now();
        when(rentalOrderMapper.selectById(1L)).thenReturn(order(RentalOrderStatus.PENDING_ACTIVATION, now.minusMinutes(10)));

        processor.cancelActivationTimeout(1L, now.minusHours(1));

        verify(walletService, never()).credit(any(), any(), any(), any(), any(), any());
        verify(apiCredentialMapper, never()).update(any(), any(Wrapper.class));
    }

    @Test
    void repeatedTimeoutRunShouldNotRefundChangedOrderAgain() {
        when(rentalOrderMapper.selectById(1L)).thenReturn(order(RentalOrderStatus.CANCELED, LocalDateTime.now().minusHours(2)));

        processor.cancelActivationTimeout(1L, LocalDateTime.now().minusHours(1));

        verify(walletService, never()).credit(any(), any(), any(), any(), any(), any());
    }

    @Test
    void autoPauseShouldPauseActivatingOrderAndCredential() {
        var now = LocalDateTime.now();
        when(rentalOrderMapper.selectById(1L)).thenReturn(order(RentalOrderStatus.ACTIVATING, now.minusMinutes(1)));
        when(apiCredentialMapper.selectOne(any(Wrapper.class))).thenReturn(credential(ApiTokenStatus.ACTIVATING));
        when(rentalOrderMapper.update(any(), any(Wrapper.class))).thenReturn(1);
        when(apiCredentialMapper.update(any(), any(Wrapper.class))).thenReturn(1);

        processor.autoPause(1L, now);

        verify(rentalOrderMapper).update(any(), any(Wrapper.class));
        verify(apiCredentialMapper).update(any(), any(Wrapper.class));
        verify(walletService, never()).credit(any(), any(), any(), any(), any(), any());
    }

    @Test
    void notYetAutoPauseOrderShouldNotProcess() {
        var now = LocalDateTime.now();
        when(rentalOrderMapper.selectById(1L)).thenReturn(order(RentalOrderStatus.ACTIVATING, now.plusMinutes(1)));

        processor.autoPause(1L, now);

        verify(apiCredentialMapper, never()).selectOne(any(Wrapper.class));
        verify(rentalOrderMapper, never()).update(any(), any(Wrapper.class));
    }

    @Test
    void nonActivatingOrderShouldNotAutoPause() {
        var now = LocalDateTime.now();
        when(rentalOrderMapper.selectById(1L)).thenReturn(order(RentalOrderStatus.PAUSED, now.minusMinutes(1)));

        processor.autoPause(1L, now);

        verify(apiCredentialMapper, never()).selectOne(any(Wrapper.class));
        verify(rentalOrderMapper, never()).update(any(), any(Wrapper.class));
    }

    @Test
    void autoPauseShouldFailWhenCredentialNotActivating() {
        var now = LocalDateTime.now();
        when(rentalOrderMapper.selectById(1L)).thenReturn(order(RentalOrderStatus.ACTIVATING, now.minusMinutes(1)));
        when(apiCredentialMapper.selectOne(any(Wrapper.class))).thenReturn(credential(ApiTokenStatus.PAUSED));

        assertThatThrownBy(() -> processor.autoPause(1L, now))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.API_CREDENTIAL_NOT_ACTIVE);
    }

    private RentalOrder order(RentalOrderStatus status, LocalDateTime time) {
        var order = new RentalOrder();
        order.setId(1L);
        order.setOrderNo("RO001");
        order.setUserId(10L);
        order.setOrderAmount(new BigDecimal("1000.00000000"));
        order.setOrderStatus(status.name());
        order.setApiGeneratedAt(time);
        order.setAutoPauseAt(time);
        return order;
    }

    private ApiCredential credential(ApiTokenStatus status) {
        var credential = new ApiCredential();
        credential.setId(2L);
        credential.setRentalOrderId(1L);
        credential.setTokenStatus(status.name());
        return credential;
    }
}

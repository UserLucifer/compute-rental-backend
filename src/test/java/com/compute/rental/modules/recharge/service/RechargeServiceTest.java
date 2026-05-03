package com.compute.rental.modules.recharge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.compute.rental.common.enums.CommonStatus;
import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.enums.RechargeOrderStatus;
import com.compute.rental.common.enums.WalletBusinessType;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.common.util.RedisKeys;
import com.compute.rental.common.util.RedisLockClient;
import com.compute.rental.common.util.RedisLockClient.RedisLock;
import com.compute.rental.modules.recharge.dto.AdminApproveRechargeRequest;
import com.compute.rental.modules.recharge.dto.AdminRejectRechargeRequest;
import com.compute.rental.modules.recharge.dto.CreateRechargeOrderRequest;
import com.compute.rental.modules.system.service.SysConfigDefaults;
import com.compute.rental.modules.system.service.SysConfigService;
import com.compute.rental.modules.user.mapper.AppUserMapper;
import com.compute.rental.modules.wallet.entity.RechargeChannel;
import com.compute.rental.modules.wallet.entity.RechargeOrder;
import com.compute.rental.modules.wallet.entity.UserWallet;
import com.compute.rental.modules.wallet.entity.WalletTransaction;
import com.compute.rental.modules.wallet.mapper.RechargeChannelMapper;
import com.compute.rental.modules.wallet.mapper.RechargeOrderMapper;
import com.compute.rental.modules.wallet.mapper.UserWalletMapper;
import com.compute.rental.modules.wallet.service.WalletService;
import java.math.BigDecimal;
import java.util.Optional;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class RechargeServiceTest {

    @Mock
    private RechargeChannelMapper rechargeChannelMapper;

    @Mock
    private RechargeOrderMapper rechargeOrderMapper;

    @Mock
    private UserWalletMapper userWalletMapper;

    @Mock
    private AppUserMapper appUserMapper;

    @Mock
    private SysConfigService sysConfigService;

    @Mock
    private WalletService walletService;

    @Mock
    private RedisLockClient redisLockClient;

    @Captor
    private ArgumentCaptor<RechargeOrder> rechargeOrderCaptor;

    @InjectMocks
    private RechargeService rechargeService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        var configuration = new Configuration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), RechargeChannel.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), RechargeOrder.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), UserWallet.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), WalletTransaction.class);
    }

    @BeforeEach
    void setUpRedisLock() {
        lenient().when(redisLockClient.tryLock(any(), any()))
                .thenReturn(Optional.of(new RedisLock("test-lock", "test-value")));
    }

    @Test
    void createOrderShouldSubmitRechargeOrder() {
        when(rechargeChannelMapper.selectById(1L)).thenReturn(channel(new BigDecimal("600.00000000")));
        when(sysConfigService.getBigDecimal(eq(SysConfigDefaults.RECHARGE_MIN_AMOUNT), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("500.00000000"));
        when(rechargeOrderMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(userWalletMapper.selectOne(any(Wrapper.class))).thenReturn(wallet());
        when(rechargeOrderMapper.insert(any(RechargeOrder.class))).thenReturn(1);

        var response = rechargeService.createOrder(10L, new CreateRechargeOrderRequest(
                1L,
                new BigDecimal("600.00000000"),
                "tx001",
                "proof.png",
                "remark"
        ));

        verify(rechargeOrderMapper).insert(rechargeOrderCaptor.capture());
        var saved = rechargeOrderCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(RechargeOrderStatus.SUBMITTED.name());
        assertThat(saved.getApplyAmount()).isEqualByComparingTo("600.00000000");
        assertThat(saved.getExternalTxNo()).isEqualTo("tx001");
        assertThat(response.status()).isEqualTo(RechargeOrderStatus.SUBMITTED.name());
    }

    @Test
    void createOrderShouldRejectWhenUserCreateLockExists() {
        when(redisLockClient.tryLock(eq(RedisKeys.rechargeCreateLock(10L)), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rechargeService.createOrder(10L, new CreateRechargeOrderRequest(
                1L,
                new BigDecimal("600.00000000"),
                "tx001",
                "proof.png",
                "remark"
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RECHARGE_ORDER_PROCESSING);

        verify(rechargeChannelMapper, never()).selectById(any());
        verify(rechargeOrderMapper, never()).insert(any(RechargeOrder.class));
    }

    @Test
    void createOrderShouldRejectAmountBelowEffectiveMinimum() {
        when(rechargeChannelMapper.selectById(1L)).thenReturn(channel(new BigDecimal("600.00000000")));
        when(sysConfigService.getBigDecimal(eq(SysConfigDefaults.RECHARGE_MIN_AMOUNT), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("500.00000000"));

        assertThatThrownBy(() -> rechargeService.createOrder(10L, new CreateRechargeOrderRequest(
                1L,
                new BigDecimal("599.00000000"),
                null,
                null,
                null
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RECHARGE_AMOUNT_BELOW_MIN);
        verify(rechargeOrderMapper, never()).insert(any(RechargeOrder.class));
    }

    @Test
    void createOrderShouldRejectDuplicateExternalTxNo() {
        when(rechargeChannelMapper.selectById(1L)).thenReturn(channel(null));
        when(sysConfigService.getBigDecimal(eq(SysConfigDefaults.RECHARGE_MIN_AMOUNT), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("500.00000000"));
        when(rechargeOrderMapper.selectOne(any(Wrapper.class))).thenReturn(order(RechargeOrderStatus.SUBMITTED));

        assertThatThrownBy(() -> rechargeService.createOrder(10L, new CreateRechargeOrderRequest(
                1L,
                new BigDecimal("500.00000000"),
                "tx001",
                null,
                null
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RECHARGE_EXTERNAL_TX_NO_EXISTS);
        verify(userWalletMapper, never()).selectOne(any(Wrapper.class));
    }

    @Test
    void approveShouldCreditWalletAndStoreWalletTxNo() {
        var submitted = order(RechargeOrderStatus.SUBMITTED);
        var approved = order(RechargeOrderStatus.APPROVED);
        approved.setWalletTxNo("WT001");
        when(rechargeOrderMapper.selectOne(any(Wrapper.class))).thenReturn(submitted, approved);
        when(rechargeOrderMapper.update(any(), any(Wrapper.class))).thenReturn(1);
        var walletTx = new WalletTransaction();
        walletTx.setTxNo("WT001");
        when(walletService.credit(eq(10L), any(BigDecimal.class), eq(WalletBusinessType.RECHARGE),
                eq("RC001"), eq("APPROVE"), any())).thenReturn(walletTx);

        var response = rechargeService.approve("RC001", 99L,
                new AdminApproveRechargeRequest(new BigDecimal("500.00000000"), "ok"));

        verify(walletService).credit(eq(10L), any(BigDecimal.class), eq(WalletBusinessType.RECHARGE),
                eq("RC001"), eq("APPROVE"), any());
        assertThat(response.status()).isEqualTo(RechargeOrderStatus.APPROVED.name());
    }

    @Test
    void approveShouldRejectWhenRechargeOperationLockExists() {
        when(redisLockClient.tryLock(eq(RedisKeys.rechargeOperationLock("RC001", "approve")), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> rechargeService.approve("RC001", 99L,
                new AdminApproveRechargeRequest(new BigDecimal("500.00000000"), "ok")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RECHARGE_ORDER_PROCESSING);

        verify(rechargeOrderMapper, never()).selectOne(any(Wrapper.class));
        verify(walletService, never()).credit(any(), any(), any(), any(), any(), any());
    }

    @Test
    void approveShouldReleaseLockAfterTransactionCompletion() {
        var lock = new RedisLock("recharge-lock", "owner-token");
        when(redisLockClient.tryLock(eq(RedisKeys.rechargeOperationLock("RC001", "approve")), any()))
                .thenReturn(Optional.of(lock));
        var submitted = order(RechargeOrderStatus.SUBMITTED);
        var approved = order(RechargeOrderStatus.APPROVED);
        approved.setWalletTxNo("WT001");
        when(rechargeOrderMapper.selectOne(any(Wrapper.class))).thenReturn(submitted, approved);
        when(rechargeOrderMapper.update(any(), any(Wrapper.class))).thenReturn(1);
        var walletTx = new WalletTransaction();
        walletTx.setTxNo("WT001");
        when(walletService.credit(eq(10L), any(BigDecimal.class), eq(WalletBusinessType.RECHARGE),
                eq("RC001"), eq("APPROVE"), any())).thenReturn(walletTx);

        TransactionSynchronizationManager.initSynchronization();
        try {
            rechargeService.approve("RC001", 99L,
                    new AdminApproveRechargeRequest(new BigDecimal("500.00000000"), "ok"));

            verify(redisLockClient, never()).unlock(any());
            var synchronizations = TransactionSynchronizationManager.getSynchronizations();
            assertThat(synchronizations).hasSize(1);
            synchronizations.get(0).afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
            verify(redisLockClient).unlock(lock);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void approveShouldRejectChangedStatusAndNotCreditAgain() {
        when(rechargeOrderMapper.selectOne(any(Wrapper.class))).thenReturn(order(RechargeOrderStatus.APPROVED));

        assertThatThrownBy(() -> rechargeService.approve("RC001", 99L,
                new AdminApproveRechargeRequest(new BigDecimal("500.00000000"), "ok")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RECHARGE_ORDER_NOT_APPROVABLE);
        verify(walletService, never()).credit(any(), any(), any(), any(), any(), any());
    }

    @Test
    void rejectShouldRejectWhenRechargeOperationLockExists() {
        when(redisLockClient.tryLock(eq(RedisKeys.rechargeOperationLock("RC001", "reject")), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> rechargeService.reject("RC001", 99L, new AdminRejectRechargeRequest("bad proof")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RECHARGE_ORDER_PROCESSING);

        verify(rechargeOrderMapper, never()).selectOne(any(Wrapper.class));
        verify(walletService, never()).credit(any(), any(), any(), any(), any(), any());
    }

    @Test
    void rejectShouldNotCreditWallet() {
        var submitted = order(RechargeOrderStatus.SUBMITTED);
        var rejected = order(RechargeOrderStatus.REJECTED);
        when(rechargeOrderMapper.selectOne(any(Wrapper.class))).thenReturn(submitted, rejected);
        when(rechargeOrderMapper.update(any(), any(Wrapper.class))).thenReturn(1);

        var response = rechargeService.reject("RC001", 99L, new AdminRejectRechargeRequest("bad proof"));

        assertThat(response.status()).isEqualTo(RechargeOrderStatus.REJECTED.name());
        verify(walletService, never()).credit(any(), any(), any(), any(), any(), any());
    }

    @Test
    void cancelShouldRejectWhenRechargeOperationLockExists() {
        when(redisLockClient.tryLock(eq(RedisKeys.rechargeOperationLock("RC001", "cancel")), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> rechargeService.cancelUserOrder(10L, "RC001"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RECHARGE_ORDER_PROCESSING);

        verify(rechargeOrderMapper, never()).selectOne(any(Wrapper.class));
        verify(rechargeOrderMapper, never()).update(any(), any(Wrapper.class));
    }

    @Test
    void getUserOrderShouldNotReturnOtherUsersOrder() {
        when(rechargeOrderMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> rechargeService.getUserOrder(99L, "RC001"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RECHARGE_ORDER_NOT_FOUND);
    }

    private RechargeChannel channel(BigDecimal minAmount) {
        var channel = new RechargeChannel();
        channel.setId(1L);
        channel.setChannelCode("USDT_TRC20");
        channel.setChannelName("USDT TRC20");
        channel.setNetwork("TRC20");
        channel.setDisplayUrl("https://example.test/qr.png");
        channel.setAccountNo("TXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
        channel.setMinAmount(minAmount);
        channel.setStatus(CommonStatus.ENABLED.value());
        return channel;
    }

    private UserWallet wallet() {
        var wallet = new UserWallet();
        wallet.setId(100L);
        wallet.setUserId(10L);
        wallet.setStatus(CommonStatus.ENABLED.value());
        return wallet;
    }

    private RechargeOrder order(RechargeOrderStatus status) {
        var order = new RechargeOrder();
        order.setId(1L);
        order.setRechargeNo("RC001");
        order.setUserId(10L);
        order.setWalletId(100L);
        order.setChannelId(1L);
        order.setCurrency("USDT");
        order.setChannelNameSnapshot("USDT TRC20");
        order.setApplyAmount(new BigDecimal("500.00000000"));
        order.setStatus(status.name());
        return order;
    }
}

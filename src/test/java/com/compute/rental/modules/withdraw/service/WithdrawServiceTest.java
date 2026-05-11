package com.compute.rental.modules.withdraw.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.compute.rental.common.enums.CommonStatus;
import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.enums.WalletBusinessType;
import com.compute.rental.common.enums.WithdrawOrderStatus;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.common.util.RedisKeys;
import com.compute.rental.common.util.RedisLockClient;
import com.compute.rental.common.util.RedisLockClient.RedisLock;
import com.compute.rental.modules.system.service.SysConfigDefaults;
import com.compute.rental.modules.system.service.SysConfigService;
import com.compute.rental.modules.user.mapper.AppUserMapper;
import com.compute.rental.modules.wallet.entity.UserWallet;
import com.compute.rental.modules.wallet.entity.UserWithdrawAddress;
import com.compute.rental.modules.wallet.entity.WalletTransaction;
import com.compute.rental.modules.wallet.entity.WithdrawOrder;
import com.compute.rental.modules.wallet.mapper.UserWalletMapper;
import com.compute.rental.modules.wallet.mapper.WithdrawOrderMapper;
import com.compute.rental.modules.wallet.service.WalletService;
import com.compute.rental.modules.withdraw.dto.AdminApproveWithdrawRequest;
import com.compute.rental.modules.withdraw.dto.AdminPaidWithdrawRequest;
import com.compute.rental.modules.withdraw.dto.AdminRejectWithdrawRequest;
import com.compute.rental.modules.withdraw.dto.CreateWithdrawOrderRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
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
class WithdrawServiceTest {

    @Mock
    private WithdrawOrderMapper withdrawOrderMapper;

    @Mock
    private UserWalletMapper userWalletMapper;

    @Mock
    private AppUserMapper appUserMapper;

    @Mock
    private SysConfigService sysConfigService;

    @Mock
    private WalletService walletService;

    @Mock
    private WithdrawAddressValidator addressValidator;

    @Mock
    private WithdrawAddressService withdrawAddressService;

    @Mock
    private RedisLockClient redisLockClient;

    @Captor
    private ArgumentCaptor<WithdrawOrder> withdrawOrderCaptor;

    @InjectMocks
    private WithdrawService withdrawService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        var configuration = new Configuration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), WithdrawOrder.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), UserWallet.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), WalletTransaction.class);
    }

    @BeforeEach
    void setUpRedisLock() {
        lenient().when(redisLockClient.tryLock(any(), any()))
                .thenReturn(Optional.of(new RedisLock("test-lock", "test-value")));
    }

    @Test
    void createOrderShouldFreezeApplyAmount() {
        var savedOrder = new AtomicReference<WithdrawOrder>();
        mockWithdrawConfigs();
        when(addressValidator.requireValid("TRC20", "T123456789ABCDEFGHJKLMNPQRSTUVWXy")).thenReturn("TRC20");
        when(userWalletMapper.selectOne(any(Wrapper.class))).thenReturn(wallet());
        doAnswer(invocation -> {
            var order = invocation.getArgument(0, WithdrawOrder.class);
            order.setId(1L);
            savedOrder.set(order);
            return 1;
        }).when(withdrawOrderMapper).insert(any(WithdrawOrder.class));
        var freezeTx = tx("WT_FREEZE");
        when(walletService.freeze(eq(10L), any(BigDecimal.class), eq(WalletBusinessType.WITHDRAW),
                any(), eq("FREEZE"), any())).thenReturn(freezeTx);
        when(withdrawOrderMapper.update(any(), any(Wrapper.class))).thenReturn(1);
        when(withdrawOrderMapper.selectOne(any(Wrapper.class))).thenAnswer(invocation -> savedOrder.get());

        var response = withdrawService.createOrder(10L, new CreateWithdrawOrderRequest(
                "TRC20",
                "name",
                "T123456789ABCDEFGHJKLMNPQRSTUVWXy",
                new BigDecimal("50.00000000"),
                "REQ001",
                null
        ));

        verify(withdrawOrderMapper).insert(withdrawOrderCaptor.capture());
        var inserted = withdrawOrderCaptor.getValue();
        assertThat(inserted.getStatus()).isEqualTo(WithdrawOrderStatus.PENDING_REVIEW.name());
        assertThat(inserted.getFeeAmount()).isEqualByComparingTo("2.50000000");
        assertThat(inserted.getActualAmount()).isEqualByComparingTo("47.50000000");
        verify(walletService).freeze(eq(10L), any(BigDecimal.class), eq(WalletBusinessType.WITHDRAW),
                eq(inserted.getWithdrawNo()), eq("FREEZE"), any());
        assertThat(response.status()).isEqualTo(WithdrawOrderStatus.PENDING_REVIEW.name());
    }

    @Test
    void createOrderShouldReturnExistingWhenClientRequestIdRepeats() {
        var existing = order(WithdrawOrderStatus.PENDING_REVIEW, "50.00000000");
        existing.setAccountName("name");
        when(withdrawOrderMapper.selectOne(any(Wrapper.class))).thenReturn(existing);

        var response = withdrawService.createOrder(10L, new CreateWithdrawOrderRequest(
                "TRC20",
                "name",
                "T123456789ABCDEFGHJKLMNPQRSTUVWXy",
                new BigDecimal("50.00000000"),
                "REQ001",
                null
        ));

        assertThat(response.withdrawNo()).isEqualTo("WD001");
        verify(withdrawOrderMapper, never()).insert(any(WithdrawOrder.class));
        verify(walletService, never()).freeze(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createOrderShouldUseWithdrawAddressIdSnapshot() {
        var savedOrder = new AtomicReference<WithdrawOrder>();
        mockWithdrawConfigs();
        when(withdrawAddressService.requireAddress(10L, 7L)).thenReturn(address());
        when(userWalletMapper.selectOne(any(Wrapper.class))).thenReturn(wallet());
        doAnswer(invocation -> {
            var order = invocation.getArgument(0, WithdrawOrder.class);
            order.setId(1L);
            savedOrder.set(order);
            return 1;
        }).when(withdrawOrderMapper).insert(any(WithdrawOrder.class));
        when(walletService.freeze(eq(10L), any(BigDecimal.class), eq(WalletBusinessType.WITHDRAW),
                any(), eq("FREEZE"), any())).thenReturn(tx("WT_FREEZE"));
        when(withdrawOrderMapper.update(any(), any(Wrapper.class))).thenReturn(1);
        when(withdrawOrderMapper.selectOne(any(Wrapper.class))).thenAnswer(invocation -> savedOrder.get());

        withdrawService.createOrder(10L, new CreateWithdrawOrderRequest(
                null,
                null,
                null,
                new BigDecimal("50.00000000"),
                "REQ001",
                7L
        ));

        verify(withdrawOrderMapper).insert(withdrawOrderCaptor.capture());
        var inserted = withdrawOrderCaptor.getValue();
        assertThat(inserted.getWithdrawAddressId()).isEqualTo(7L);
        assertThat(inserted.getNetwork()).isEqualTo("TRC20");
        assertThat(inserted.getAccountName()).isEqualTo("saved name");
        assertThat(inserted.getAccountNo()).isEqualTo("T123456789ABCDEFGHJKLMNPQRSTUVWXy");
    }

    @Test
    void createOrderShouldRejectWhenUserCreateLockExists() {
        when(redisLockClient.tryLock(eq(RedisKeys.withdrawCreateLock(10L)), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> withdrawService.createOrder(10L, new CreateWithdrawOrderRequest(
                "TRC20",
                "name",
                "T123456789ABCDEFGHJKLMNPQRSTUVWXy",
                new BigDecimal("50.00000000"),
                "REQ001",
                null
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.WITHDRAW_ORDER_PROCESSING);

        verify(withdrawOrderMapper, never()).insert(any(WithdrawOrder.class));
        verify(walletService, never()).freeze(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createOrderShouldRejectBelowMinimumAmount() {
        when(sysConfigService.getBigDecimal(eq(SysConfigDefaults.WITHDRAW_MIN_AMOUNT), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("10"));

        assertThatThrownBy(() -> withdrawService.createOrder(10L, new CreateWithdrawOrderRequest(
                "TRC20",
                null,
                "T123456789ABCDEFGHJKLMNPQRSTUVWXy",
                new BigDecimal("9.99000000"),
                "REQ001",
                null
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.WITHDRAW_AMOUNT_BELOW_MIN);
        verify(walletService, never()).freeze(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createOrderShouldRejectInvalidAddress() {
        when(sysConfigService.getBigDecimal(eq(SysConfigDefaults.WITHDRAW_MIN_AMOUNT), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("10"));
        when(addressValidator.requireValid("TRC20", "bad")).thenThrow(new BusinessException(ErrorCode.WITHDRAW_ADDRESS_INVALID));

        assertThatThrownBy(() -> withdrawService.createOrder(10L, new CreateWithdrawOrderRequest(
                "TRC20",
                null,
                "bad",
                new BigDecimal("10.00000000"),
                "REQ001",
                null
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.WITHDRAW_ADDRESS_INVALID);
        verify(walletService, never()).freeze(any(), any(), any(), any(), any(), any());
    }

    @Test
    void cancelShouldUnfreezePendingOrder() {
        when(withdrawOrderMapper.selectOne(any(Wrapper.class))).thenReturn(order(WithdrawOrderStatus.PENDING_REVIEW, "50.00000000"));
        when(withdrawOrderMapper.update(any(), any(Wrapper.class))).thenReturn(1);
        when(walletService.unfreeze(eq(10L), any(BigDecimal.class), eq(WalletBusinessType.WITHDRAW),
                eq("WD001"), eq("UNFREEZE"), any())).thenReturn(tx("WT_UNFREEZE"));

        withdrawService.cancelUserOrder(10L, "WD001");

        verify(walletService).unfreeze(eq(10L), any(BigDecimal.class), eq(WalletBusinessType.WITHDRAW),
                eq("WD001"), eq("UNFREEZE"), any());
    }

    @Test
    void cancelShouldRejectWhenWithdrawOperationLockExists() {
        when(redisLockClient.tryLock(eq(RedisKeys.withdrawOperationLock("WD001", "cancel")), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> withdrawService.cancelUserOrder(10L, "WD001"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.WITHDRAW_ORDER_PROCESSING);

        verify(withdrawOrderMapper, never()).selectOne(any(Wrapper.class));
        verify(walletService, never()).unfreeze(any(), any(), any(), any(), any(), any());
    }

    @Test
    void paidShouldReleaseLockAfterTransactionCompletion() {
        var lock = new RedisLock("withdraw-lock", "owner-token");
        when(redisLockClient.tryLock(eq(RedisKeys.withdrawOperationLock("WD001", "paid")), any()))
                .thenReturn(Optional.of(lock));
        when(withdrawOrderMapper.selectOne(any(Wrapper.class))).thenReturn(order(WithdrawOrderStatus.APPROVED, "50.00000000"),
                order(WithdrawOrderStatus.PAID, "50.00000000"));
        when(withdrawOrderMapper.update(any(), any(Wrapper.class))).thenReturn(1);
        when(walletService.deductFrozen(eq(10L), any(BigDecimal.class), any(BigDecimal.class),
                eq(WalletBusinessType.WITHDRAW), eq("WD001"), eq("PAID"), any())).thenReturn(tx("WT_PAID"));

        TransactionSynchronizationManager.initSynchronization();
        try {
            withdrawService.paid("WD001", new AdminPaidWithdrawRequest("chain-tx"));

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
    void approveShouldNotTouchWallet() {
        when(withdrawOrderMapper.selectOne(any(Wrapper.class))).thenReturn(order(WithdrawOrderStatus.PENDING_REVIEW, "50.00000000"),
                order(WithdrawOrderStatus.APPROVED, "50.00000000"));
        when(withdrawOrderMapper.update(any(), any(Wrapper.class))).thenReturn(1);

        var response = withdrawService.approve("WD001", 99L, new AdminApproveWithdrawRequest("ok"));

        assertThat(response.status()).isEqualTo(WithdrawOrderStatus.APPROVED.name());
        verify(walletService, never()).freeze(any(), any(), any(), any(), any(), any());
        verify(walletService, never()).unfreeze(any(), any(), any(), any(), any(), any());
        verify(walletService, never()).deductFrozen(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void rejectShouldUnfreezePendingOrApprovedOrder() {
        when(withdrawOrderMapper.selectOne(any(Wrapper.class))).thenReturn(order(WithdrawOrderStatus.APPROVED, "50.00000000"),
                order(WithdrawOrderStatus.REJECTED, "50.00000000"));
        when(withdrawOrderMapper.update(any(), any(Wrapper.class))).thenReturn(1);
        when(walletService.unfreeze(eq(10L), any(BigDecimal.class), eq(WalletBusinessType.WITHDRAW),
                eq("WD001"), eq("UNFREEZE"), any())).thenReturn(tx("WT_UNFREEZE"));

        var response = withdrawService.reject("WD001", 99L, new AdminRejectWithdrawRequest("reject"));

        assertThat(response.status()).isEqualTo(WithdrawOrderStatus.REJECTED.name());
        verify(walletService).unfreeze(eq(10L), any(BigDecimal.class), eq(WalletBusinessType.WITHDRAW),
                eq("WD001"), eq("UNFREEZE"), any());
    }

    @Test
    void paidShouldDeductFrozenAndIncreaseTotalWithdrawByActualAmount() {
        when(withdrawOrderMapper.selectOne(any(Wrapper.class))).thenReturn(order(WithdrawOrderStatus.APPROVED, "50.00000000"),
                order(WithdrawOrderStatus.PAID, "50.00000000"));
        when(withdrawOrderMapper.update(any(), any(Wrapper.class))).thenReturn(1);
        when(walletService.deductFrozen(eq(10L), any(BigDecimal.class), any(BigDecimal.class),
                eq(WalletBusinessType.WITHDRAW), eq("WD001"), eq("PAID"), any())).thenReturn(tx("WT_PAID"));

        var response = withdrawService.paid("WD001", new AdminPaidWithdrawRequest("chain-tx"));

        assertThat(response.status()).isEqualTo(WithdrawOrderStatus.PAID.name());
        verify(walletService).deductFrozen(eq(10L), any(BigDecimal.class), any(BigDecimal.class),
                eq(WalletBusinessType.WITHDRAW), eq("WD001"), eq("PAID"), any());
    }

    @Test
    void getUserOrderShouldNotReturnOtherUsersOrder() {
        when(withdrawOrderMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> withdrawService.getUserOrder(99L, "WD001"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.WITHDRAW_ORDER_NOT_FOUND);
    }

    private void mockWithdrawConfigs() {
        when(sysConfigService.getBigDecimal(eq(SysConfigDefaults.WITHDRAW_MIN_AMOUNT), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("10"));
        when(sysConfigService.getBigDecimal(eq(SysConfigDefaults.WITHDRAW_FEE_FREE_THRESHOLD), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("100"));
        when(sysConfigService.getBigDecimal(eq(SysConfigDefaults.WITHDRAW_FEE_RATE), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("0.05"));
    }

    private UserWallet wallet() {
        var wallet = new UserWallet();
        wallet.setId(100L);
        wallet.setUserId(10L);
        wallet.setStatus(CommonStatus.ENABLED.value());
        return wallet;
    }

    private UserWithdrawAddress address() {
        var address = new UserWithdrawAddress();
        address.setId(7L);
        address.setUserId(10L);
        address.setNetwork("TRC20");
        address.setAccountName("saved name");
        address.setAccountNo("T123456789ABCDEFGHJKLMNPQRSTUVWXy");
        address.setIsDefault(1);
        address.setStatus(CommonStatus.ENABLED.value());
        return address;
    }

    private WithdrawOrder order(WithdrawOrderStatus status, String applyAmount) {
        var order = new WithdrawOrder();
        order.setId(1L);
        order.setWithdrawNo("WD001");
        order.setUserId(10L);
        order.setWalletId(100L);
        order.setCurrency("USDT");
        order.setWithdrawMethod("USDT");
        order.setNetwork("TRC20");
        order.setAccountNo("T123456789ABCDEFGHJKLMNPQRSTUVWXy");
        order.setApplyAmount(new BigDecimal(applyAmount));
        order.setFeeAmount(new BigDecimal("2.50000000"));
        order.setActualAmount(new BigDecimal(applyAmount).subtract(new BigDecimal("2.50000000")));
        order.setStatus(status.name());
        return order;
    }

    private WalletTransaction tx(String txNo) {
        var tx = new WalletTransaction();
        tx.setTxNo(txNo);
        return tx;
    }
}

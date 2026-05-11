package com.compute.rental.modules.order.service;

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
import com.compute.rental.common.enums.ApiDeployOrderStatus;
import com.compute.rental.common.enums.ApiTokenStatus;
import com.compute.rental.common.enums.CommonStatus;
import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.enums.ProfitStatus;
import com.compute.rental.common.enums.RentalOrderSettlementStatus;
import com.compute.rental.common.enums.RentalOrderStatus;
import com.compute.rental.common.enums.WalletBusinessType;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.common.util.RedisKeys;
import com.compute.rental.common.util.RedisLockClient;
import com.compute.rental.common.util.RedisLockClient.RedisLock;
import com.compute.rental.modules.order.dto.CreateRentalOrderRequest;
import com.compute.rental.modules.order.entity.ApiDeployOrder;
import com.compute.rental.modules.order.entity.ApiCredential;
import com.compute.rental.modules.order.entity.RentalOrder;
import com.compute.rental.modules.order.mapper.ApiDeployOrderMapper;
import com.compute.rental.modules.order.mapper.ApiCredentialMapper;
import com.compute.rental.modules.order.mapper.RentalOrderMapper;
import com.compute.rental.modules.product.entity.AiModel;
import com.compute.rental.modules.product.entity.GpuModel;
import com.compute.rental.modules.product.entity.Product;
import com.compute.rental.modules.product.entity.Region;
import com.compute.rental.modules.product.entity.RentalCycleRule;
import com.compute.rental.modules.product.mapper.AiModelMapper;
import com.compute.rental.modules.product.mapper.GpuModelMapper;
import com.compute.rental.modules.product.mapper.ProductMapper;
import com.compute.rental.modules.product.mapper.RegionMapper;
import com.compute.rental.modules.product.mapper.RentalCycleRuleMapper;
import com.compute.rental.modules.system.service.SysConfigDefaults;
import com.compute.rental.modules.system.service.SysConfigService;
import com.compute.rental.modules.user.mapper.AppUserMapper;
import com.compute.rental.modules.wallet.entity.WalletTransaction;
import com.compute.rental.modules.wallet.service.WalletService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
class RentalOrderServiceTest {

    @Mock
    private RentalOrderMapper rentalOrderMapper;

    @Mock
    private ApiDeployOrderMapper apiDeployOrderMapper;

    @Mock
    private ApiCredentialMapper apiCredentialMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private AiModelMapper aiModelMapper;

    @Mock
    private RentalCycleRuleMapper rentalCycleRuleMapper;

    @Mock
    private RegionMapper regionMapper;

    @Mock
    private GpuModelMapper gpuModelMapper;

    @Mock
    private AppUserMapper appUserMapper;

    @Mock
    private WalletService walletService;

    @Mock
    private ApiTokenCryptoService apiTokenCryptoService;

    @Mock
    private ApiTokenProperties apiTokenProperties;

    @Mock
    private RedisLockClient redisLockClient;

    @Mock
    private RentalOrderRunSegmentService runSegmentService;

    @Mock
    private SysConfigService sysConfigService;

    @Captor
    private ArgumentCaptor<RentalOrder> rentalOrderCaptor;

    @Captor
    private ArgumentCaptor<ApiCredential> apiCredentialCaptor;

    @Captor
    private ArgumentCaptor<ApiDeployOrder> apiDeployOrderCaptor;

    @InjectMocks
    private RentalOrderService rentalOrderService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        var configuration = new Configuration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), RentalOrder.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), ApiDeployOrder.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), ApiCredential.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), Product.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), AiModel.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), RentalCycleRule.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), Region.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), GpuModel.class);
    }

    @BeforeEach
    void setUpRedisLock() {
        lenient().when(redisLockClient.tryLock(any(), any()))
                .thenReturn(Optional.of(new RedisLock("test-lock", "test-value")));
        lenient().when(sysConfigService.getInteger(
                        eq(SysConfigDefaults.ORDER_PENDING_ACTIVATION_TIMEOUT_MINUTES),
                        eq(15)))
                .thenReturn(15);
    }

    @Test
    void createOrderShouldSaveSnapshotsAndNotDebitWallet() {
        mockCatalog();
        when(rentalOrderMapper.insert(any(RentalOrder.class))).thenReturn(1);

        var response = rentalOrderService.createOrder(10L, new CreateRentalOrderRequest(1L, 2L, 3L, "REQ001"));

        verify(rentalOrderMapper).insert(rentalOrderCaptor.capture());
        var saved = rentalOrderCaptor.getValue();
        assertThat(saved.getProductNameSnapshot()).isEqualTo("A100 Rental");
        assertThat(saved.getRegionNameSnapshot()).isEqualTo("Hong Kong");
        assertThat(saved.getGpuModelSnapshot()).isEqualTo("NVIDIA A100");
        assertThat(saved.getAiModelNameSnapshot()).isEqualTo("GPT Test");
        assertThat(saved.getCycleDaysSnapshot()).isEqualTo(30);
        assertThat(saved.getOrderAmount()).isEqualByComparingTo("1000.00000000");
        assertThat(saved.getPaidAmount()).isEqualByComparingTo("0.00000000");
        assertThat(saved.getExpectedDailyProfit()).isEqualByComparingTo("12.00000000");
        assertThat(saved.getExpectedTotalProfit()).isEqualByComparingTo("360.00000000");
        assertThat(saved.getOrderStatus()).isEqualTo(RentalOrderStatus.PENDING_PAY.name());
        assertThat(saved.getProfitStatus()).isEqualTo(ProfitStatus.NOT_STARTED.name());
        assertThat(saved.getSettlementStatus()).isEqualTo(RentalOrderSettlementStatus.UNSETTLED.name());
        assertThat(response.orderStatus()).isEqualTo(RentalOrderStatus.PENDING_PAY.name());
        verify(walletService, never()).debit(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createOrderShouldReturnExistingWhenClientRequestIdRepeats() {
        when(rentalOrderMapper.selectOne(any(Wrapper.class))).thenReturn(order(RentalOrderStatus.PENDING_PAY));

        var response = rentalOrderService.createOrder(10L, new CreateRentalOrderRequest(1L, 2L, 3L, "REQ001"));

        assertThat(response.orderNo()).isEqualTo("RO001");
        verify(rentalOrderMapper, never()).insert(any(RentalOrder.class));
        verify(productMapper, never()).selectById(any());
    }

    @Test
    void createOrderShouldRejectWhenClientRequestIdReusedWithDifferentPayload() {
        when(rentalOrderMapper.selectOne(any(Wrapper.class))).thenReturn(order(RentalOrderStatus.PENDING_PAY));

        assertThatThrownBy(() -> rentalOrderService.createOrder(10L,
                new CreateRentalOrderRequest(9L, 2L, 3L, "REQ001")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IDEMPOTENCY_CONFLICT);

        verify(rentalOrderMapper, never()).insert(any(RentalOrder.class));
        verify(productMapper, never()).selectById(any());
    }

    @Test
    void payMachineFeeShouldDebitWalletAndGenerateApiCredential() {
        var pending = order(RentalOrderStatus.PENDING_PAY);
        var paid = order(RentalOrderStatus.PENDING_ACTIVATION);
        paid.setMachinePayTxNo("WT001");
        when(rentalOrderMapper.selectOne(any(Wrapper.class))).thenReturn(pending, paid);
        when(rentalOrderMapper.update(any(), any(Wrapper.class))).thenReturn(1);
        when(apiCredentialMapper.selectOne(any(Wrapper.class))).thenReturn(null, credential(ApiTokenStatus.GENERATED));
        when(walletService.debit(eq(10L), any(BigDecimal.class), eq(WalletBusinessType.RENT_PAY),
                eq("RO001"), eq("PAY"), any())).thenReturn(walletTransaction());
        when(apiTokenProperties.baseUrl()).thenReturn("https://api.example.invalid/v1");
        when(apiTokenCryptoService.encrypt(any())).thenReturn("ciphertext");
        when(apiCredentialMapper.insert(any(ApiCredential.class))).thenReturn(1);

        var response = rentalOrderService.payMachineFee(10L, "RO001");

        verify(walletService).debit(eq(10L), any(BigDecimal.class), eq(WalletBusinessType.RENT_PAY),
                eq("RO001"), eq("PAY"), any());
        verify(apiCredentialMapper).insert(apiCredentialCaptor.capture());
        var credential = apiCredentialCaptor.getValue();
        assertThat(credential.getTokenStatus()).isEqualTo(ApiTokenStatus.GENERATED.name());
        assertThat(credential.getTokenCiphertext()).isEqualTo("ciphertext");
        assertThat(credential.getTokenMasked()).startsWith("sk-****");
        assertThat(credential.getDeployFeeSnapshot()).isEqualByComparingTo("100.00000000");
        assertThat(response.orderStatus()).isEqualTo(RentalOrderStatus.PENDING_ACTIVATION.name());
    }

    @Test
    void payMachineFeeShouldRejectWhenOrderOperationLockExists() {
        when(redisLockClient.tryLock(eq(RedisKeys.orderOperationLock("RO001", "machine-pay")), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> rentalOrderService.payMachineFee(10L, "RO001"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RENTAL_ORDER_PROCESSING);

        verify(rentalOrderMapper, never()).selectOne(any(Wrapper.class));
        verify(walletService, never()).debit(any(), any(), any(), any(), any(), any());
    }

    @Test
    void payMachineFeeShouldReleaseOrderLockAfterTransactionCompletion() {
        var pending = order(RentalOrderStatus.PENDING_PAY);
        var paid = order(RentalOrderStatus.PENDING_ACTIVATION);
        var lock = new RedisLock("test-lock", "test-value");
        when(redisLockClient.tryLock(eq(RedisKeys.orderOperationLock("RO001", "machine-pay")), any()))
                .thenReturn(Optional.of(lock));
        when(rentalOrderMapper.selectOne(any(Wrapper.class))).thenReturn(pending, paid);
        when(rentalOrderMapper.update(any(), any(Wrapper.class))).thenReturn(1);
        when(apiCredentialMapper.selectOne(any(Wrapper.class))).thenReturn(null, credential(ApiTokenStatus.GENERATED));
        when(walletService.debit(eq(10L), any(BigDecimal.class), eq(WalletBusinessType.RENT_PAY),
                eq("RO001"), eq("PAY"), any())).thenReturn(walletTransaction());
        when(apiTokenProperties.baseUrl()).thenReturn("https://api.example.invalid/v1");
        when(apiTokenCryptoService.encrypt(any())).thenReturn("ciphertext");
        when(apiCredentialMapper.insert(any(ApiCredential.class))).thenReturn(1);

        TransactionSynchronizationManager.initSynchronization();
        try {
            rentalOrderService.payMachineFee(10L, "RO001");

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
    void payMachineFeeShouldRejectInsufficientBalanceAndNotGenerateCredential() {
        when(rentalOrderMapper.selectOne(any(Wrapper.class))).thenReturn(order(RentalOrderStatus.PENDING_PAY));
        when(rentalOrderMapper.update(any(), any(Wrapper.class))).thenReturn(1);
        when(walletService.debit(eq(10L), any(BigDecimal.class), eq(WalletBusinessType.RENT_PAY),
                eq("RO001"), eq("PAY"), any()))
                .thenThrow(new BusinessException(ErrorCode.INSUFFICIENT_BALANCE));

        assertThatThrownBy(() -> rentalOrderService.payMachineFee(10L, "RO001"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);
        verify(apiCredentialMapper, never()).insert(any(ApiCredential.class));
    }

    @Test
    void repeatedPayShouldNotDebitWalletAgain() {
        when(rentalOrderMapper.selectOne(any(Wrapper.class))).thenReturn(order(RentalOrderStatus.PENDING_ACTIVATION));

        assertThatThrownBy(() -> rentalOrderService.payMachineFee(10L, "RO001"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RENTAL_ORDER_NOT_PAYABLE);
        verify(walletService, never()).debit(any(), any(), any(), any(), any(), any());
        verify(apiCredentialMapper, never()).insert(any(ApiCredential.class));
    }

    @Test
    void cancelPendingPayShouldNotRefund() {
        var pending = order(RentalOrderStatus.PENDING_PAY);
        var canceled = order(RentalOrderStatus.CANCELED);
        when(rentalOrderMapper.selectOne(any(Wrapper.class))).thenReturn(pending, canceled);
        when(rentalOrderMapper.update(any(), any(Wrapper.class))).thenReturn(1);
        when(apiCredentialMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        var response = rentalOrderService.cancelOrder(10L, "RO001");

        assertThat(response.orderStatus()).isEqualTo(RentalOrderStatus.CANCELED.name());
        verify(walletService, never()).credit(any(), any(), any(), any(), any(), any());
    }

    @Test
    void cancelPendingActivationShouldRefundAndRevokeApiCredential() {
        var pendingActivation = order(RentalOrderStatus.PENDING_ACTIVATION);
        var canceled = order(RentalOrderStatus.CANCELED);
        when(rentalOrderMapper.selectOne(any(Wrapper.class))).thenReturn(pendingActivation, canceled);
        when(rentalOrderMapper.update(any(), any(Wrapper.class))).thenReturn(1);
        when(apiCredentialMapper.selectOne(any(Wrapper.class))).thenReturn(credential(ApiTokenStatus.REVOKED));
        when(walletService.credit(eq(10L), any(BigDecimal.class), eq(WalletBusinessType.REFUND),
                eq("RO001"), eq("CANCEL"), any())).thenReturn(walletTransaction());

        var response = rentalOrderService.cancelOrder(10L, "RO001");

        verify(walletService).credit(eq(10L), any(BigDecimal.class), eq(WalletBusinessType.REFUND),
                eq("RO001"), eq("CANCEL"), any());
        verify(apiCredentialMapper).update(any(), any(Wrapper.class));
        assertThat(response.orderStatus()).isEqualTo(RentalOrderStatus.CANCELED.name());
        assertThat(response.apiCredential().tokenStatus()).isEqualTo(ApiTokenStatus.REVOKED.name());
    }

    @Test
    void cancelShouldRejectActivatingOrLaterStatus() {
        when(rentalOrderMapper.selectOne(any(Wrapper.class))).thenReturn(order(RentalOrderStatus.ACTIVATING));

        assertThatThrownBy(() -> rentalOrderService.cancelOrder(10L, "RO001"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RENTAL_ORDER_NOT_CANCELABLE);
        verify(walletService, never()).credit(any(), any(), any(), any(), any(), any());
    }

    @Test
    void userShouldNotAccessOtherUsersOrder() {
        when(rentalOrderMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> rentalOrderService.getUserOrder(99L, "RO001"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RENTAL_ORDER_NOT_FOUND);
    }

    @Test
    void payDeployFeeShouldStartOrderAndCredentialImmediately() {
        when(rentalOrderMapper.selectOne(any(Wrapper.class))).thenReturn(order(RentalOrderStatus.PENDING_ACTIVATION));
        when(apiCredentialMapper.selectOne(any(Wrapper.class))).thenReturn(credential(ApiTokenStatus.GENERATED));
        when(apiDeployOrderMapper.selectOne(any(Wrapper.class))).thenReturn(null, deployOrder(ApiDeployOrderStatus.PAID));
        when(apiDeployOrderMapper.insert(any(ApiDeployOrder.class))).thenReturn(1);
        when(apiDeployOrderMapper.update(any(), any(Wrapper.class))).thenReturn(1);
        when(apiCredentialMapper.update(any(), any(Wrapper.class))).thenReturn(1);
        when(rentalOrderMapper.update(any(), any(Wrapper.class))).thenReturn(1);
        when(walletService.debit(eq(10L), any(BigDecimal.class), eq(WalletBusinessType.API_DEPLOY_FEE),
                any(), eq("PAY"), any())).thenReturn(walletTransaction());

        var response = rentalOrderService.payDeployFee(10L, "RO001");

        verify(walletService).debit(eq(10L), any(BigDecimal.class), eq(WalletBusinessType.API_DEPLOY_FEE),
                any(), eq("PAY"), any());
        verify(apiDeployOrderMapper).insert(apiDeployOrderCaptor.capture());
        assertThat(apiDeployOrderCaptor.getValue().getStatus()).isEqualTo(ApiDeployOrderStatus.PENDING_PAY.name());
        assertThat(response.status()).isEqualTo(ApiDeployOrderStatus.PAID.name());
        assertThat(response.walletTxNo()).isEqualTo("WT001");
        verify(runSegmentService).openSegment(any(RentalOrder.class), any());
    }

    @Test
    void payDeployFeeShouldRejectInsufficientBalance() {
        when(rentalOrderMapper.selectOne(any(Wrapper.class))).thenReturn(order(RentalOrderStatus.PENDING_ACTIVATION));
        when(apiCredentialMapper.selectOne(any(Wrapper.class))).thenReturn(credential(ApiTokenStatus.GENERATED));
        when(apiDeployOrderMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(apiDeployOrderMapper.insert(any(ApiDeployOrder.class))).thenReturn(1);
        when(walletService.debit(eq(10L), any(BigDecimal.class), eq(WalletBusinessType.API_DEPLOY_FEE),
                any(), eq("PAY"), any()))
                .thenThrow(new BusinessException(ErrorCode.INSUFFICIENT_BALANCE));

        assertThatThrownBy(() -> rentalOrderService.payDeployFee(10L, "RO001"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);
        verify(apiCredentialMapper, never()).update(any(), any(Wrapper.class));
        verify(rentalOrderMapper, never()).update(any(), any(Wrapper.class));
    }

    @Test
    void payDeployFeeShouldCancelWhenDeployFeeTimeoutExpired() {
        var expired = order(RentalOrderStatus.PENDING_ACTIVATION);
        expired.setApiGeneratedAt(LocalDateTime.now().minusMinutes(16));
        when(rentalOrderMapper.selectOne(any(Wrapper.class))).thenReturn(expired);
        when(apiCredentialMapper.selectOne(any(Wrapper.class))).thenReturn(credential(ApiTokenStatus.GENERATED));
        when(apiDeployOrderMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(rentalOrderMapper.update(any(), any(Wrapper.class))).thenReturn(1);
        when(walletService.credit(eq(10L), any(BigDecimal.class), eq(WalletBusinessType.REFUND),
                eq("RO001"), eq("DEPLOY_FEE_TIMEOUT"), any())).thenReturn(walletTransaction());

        assertThatThrownBy(() -> rentalOrderService.payDeployFee(10L, "RO001"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RENTAL_ORDER_DEPLOY_FEE_EXPIRED);

        verify(walletService).credit(eq(10L), any(BigDecimal.class), eq(WalletBusinessType.REFUND),
                eq("RO001"), eq("DEPLOY_FEE_TIMEOUT"), any());
        verify(apiCredentialMapper).update(any(), any(Wrapper.class));
        verify(walletService, never()).debit(any(), any(), any(), any(), any(), any());
        verify(apiDeployOrderMapper, never()).insert(any(ApiDeployOrder.class));
    }

    @Test
    void repeatedDeployPayShouldReturnPaidOrderWithoutDebitAgain() {
        when(rentalOrderMapper.selectOne(any(Wrapper.class))).thenReturn(order(RentalOrderStatus.RUNNING));
        when(apiCredentialMapper.selectOne(any(Wrapper.class))).thenReturn(credential(ApiTokenStatus.ACTIVE));
        when(apiDeployOrderMapper.selectOne(any(Wrapper.class))).thenReturn(deployOrder(ApiDeployOrderStatus.PAID));

        var response = rentalOrderService.payDeployFee(10L, "RO001");

        assertThat(response.status()).isEqualTo(ApiDeployOrderStatus.PAID.name());
        verify(walletService, never()).debit(any(), any(), any(), any(), any(), any());
        verify(apiDeployOrderMapper, never()).insert(any(ApiDeployOrder.class));
    }

    @Test
    void nonPendingActivationOrderShouldNotPayDeployFee() {
        when(rentalOrderMapper.selectOne(any(Wrapper.class))).thenReturn(order(RentalOrderStatus.PENDING_PAY));
        when(apiCredentialMapper.selectOne(any(Wrapper.class))).thenReturn(credential(ApiTokenStatus.GENERATED));
        when(apiDeployOrderMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> rentalOrderService.payDeployFee(10L, "RO001"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RENTAL_ORDER_DEPLOY_FEE_NOT_PAYABLE);
        verify(walletService, never()).debit(any(), any(), any(), any(), any(), any());
    }

    @Test
    void startPausedOrderShouldSetRunningAndProfitWindow() {
        var paused = order(RentalOrderStatus.PAUSED);
        var running = order(RentalOrderStatus.RUNNING);
        var profitStartAt = java.time.LocalDateTime.now().minusHours(25);
        running.setProfitStatus(ProfitStatus.RUNNING.name());
        running.setStartedAt(profitStartAt);
        running.setProfitStartAt(profitStartAt);
        running.setProfitEndAt(profitStartAt.plusDays(30));
        when(rentalOrderMapper.selectOne(any(Wrapper.class))).thenReturn(paused, running);
        when(apiCredentialMapper.selectOne(any(Wrapper.class)))
                .thenReturn(credential(ApiTokenStatus.PAUSED), credential(ApiTokenStatus.ACTIVE));
        when(rentalOrderMapper.update(any(), any(Wrapper.class))).thenReturn(1);
        when(apiCredentialMapper.update(any(), any(Wrapper.class))).thenReturn(1);

        var response = rentalOrderService.startOrder(10L, "RO001");

        assertThat(response.orderStatus()).isEqualTo(RentalOrderStatus.RUNNING.name());
        assertThat(response.profitStatus()).isEqualTo(ProfitStatus.RUNNING.name());
        assertThat(response.profitStartAt()).isEqualTo(profitStartAt);
        assertThat(response.profitEndAt()).isEqualTo(profitStartAt.plusDays(30));
        verify(runSegmentService).openSegment(any(RentalOrder.class), any());
    }

    @Test
    void expiredPausedOrderShouldNotStart() {
        var paused = order(RentalOrderStatus.PAUSED);
        paused.setProfitStartAt(java.time.LocalDateTime.now().minusDays(30));
        paused.setProfitEndAt(java.time.LocalDateTime.now().minusMinutes(1));
        when(rentalOrderMapper.selectOne(any(Wrapper.class))).thenReturn(paused);
        when(apiCredentialMapper.selectOne(any(Wrapper.class))).thenReturn(credential(ApiTokenStatus.PAUSED));

        assertThatThrownBy(() -> rentalOrderService.startOrder(10L, "RO001"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RENTAL_ORDER_NOT_STARTABLE);
        verify(rentalOrderMapper, never()).update(any(), any(Wrapper.class));
        verify(runSegmentService, never()).openSegment(any(), any());
    }

    @Test
    void activatingOrderShouldNotStartDirectly() {
        when(rentalOrderMapper.selectOne(any(Wrapper.class))).thenReturn(order(RentalOrderStatus.ACTIVATING));

        assertThatThrownBy(() -> rentalOrderService.startOrder(10L, "RO001"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RENTAL_ORDER_NOT_STARTABLE);
        verify(apiCredentialMapper, never()).update(any(), any(Wrapper.class));
    }

    @Test
    void userShouldNotOperateOtherUsersOrder() {
        when(rentalOrderMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> rentalOrderService.payDeployFee(99L, "RO001"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RENTAL_ORDER_NOT_FOUND);
    }

    private void mockCatalog() {
        when(productMapper.selectById(1L)).thenReturn(product());
        when(aiModelMapper.selectById(2L)).thenReturn(aiModel());
        when(rentalCycleRuleMapper.selectById(3L)).thenReturn(cycleRule());
        when(regionMapper.selectById(4L)).thenReturn(region());
        when(gpuModelMapper.selectById(5L)).thenReturn(gpuModel());
    }

    private Product product() {
        var product = new Product();
        product.setId(1L);
        product.setProductCode("P001");
        product.setProductName("A100 Rental");
        product.setMachineCode("M001");
        product.setMachineAlias("A100-1");
        product.setRegionId(4L);
        product.setGpuModelId(5L);
        product.setGpuMemoryGb(80);
        product.setGpuPowerTops(new BigDecimal("312.0000"));
        product.setRentPrice(new BigDecimal("1000.00000000"));
        product.setTokenOutputPerDay(1000L);
        product.setStatus(CommonStatus.ENABLED.value());
        return product;
    }

    private AiModel aiModel() {
        var model = new AiModel();
        model.setId(2L);
        model.setModelName("GPT Test");
        model.setVendorName("OpenAI");
        model.setMonthlyTokenConsumptionTrillion(new BigDecimal("10.0000"));
        model.setTokenUnitPrice(new BigDecimal("0.01000000"));
        model.setDeployTechFee(new BigDecimal("100.00000000"));
        model.setStatus(CommonStatus.ENABLED.value());
        return model;
    }

    private RentalCycleRule cycleRule() {
        var rule = new RentalCycleRule();
        rule.setId(3L);
        rule.setCycleDays(30);
        rule.setYieldMultiplier(new BigDecimal("1.2000"));
        rule.setEarlyPenaltyRate(new BigDecimal("0.0100"));
        rule.setStatus(CommonStatus.ENABLED.value());
        return rule;
    }

    private Region region() {
        var region = new Region();
        region.setId(4L);
        region.setRegionName("Hong Kong");
        return region;
    }

    private GpuModel gpuModel() {
        var gpuModel = new GpuModel();
        gpuModel.setId(5L);
        gpuModel.setModelName("NVIDIA A100");
        return gpuModel;
    }

    private RentalOrder order(RentalOrderStatus status) {
        var order = new RentalOrder();
        order.setId(100L);
        order.setOrderNo("RO001");
        order.setUserId(10L);
        order.setProductId(1L);
        order.setAiModelId(2L);
        order.setCycleRuleId(3L);
        order.setProductCodeSnapshot("P001");
        order.setProductNameSnapshot("A100 Rental");
        order.setMachineCodeSnapshot("M001");
        order.setMachineAliasSnapshot("A100-1");
        order.setRegionNameSnapshot("Hong Kong");
        order.setGpuModelSnapshot("NVIDIA A100");
        order.setGpuMemorySnapshotGb(80);
        order.setGpuRentPriceSnapshot(new BigDecimal("1000.00000000"));
        order.setTokenOutputPerDaySnapshot(1000L);
        order.setAiModelNameSnapshot("GPT Test");
        order.setDeployFeeSnapshot(new BigDecimal("100.00000000"));
        order.setCycleDaysSnapshot(30);
        order.setYieldMultiplierSnapshot(new BigDecimal("1.2000"));
        order.setEarlyPenaltyRateSnapshot(new BigDecimal("0.0100"));
        order.setCurrency("USDT");
        order.setOrderAmount(new BigDecimal("1000.00000000"));
        order.setPaidAmount(status == RentalOrderStatus.PENDING_PAY
                ? BigDecimal.ZERO.setScale(8) : new BigDecimal("1000.00000000"));
        if (status != RentalOrderStatus.PENDING_PAY) {
            order.setPaidAt(LocalDateTime.now());
            order.setApiGeneratedAt(LocalDateTime.now());
        }
        order.setExpectedDailyProfit(new BigDecimal("12.00000000"));
        order.setExpectedTotalProfit(new BigDecimal("360.00000000"));
        order.setOrderStatus(status.name());
        order.setProfitStatus(ProfitStatus.NOT_STARTED.name());
        order.setSettlementStatus(RentalOrderSettlementStatus.UNSETTLED.name());
        return order;
    }

    private ApiCredential credential(ApiTokenStatus status) {
        var credential = new ApiCredential();
        credential.setId(200L);
        credential.setCredentialNo("AC001");
        credential.setUserId(10L);
        credential.setRentalOrderId(100L);
        credential.setApiName("GPT Test API");
        credential.setApiBaseUrl("https://api.example.invalid/v1");
        credential.setTokenMasked("sk-****abcdef");
        credential.setModelNameSnapshot("GPT Test");
        credential.setDeployFeeSnapshot(new BigDecimal("100.00000000"));
        credential.setTokenStatus(status.name());
        return credential;
    }

    private ApiDeployOrder deployOrder(ApiDeployOrderStatus status) {
        var deployOrder = new ApiDeployOrder();
        deployOrder.setId(300L);
        deployOrder.setDeployNo("AD001");
        deployOrder.setUserId(10L);
        deployOrder.setRentalOrderId(100L);
        deployOrder.setApiCredentialId(200L);
        deployOrder.setAiModelId(2L);
        deployOrder.setModelNameSnapshot("GPT Test");
        deployOrder.setCurrency("USDT");
        deployOrder.setDeployFeeAmount(new BigDecimal("100.00000000"));
        deployOrder.setStatus(status.name());
        deployOrder.setWalletTxNo(status == ApiDeployOrderStatus.PAID ? "WT001" : null);
        return deployOrder;
    }

    private WalletTransaction walletTransaction() {
        var transaction = new WalletTransaction();
        transaction.setTxNo("WT001");
        return transaction;
    }
}

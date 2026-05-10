package com.compute.rental.modules.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.compute.rental.common.enums.ApiDeployOrderStatus;
import com.compute.rental.common.enums.ApiTokenStatus;
import com.compute.rental.common.enums.CommonStatus;
import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.enums.ProfitStatus;
import com.compute.rental.common.enums.RentalOrderSettlementStatus;
import com.compute.rental.common.enums.RentalOrderStatus;
import com.compute.rental.common.enums.WalletBusinessType;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.common.page.PageParam;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.common.util.DateTimeUtils;
import com.compute.rental.common.util.MoneyUtils;
import com.compute.rental.common.util.RedisKeys;
import com.compute.rental.common.util.RedisLockClient;
import com.compute.rental.common.util.RedisLockClient.RedisLock;
import com.compute.rental.modules.order.dto.ApiCredentialResponse;
import com.compute.rental.modules.order.dto.ApiDeployInfoResponse;
import com.compute.rental.modules.order.dto.ApiDeployOrderResponse;
import com.compute.rental.modules.order.dto.CreateRentalOrderRequest;
import com.compute.rental.modules.order.dto.RentalOrderDetailResponse;
import com.compute.rental.modules.order.dto.RentalOrderQueryRequest;
import com.compute.rental.modules.order.dto.RentalOrderSummaryResponse;
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
import com.compute.rental.modules.user.entity.AppUser;
import com.compute.rental.modules.user.mapper.AppUserMapper;
import com.compute.rental.modules.wallet.service.WalletService;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class RentalOrderService {

    private static final String CURRENCY_USDT = "USDT";
    private static final String PAY_ACTION = "PAY";
    private static final String CANCEL_ACTION = "CANCEL";
    private static final Duration ORDER_OPERATION_LOCK_TTL = Duration.ofMinutes(1);

    private final RentalOrderMapper rentalOrderMapper;
    private final ApiDeployOrderMapper apiDeployOrderMapper;
    private final ApiCredentialMapper apiCredentialMapper;
    private final ProductMapper productMapper;
    private final AiModelMapper aiModelMapper;
    private final RentalCycleRuleMapper rentalCycleRuleMapper;
    private final RegionMapper regionMapper;
    private final GpuModelMapper gpuModelMapper;
    private final AppUserMapper appUserMapper;
    private final WalletService walletService;
    private final ApiTokenCryptoService apiTokenCryptoService;
    private final ApiTokenProperties apiTokenProperties;
    private final RedisLockClient redisLockClient;
    private final RentalOrderRunSegmentService runSegmentService;
    private final Duration autoPauseDelay;

    public RentalOrderService(
            RentalOrderMapper rentalOrderMapper,
            ApiDeployOrderMapper apiDeployOrderMapper,
            ApiCredentialMapper apiCredentialMapper,
            ProductMapper productMapper,
            AiModelMapper aiModelMapper,
            RentalCycleRuleMapper rentalCycleRuleMapper,
            RegionMapper regionMapper,
            GpuModelMapper gpuModelMapper,
            AppUserMapper appUserMapper,
            WalletService walletService,
            ApiTokenCryptoService apiTokenCryptoService,
            ApiTokenProperties apiTokenProperties,
            RedisLockClient redisLockClient,
            RentalOrderRunSegmentService runSegmentService,
            @Value("${app.order.auto-pause-delay:24h}") Duration autoPauseDelay
    ) {
        this.rentalOrderMapper = rentalOrderMapper;
        this.apiDeployOrderMapper = apiDeployOrderMapper;
        this.apiCredentialMapper = apiCredentialMapper;
        this.productMapper = productMapper;
        this.aiModelMapper = aiModelMapper;
        this.rentalCycleRuleMapper = rentalCycleRuleMapper;
        this.regionMapper = regionMapper;
        this.gpuModelMapper = gpuModelMapper;
        this.appUserMapper = appUserMapper;
        this.walletService = walletService;
        this.apiTokenCryptoService = apiTokenCryptoService;
        this.apiTokenProperties = apiTokenProperties;
        this.redisLockClient = redisLockClient;
        this.runSegmentService = runSegmentService;
        this.autoPauseDelay = autoPauseDelay == null ? Duration.ofHours(24) : autoPauseDelay;
    }

    @Transactional
    public RentalOrderDetailResponse createOrder(Long userId, CreateRentalOrderRequest request) {
        var product = requireEnabledProduct(request.productId());
        var aiModel = requireEnabledAiModel(request.aiModelId());
        var cycleRule = requireEnabledCycleRule(request.cycleRuleId());
        var now = DateTimeUtils.now();
        var order = buildOrder(userId, product, aiModel, cycleRule, now);
        rentalOrderMapper.insert(order);
        return toDetailResponse(order, null);
    }

    public PageResult<RentalOrderSummaryResponse> pageUserOrders(Long userId, RentalOrderQueryRequest request) {
        var page = new Page<RentalOrder>(request.current(), request.size());
        var wrapper = new LambdaQueryWrapper<RentalOrder>()
                .eq(RentalOrder::getUserId, userId)
                .eq(request.orderStatus() != null, RentalOrder::getOrderStatus,
                        request.orderStatus() == null ? null : request.orderStatus().name())
                .ge(request.startTime() != null, RentalOrder::getCreatedAt, request.startTime())
                .le(request.endTime() != null, RentalOrder::getCreatedAt, request.endTime())
                .orderByDesc(RentalOrder::getId);
        var result = rentalOrderMapper.selectPage(page, wrapper);
        var userNames = userNameMap(result.getRecords().stream().map(RentalOrder::getUserId).toList());
        return new PageResult<>(result.getRecords().stream()
                .map(order -> toSummaryResponse(order, userNames.get(order.getUserId())))
                .toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    public PageResult<ApiDeployInfoResponse> pageUserApiManagement(Long userId, PageParam request) {
        var page = new Page<ApiCredential>(request.current(), request.size());
        var wrapper = new LambdaQueryWrapper<ApiCredential>()
                .eq(ApiCredential::getUserId, userId)
                .orderByDesc(ApiCredential::getId);
        var result = apiCredentialMapper.selectPage(page, wrapper);
        var credentials = result.getRecords();
        if (credentials.isEmpty()) {
            return new PageResult<>(List.of(), result.getTotal(), result.getCurrent(), result.getSize());
        }

        var orderIds = credentials.stream().map(ApiCredential::getRentalOrderId).toList();
        var orderMap = new HashMap<Long, RentalOrder>();
        rentalOrderMapper.selectBatchIds(orderIds).forEach(order -> orderMap.put(order.getId(), order));

        var credentialIds = credentials.stream().map(ApiCredential::getId).toList();
        var deployOrderMap = new HashMap<Long, ApiDeployOrder>();
        apiDeployOrderMapper.selectList(new LambdaQueryWrapper<ApiDeployOrder>()
                        .in(ApiDeployOrder::getApiCredentialId, credentialIds))
                .forEach(deployOrder -> deployOrderMap.put(deployOrder.getApiCredentialId(), deployOrder));

        return new PageResult<>(credentials.stream()
                .map(credential -> toDeployInfoResponse(
                        orderMap.get(credential.getRentalOrderId()),
                        credential,
                        deployOrderMap.get(credential.getId())))
                .toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    public RentalOrderDetailResponse getUserOrder(Long userId, String orderNo) {
        var order = requireUserOrder(userId, orderNo);
        return toDetailResponse(order, findCredential(order.getId()));
    }

    @Transactional
    public RentalOrderDetailResponse payMachineFee(Long userId, String orderNo) {
        return withOrderOperationLock(orderNo, "machine-pay", () -> doPayMachineFee(userId, orderNo));
    }

    private RentalOrderDetailResponse doPayMachineFee(Long userId, String orderNo) {
        var order = requireUserOrder(userId, orderNo);
        if (!RentalOrderStatus.PENDING_PAY.name().equals(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.RENTAL_ORDER_NOT_PAYABLE);
        }
        var now = DateTimeUtils.now();
        var updated = rentalOrderMapper.update(null, new LambdaUpdateWrapper<RentalOrder>()
                .eq(RentalOrder::getId, order.getId())
                .eq(RentalOrder::getOrderStatus, RentalOrderStatus.PENDING_PAY.name())
                .set(RentalOrder::getOrderStatus, RentalOrderStatus.PENDING_ACTIVATION.name())
                .set(RentalOrder::getPaidAmount, order.getOrderAmount())
                .set(RentalOrder::getPaidAt, now)
                .set(RentalOrder::getApiGeneratedAt, now)
                .set(RentalOrder::getUpdatedAt, now));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CONCURRENT_UPDATE_FAILED, "租赁订单状态已变化");
        }

        var walletTx = walletService.debit(
                userId,
                order.getOrderAmount(),
                WalletBusinessType.RENT_PAY,
                order.getOrderNo(),
                PAY_ACTION,
                "租赁机器费用支付"
        );
        var credential = createCredentialIfAbsent(order, now);
        rentalOrderMapper.update(null, new LambdaUpdateWrapper<RentalOrder>()
                .eq(RentalOrder::getId, order.getId())
                .set(RentalOrder::getMachinePayTxNo, walletTx.getTxNo())
                .set(RentalOrder::getUpdatedAt, DateTimeUtils.now()));
        return getUserOrder(userId, orderNo);
    }

    @Transactional
    public RentalOrderDetailResponse cancelOrder(Long userId, String orderNo) {
        return withOrderOperationLock(orderNo, "cancel", () -> doCancelOrder(userId, orderNo));
    }

    private RentalOrderDetailResponse doCancelOrder(Long userId, String orderNo) {
        var order = requireUserOrder(userId, orderNo);
        if (RentalOrderStatus.PENDING_PAY.name().equals(order.getOrderStatus())) {
            cancelPendingPay(order);
            return getUserOrder(userId, orderNo);
        }
        if (RentalOrderStatus.PENDING_ACTIVATION.name().equals(order.getOrderStatus())) {
            cancelPendingActivation(order);
            return getUserOrder(userId, orderNo);
        }
        throw new BusinessException(ErrorCode.RENTAL_ORDER_NOT_CANCELABLE);
    }

    public ApiCredentialResponse getUserApiCredential(Long userId, String orderNo) {
        var order = requireUserOrder(userId, orderNo);
        var credential = findCredential(order.getId());
        if (credential == null) {
            throw new BusinessException(ErrorCode.API_CREDENTIAL_NOT_FOUND);
        }
        return toCredentialResponse(credential);
    }

    public ApiDeployInfoResponse getDeployInfo(Long userId, String orderNo) {
        var order = requireUserOrder(userId, orderNo);
        var credential = requireCredential(order.getId());
        return toDeployInfoResponse(order, credential, findDeployOrder(order.getId(), credential.getId()));
    }

    @Transactional
    public ApiDeployOrderResponse payDeployFee(Long userId, String orderNo) {
        return withOrderOperationLock(orderNo, "deploy-pay", () -> doPayDeployFee(userId, orderNo));
    }

    private ApiDeployOrderResponse doPayDeployFee(Long userId, String orderNo) {
        var order = requireUserOrder(userId, orderNo);
        var credential = requireCredential(order.getId());
        var existing = findDeployOrder(order.getId(), credential.getId());
        if (existing != null && ApiDeployOrderStatus.PAID.name().equals(existing.getStatus())) {
            return toDeployOrderResponse(order, credential, existing);
        }
        if (!RentalOrderStatus.PENDING_ACTIVATION.name().equals(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.RENTAL_ORDER_DEPLOY_FEE_NOT_PAYABLE);
        }
        if (!ApiTokenStatus.GENERATED.name().equals(credential.getTokenStatus())) {
            throw new BusinessException(ErrorCode.API_CREDENTIAL_NOT_GENERATED);
        }

        var now = DateTimeUtils.now();
        var autoPauseAt = now.plus(autoPauseDelay);
        var deployOrder = createPendingDeployOrder(order, credential, now);
        if (ApiDeployOrderStatus.PAID.name().equals(deployOrder.getStatus())) {
            return toDeployOrderResponse(order, credential, deployOrder);
        }
        var walletTx = walletService.debit(
                userId,
                deployOrder.getDeployFeeAmount(),
                WalletBusinessType.API_DEPLOY_FEE,
                deployOrder.getDeployNo(),
                PAY_ACTION,
                "API 部署费用支付"
        );
        var updatedDeploy = apiDeployOrderMapper.update(null, new LambdaUpdateWrapper<ApiDeployOrder>()
                .eq(ApiDeployOrder::getId, deployOrder.getId())
                .eq(ApiDeployOrder::getStatus, ApiDeployOrderStatus.PENDING_PAY.name())
                .set(ApiDeployOrder::getStatus, ApiDeployOrderStatus.PAID.name())
                .set(ApiDeployOrder::getWalletTxNo, walletTx.getTxNo())
                .set(ApiDeployOrder::getPaidAt, now)
                .set(ApiDeployOrder::getUpdatedAt, now));
        if (updatedDeploy == 0) {
            throw new BusinessException(ErrorCode.API_DEPLOY_ORDER_STATUS_CHANGED);
        }
        var updatedCredential = apiCredentialMapper.update(null, new LambdaUpdateWrapper<ApiCredential>()
                .eq(ApiCredential::getId, credential.getId())
                .eq(ApiCredential::getTokenStatus, ApiTokenStatus.GENERATED.name())
                .set(ApiCredential::getTokenStatus, ApiTokenStatus.ACTIVE.name())
                .set(ApiCredential::getActivationPaidAt, now)
                .set(ApiCredential::getActivatedAt, now)
                .set(ApiCredential::getAutoPauseAt, autoPauseAt)
                .set(ApiCredential::getStartedAt, now)
                .set(ApiCredential::getUpdatedAt, now));
        if (updatedCredential == 0) {
            throw new BusinessException(ErrorCode.API_CREDENTIAL_STATUS_CHANGED);
        }
        var profitEndAt = now.plusDays(order.getCycleDaysSnapshot());
        var updatedOrder = rentalOrderMapper.update(null, new LambdaUpdateWrapper<RentalOrder>()
                .eq(RentalOrder::getId, order.getId())
                .eq(RentalOrder::getOrderStatus, RentalOrderStatus.PENDING_ACTIVATION.name())
                .set(RentalOrder::getOrderStatus, RentalOrderStatus.RUNNING.name())
                .set(RentalOrder::getProfitStatus, ProfitStatus.RUNNING.name())
                .set(RentalOrder::getDeployFeePaidAt, now)
                .set(RentalOrder::getActivatedAt, now)
                .set(RentalOrder::getAutoPauseAt, autoPauseAt)
                .set(RentalOrder::getStartedAt, now)
                .set(RentalOrder::getProfitStartAt, now)
                .set(RentalOrder::getProfitEndAt, profitEndAt)
                .set(RentalOrder::getUpdatedAt, now));
        if (updatedOrder == 0) {
            throw new BusinessException(ErrorCode.CONCURRENT_UPDATE_FAILED, "租赁订单状态已变化");
        }
        runSegmentService.openSegment(order, now);
        // TODO: create sys_notification API_ACTIVATED after notification service is implemented.

        var paidDeployOrder = findDeployOrder(order.getId(), credential.getId());
        return toDeployOrderResponse(order, credential, paidDeployOrder == null ? deployOrder : paidDeployOrder);
    }

    public ApiDeployOrderResponse getDeployOrder(Long userId, String orderNo) {
        var order = requireUserOrder(userId, orderNo);
        var credential = requireCredential(order.getId());
        var deployOrder = findDeployOrder(order.getId(), credential.getId());
        if (deployOrder == null) {
            throw new BusinessException(ErrorCode.API_DEPLOY_ORDER_NOT_FOUND);
        }
        return toDeployOrderResponse(order, credential, deployOrder);
    }

    @Transactional
    public RentalOrderDetailResponse startOrder(Long userId, String orderNo) {
        return withOrderOperationLock(orderNo, "start", () -> doStartOrder(userId, orderNo));
    }

    private RentalOrderDetailResponse doStartOrder(Long userId, String orderNo) {
        var order = requireUserOrder(userId, orderNo);
        if (!RentalOrderStatus.PAUSED.name().equals(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.RENTAL_ORDER_NOT_STARTABLE);
        }
        var credential = requireCredential(order.getId());
        if (!ApiTokenStatus.PAUSED.name().equals(credential.getTokenStatus())) {
            throw new BusinessException(ErrorCode.API_CREDENTIAL_NOT_PAUSED);
        }
        var now = DateTimeUtils.now();
        if (order.getProfitEndAt() != null && !order.getProfitEndAt().isAfter(now)) {
            throw new BusinessException(ErrorCode.RENTAL_ORDER_NOT_STARTABLE, "订单已到期，等待系统结算");
        }
        var profitStartAt = order.getProfitStartAt() == null ? now : order.getProfitStartAt();
        var profitEndAt = order.getProfitEndAt() == null ? profitStartAt.plusDays(order.getCycleDaysSnapshot()) : order.getProfitEndAt();
        var updatedOrder = rentalOrderMapper.update(null, new LambdaUpdateWrapper<RentalOrder>()
                .eq(RentalOrder::getId, order.getId())
                .eq(RentalOrder::getOrderStatus, RentalOrderStatus.PAUSED.name())
                .set(RentalOrder::getOrderStatus, RentalOrderStatus.RUNNING.name())
                .set(RentalOrder::getProfitStatus, ProfitStatus.RUNNING.name())
                .set(order.getStartedAt() == null, RentalOrder::getStartedAt, now)
                .set(order.getProfitStartAt() == null, RentalOrder::getProfitStartAt, profitStartAt)
                .set(order.getProfitEndAt() == null, RentalOrder::getProfitEndAt, profitEndAt)
                .set(RentalOrder::getUpdatedAt, now));
        if (updatedOrder == 0) {
            throw new BusinessException(ErrorCode.CONCURRENT_UPDATE_FAILED, "租赁订单状态已变化");
        }
        var updatedCredential = apiCredentialMapper.update(null, new LambdaUpdateWrapper<ApiCredential>()
                .eq(ApiCredential::getId, credential.getId())
                .eq(ApiCredential::getTokenStatus, ApiTokenStatus.PAUSED.name())
                .set(ApiCredential::getTokenStatus, ApiTokenStatus.ACTIVE.name())
                .set(credential.getStartedAt() == null, ApiCredential::getStartedAt, now)
                .set(ApiCredential::getUpdatedAt, now));
        if (updatedCredential == 0) {
            throw new BusinessException(ErrorCode.API_CREDENTIAL_STATUS_CHANGED);
        }
        runSegmentService.openSegment(order, now);
        return getUserOrder(userId, orderNo);
    }

    private <T> T withOrderOperationLock(String orderNo, String operation, Supplier<T> action) {
        var lockKey = RedisKeys.orderOperationLock(orderNo, operation);
        var lock = redisLockClient.tryLock(lockKey, ORDER_OPERATION_LOCK_TTL);
        if (lock.isEmpty()) {
            throw new BusinessException(ErrorCode.RENTAL_ORDER_PROCESSING);
        }
        var releaseRegistered = registerUnlockAfterTransaction(lock.get());
        try {
            return action.get();
        } finally {
            if (!releaseRegistered) {
                redisLockClient.unlock(lock.get());
            }
        }
    }

    private boolean registerUnlockAfterTransaction(RedisLock lock) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return false;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                redisLockClient.unlock(lock);
            }
        });
        return true;
    }

    private void cancelPendingPay(RentalOrder order) {
        var updated = rentalOrderMapper.update(null, new LambdaUpdateWrapper<RentalOrder>()
                .eq(RentalOrder::getId, order.getId())
                .eq(RentalOrder::getOrderStatus, RentalOrderStatus.PENDING_PAY.name())
                .set(RentalOrder::getOrderStatus, RentalOrderStatus.CANCELED.name())
                .set(RentalOrder::getCanceledAt, DateTimeUtils.now())
                .set(RentalOrder::getUpdatedAt, DateTimeUtils.now()));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CONCURRENT_UPDATE_FAILED, "租赁订单状态已变化");
        }
    }

    private void cancelPendingActivation(RentalOrder order) {
        var now = DateTimeUtils.now();
        var updated = rentalOrderMapper.update(null, new LambdaUpdateWrapper<RentalOrder>()
                .eq(RentalOrder::getId, order.getId())
                .eq(RentalOrder::getOrderStatus, RentalOrderStatus.PENDING_ACTIVATION.name())
                .set(RentalOrder::getOrderStatus, RentalOrderStatus.CANCELED.name())
                .set(RentalOrder::getCanceledAt, now)
                .set(RentalOrder::getUpdatedAt, now));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CONCURRENT_UPDATE_FAILED, "租赁订单状态已变化");
        }
        walletService.credit(
                order.getUserId(),
                order.getOrderAmount(),
                WalletBusinessType.REFUND,
                order.getOrderNo(),
                CANCEL_ACTION,
                "租赁订单取消退款"
        );
        apiCredentialMapper.update(null, new LambdaUpdateWrapper<ApiCredential>()
                .eq(ApiCredential::getRentalOrderId, order.getId())
                .ne(ApiCredential::getTokenStatus, ApiTokenStatus.REVOKED.name())
                .set(ApiCredential::getTokenStatus, ApiTokenStatus.REVOKED.name())
                .set(ApiCredential::getRevokedAt, now)
                .set(ApiCredential::getUpdatedAt, now));
        // TODO: create sys_notification ORDER_CANCELED after notification service is implemented.
    }

    private RentalOrder buildOrder(Long userId, Product product, AiModel aiModel, RentalCycleRule cycleRule,
                                   java.time.LocalDateTime now) {
        var tokenOutputPerDay = product.getTokenOutputPerDay() == null ? 0L : product.getTokenOutputPerDay();
        var expectedDailyProfit = MoneyUtils.scale(BigDecimal.valueOf(tokenOutputPerDay)
                .multiply(aiModel.getTokenUnitPrice())
                .multiply(cycleRule.getYieldMultiplier()));
        var expectedTotalProfit = MoneyUtils.scale(expectedDailyProfit.multiply(BigDecimal.valueOf(cycleRule.getCycleDays())));
        var order = new RentalOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setProductId(product.getId());
        order.setAiModelId(aiModel.getId());
        order.setCycleRuleId(cycleRule.getId());
        order.setProductCodeSnapshot(product.getProductCode());
        order.setProductNameSnapshot(product.getProductName());
        order.setMachineCodeSnapshot(product.getMachineCode());
        order.setMachineAliasSnapshot(product.getMachineAlias());
        order.setRegionNameSnapshot(requireRegionName(product.getRegionId()));
        order.setGpuModelSnapshot(requireGpuModelName(product.getGpuModelId()));
        order.setGpuMemorySnapshotGb(product.getGpuMemoryGb());
        order.setGpuPowerTopsSnapshot(product.getGpuPowerTops());
        order.setGpuRentPriceSnapshot(product.getRentPrice());
        order.setTokenOutputPerDaySnapshot(tokenOutputPerDay);
        order.setAiModelNameSnapshot(aiModel.getModelName());
        order.setAiVendorNameSnapshot(aiModel.getVendorName());
        order.setMonthlyTokenConsumptionSnapshot(aiModel.getMonthlyTokenConsumptionTrillion());
        order.setTokenUnitPriceSnapshot(aiModel.getTokenUnitPrice());
        order.setDeployFeeSnapshot(aiModel.getDeployTechFee());
        order.setCycleDaysSnapshot(cycleRule.getCycleDays());
        order.setYieldMultiplierSnapshot(cycleRule.getYieldMultiplier());
        order.setEarlyPenaltyRateSnapshot(cycleRule.getEarlyPenaltyRate());
        order.setCurrency(CURRENCY_USDT);
        order.setOrderAmount(MoneyUtils.scale(product.getRentPrice()));
        order.setPaidAmount(MoneyUtils.ZERO);
        order.setExpectedDailyProfit(expectedDailyProfit);
        order.setExpectedTotalProfit(expectedTotalProfit);
        order.setOrderStatus(RentalOrderStatus.PENDING_PAY.name());
        order.setProfitStatus(ProfitStatus.NOT_STARTED.name());
        order.setSettlementStatus(RentalOrderSettlementStatus.UNSETTLED.name());
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        return order;
    }

    private ApiCredential createCredentialIfAbsent(RentalOrder order, java.time.LocalDateTime now) {
        var existing = findCredential(order.getId());
        if (existing != null) {
            return existing;
        }
        var plaintextToken = generatePlainToken();
        var credential = new ApiCredential();
        credential.setCredentialNo(generateCredentialNo());
        credential.setUserId(order.getUserId());
        credential.setRentalOrderId(order.getId());
        credential.setApiName(order.getAiModelNameSnapshot() + " API");
        credential.setApiBaseUrl(apiTokenProperties.baseUrl());
        credential.setTokenCiphertext(apiTokenCryptoService.encrypt(plaintextToken));
        credential.setTokenMasked(maskToken(plaintextToken));
        credential.setModelNameSnapshot(order.getAiModelNameSnapshot());
        credential.setDeployFeeSnapshot(order.getDeployFeeSnapshot());
        credential.setTokenStatus(ApiTokenStatus.GENERATED.name());
        credential.setGeneratedAt(now);
        credential.setMockRequestCount(0L);
        credential.setMockTokenDisplay(0L);
        credential.setCreatedAt(now);
        credential.setUpdatedAt(now);
        try {
            apiCredentialMapper.insert(credential);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.API_CREDENTIAL_ALREADY_EXISTS);
        }
        return credential;
    }

    private Product requireEnabledProduct(Long productId) {
        var product = productMapper.selectById(productId);
        if (product == null || !Integer.valueOf(CommonStatus.ENABLED.value()).equals(product.getStatus())) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return product;
    }

    private AiModel requireEnabledAiModel(Long aiModelId) {
        var aiModel = aiModelMapper.selectById(aiModelId);
        if (aiModel == null || !Integer.valueOf(CommonStatus.ENABLED.value()).equals(aiModel.getStatus())) {
            throw new BusinessException(ErrorCode.AI_MODEL_NOT_FOUND);
        }
        return aiModel;
    }

    private RentalCycleRule requireEnabledCycleRule(Long cycleRuleId) {
        var cycleRule = rentalCycleRuleMapper.selectById(cycleRuleId);
        if (cycleRule == null || !Integer.valueOf(CommonStatus.ENABLED.value()).equals(cycleRule.getStatus())) {
            throw new BusinessException(ErrorCode.RENTAL_CYCLE_RULE_NOT_FOUND);
        }
        return cycleRule;
    }

    private String requireRegionName(Long regionId) {
        var region = regionMapper.selectById(regionId);
        if (region == null) {
            throw new BusinessException(ErrorCode.PRODUCT_REGION_DISABLED);
        }
        return region.getRegionName();
    }

    private String requireGpuModelName(Long gpuModelId) {
        var gpuModel = gpuModelMapper.selectById(gpuModelId);
        if (gpuModel == null) {
            throw new BusinessException(ErrorCode.PRODUCT_GPU_DISABLED);
        }
        return gpuModel.getModelName();
    }

    private RentalOrder requireUserOrder(Long userId, String orderNo) {
        var order = rentalOrderMapper.selectOne(new LambdaQueryWrapper<RentalOrder>()
                .eq(RentalOrder::getUserId, userId)
                .eq(RentalOrder::getOrderNo, orderNo)
                .last("LIMIT 1"));
        if (order == null) {
            throw new BusinessException(ErrorCode.RENTAL_ORDER_NOT_FOUND);
        }
        return order;
    }

    private ApiCredential findCredential(Long rentalOrderId) {
        return apiCredentialMapper.selectOne(new LambdaQueryWrapper<ApiCredential>()
                .eq(ApiCredential::getRentalOrderId, rentalOrderId)
                .last("LIMIT 1"));
    }

    private ApiCredential requireCredential(Long rentalOrderId) {
        var credential = findCredential(rentalOrderId);
        if (credential == null) {
            throw new BusinessException(ErrorCode.API_CREDENTIAL_NOT_FOUND);
        }
        return credential;
    }

    private ApiDeployOrder createPendingDeployOrder(RentalOrder order, ApiCredential credential,
                                                    java.time.LocalDateTime now) {
        var deployOrder = new ApiDeployOrder();
        deployOrder.setDeployNo(generateDeployNo());
        deployOrder.setUserId(order.getUserId());
        deployOrder.setRentalOrderId(order.getId());
        deployOrder.setApiCredentialId(credential.getId());
        deployOrder.setAiModelId(order.getAiModelId());
        deployOrder.setModelNameSnapshot(order.getAiModelNameSnapshot());
        deployOrder.setCurrency(CURRENCY_USDT);
        deployOrder.setDeployFeeAmount(MoneyUtils.scale(credential.getDeployFeeSnapshot()));
        deployOrder.setStatus(ApiDeployOrderStatus.PENDING_PAY.name());
        deployOrder.setCreatedAt(now);
        deployOrder.setUpdatedAt(now);
        try {
            apiDeployOrderMapper.insert(deployOrder);
        } catch (DuplicateKeyException ex) {
            var existing = findDeployOrder(order.getId(), credential.getId());
            if (existing != null && ApiDeployOrderStatus.PAID.name().equals(existing.getStatus())) {
                return existing;
            }
            throw new BusinessException(ErrorCode.API_DEPLOY_ORDER_ALREADY_EXISTS);
        }
        return deployOrder;
    }

    private ApiDeployOrder findDeployOrder(Long rentalOrderId, Long apiCredentialId) {
        return apiDeployOrderMapper.selectOne(new LambdaQueryWrapper<ApiDeployOrder>()
                .eq(ApiDeployOrder::getRentalOrderId, rentalOrderId)
                .eq(ApiDeployOrder::getApiCredentialId, apiCredentialId)
                .last("LIMIT 1"));
    }

    private RentalOrderSummaryResponse toSummaryResponse(RentalOrder order) {
        return toSummaryResponse(order, userName(order.getUserId()));
    }

    private RentalOrderSummaryResponse toSummaryResponse(RentalOrder order, String userName) {
        return new RentalOrderSummaryResponse(
                order.getOrderNo(),
                userName,
                order.getProductNameSnapshot(),
                order.getMachineCodeSnapshot(),
                order.getMachineAliasSnapshot(),
                order.getAiModelNameSnapshot(),
                order.getCycleDaysSnapshot(),
                order.getOrderAmount(),
                order.getExpectedDailyProfit(),
                order.getExpectedTotalProfit(),
                order.getOrderStatus(),
                order.getProfitStatus(),
                order.getSettlementStatus(),
                order.getCreatedAt(),
                order.getPaidAt(),
                order.getApiGeneratedAt(),
                order.getDeployFeePaidAt(),
                order.getActivatedAt(),
                order.getAutoPauseAt(),
                order.getPausedAt(),
                order.getStartedAt(),
                order.getProfitStartAt(),
                order.getProfitEndAt()
        );
    }

    private RentalOrderDetailResponse toDetailResponse(RentalOrder order, ApiCredential credential) {
        return new RentalOrderDetailResponse(
                order.getOrderNo(),
                userName(order.getUserId()),
                order.getProductId(),
                order.getAiModelId(),
                order.getCycleRuleId(),
                order.getProductCodeSnapshot(),
                order.getProductNameSnapshot(),
                order.getMachineCodeSnapshot(),
                order.getMachineAliasSnapshot(),
                order.getRegionNameSnapshot(),
                order.getGpuModelSnapshot(),
                order.getGpuMemorySnapshotGb(),
                order.getGpuPowerTopsSnapshot(),
                order.getGpuRentPriceSnapshot(),
                order.getTokenOutputPerDaySnapshot(),
                order.getAiModelNameSnapshot(),
                order.getAiVendorNameSnapshot(),
                order.getMonthlyTokenConsumptionSnapshot(),
                order.getTokenUnitPriceSnapshot(),
                order.getDeployFeeSnapshot(),
                order.getCycleDaysSnapshot(),
                order.getYieldMultiplierSnapshot(),
                order.getEarlyPenaltyRateSnapshot(),
                order.getCurrency(),
                order.getOrderAmount(),
                order.getPaidAmount(),
                order.getExpectedDailyProfit(),
                order.getExpectedTotalProfit(),
                order.getOrderStatus(),
                order.getProfitStatus(),
                order.getSettlementStatus(),
                order.getMachinePayTxNo(),
                order.getPaidAt(),
                order.getApiGeneratedAt(),
                order.getDeployFeePaidAt(),
                order.getActivatedAt(),
                order.getAutoPauseAt(),
                order.getPausedAt(),
                order.getStartedAt(),
                order.getProfitStartAt(),
                order.getProfitEndAt(),
                order.getExpiredAt(),
                order.getCanceledAt(),
                order.getFinishedAt(),
                order.getCreatedAt(),
                credential == null ? null : toCredentialResponse(credential)
        );
    }

    private ApiCredentialResponse toCredentialResponse(ApiCredential credential) {
        return new ApiCredentialResponse(
                credential.getCredentialNo(),
                credential.getApiName(),
                credential.getApiBaseUrl(),
                credential.getTokenMasked(),
                credential.getModelNameSnapshot(),
                credential.getDeployFeeSnapshot(),
                credential.getTokenStatus(),
                credential.getGeneratedAt(),
                credential.getActivationPaidAt(),
                credential.getActivatedAt(),
                credential.getAutoPauseAt(),
                credential.getPausedAt(),
                credential.getStartedAt(),
                credential.getExpiredAt()
        );
    }

    private ApiDeployInfoResponse toDeployInfoResponse(
            RentalOrder order,
            ApiCredential credential,
            ApiDeployOrder deployOrder
    ) {
        return new ApiDeployInfoResponse(
                order.getOrderNo(),
                order.getOrderStatus(),
                credential.getCredentialNo(),
                credential.getTokenStatus(),
                credential.getModelNameSnapshot(),
                credential.getDeployFeeSnapshot(),
                credential.getApiName(),
                credential.getApiBaseUrl(),
                credential.getTokenMasked(),
                deployOrder == null ? null : deployOrder.getStatus(),
                deployOrder == null ? null : deployOrder.getPaidAt()
        );
    }

    private ApiDeployOrderResponse toDeployOrderResponse(
            RentalOrder order,
            ApiCredential credential,
            ApiDeployOrder deployOrder
    ) {
        return new ApiDeployOrderResponse(
                deployOrder.getDeployNo(),
                userName(deployOrder.getUserId()),
                order.getOrderNo(),
                credential.getCredentialNo(),
                deployOrder.getModelNameSnapshot(),
                deployOrder.getDeployFeeAmount(),
                deployOrder.getStatus(),
                deployOrder.getWalletTxNo(),
                deployOrder.getPaidAt(),
                deployOrder.getCreatedAt()
        );
    }

    private String generateOrderNo() {
        return "RO" + DateTimeUtils.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + randomSuffix(8);
    }

    private String userName(Long userId) {
        var user = userId == null ? null : appUserMapper.selectById(userId);
        return user == null ? null : user.getUserName();
    }

    private Map<Long, String> userNameMap(List<Long> userIds) {
        var ids = userIds.stream().filter(id -> id != null).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        var userNames = new HashMap<Long, String>();
        for (var user : appUserMapper.selectBatchIds(ids)) {
            userNames.put(user.getId(), user.getUserName());
        }
        return userNames;
    }

    private String generateCredentialNo() {
        return "AC" + DateTimeUtils.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + randomSuffix(8);
    }

    private String generateDeployNo() {
        return "AD" + DateTimeUtils.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + randomSuffix(8);
    }

    private String generatePlainToken() {
        return "sk-" + UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String maskToken(String token) {
        return token.substring(0, 3) + "****" + token.substring(token.length() - 6);
    }

    private String randomSuffix(int length) {
        return UUID.randomUUID().toString().replace("-", "").substring(0, length).toUpperCase(Locale.ROOT);
    }
}

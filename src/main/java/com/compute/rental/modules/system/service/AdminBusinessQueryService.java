package com.compute.rental.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.compute.rental.common.enums.ApiTokenStatus;
import com.compute.rental.common.enums.CommonStatus;
import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.enums.ProfitStatus;
import com.compute.rental.common.enums.RentalOrderStatus;
import com.compute.rental.common.enums.RecordSettleStatus;
import com.compute.rental.common.enums.RunSegmentCloseReason;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.common.util.DateTimeUtils;
import com.compute.rental.common.util.WalletRemarkUtils;
import com.compute.rental.modules.commission.entity.CommissionRecord;
import com.compute.rental.modules.commission.mapper.CommissionRecordMapper;
import com.compute.rental.modules.order.entity.ApiCredential;
import com.compute.rental.modules.order.entity.ApiDeployOrder;
import com.compute.rental.modules.order.entity.RentalOrder;
import com.compute.rental.modules.order.entity.RentalProfitRecord;
import com.compute.rental.modules.order.entity.RentalSettlementOrder;
import com.compute.rental.modules.order.mapper.ApiCredentialMapper;
import com.compute.rental.modules.order.mapper.ApiDeployOrderMapper;
import com.compute.rental.modules.order.mapper.RentalOrderMapper;
import com.compute.rental.modules.order.mapper.RentalProfitRecordMapper;
import com.compute.rental.modules.order.mapper.RentalSettlementOrderMapper;
import com.compute.rental.modules.order.service.RentalOrderRunSegmentService;
import com.compute.rental.modules.system.dto.AdminApiCredentialResponse;
import com.compute.rental.modules.system.dto.AdminApiDeployOrderResponse;
import com.compute.rental.modules.system.dto.AdminCommissionRecordResponse;
import com.compute.rental.modules.system.dto.AdminLogResponse;
import com.compute.rental.modules.system.dto.AdminProfitRecordResponse;
import com.compute.rental.modules.system.dto.AdminRentalOrderDetailResponse;
import com.compute.rental.modules.system.dto.AdminRentalOrderResponse;
import com.compute.rental.modules.system.dto.AdminSettlementOrderResponse;
import com.compute.rental.modules.system.dto.AdminTeamLeaderboardRow;
import com.compute.rental.modules.system.dto.AdminTeamListRow;
import com.compute.rental.modules.system.dto.AdminTeamMemberRow;
import com.compute.rental.modules.system.dto.AdminTeamOverviewResponse;
import com.compute.rental.modules.system.dto.AdminTeamRelationResponse;
import com.compute.rental.modules.system.dto.AdminTeamTreeNode;
import com.compute.rental.modules.system.dto.AdminTeamUserSummaryResponse;
import com.compute.rental.modules.system.dto.AdminUserResponse;
import com.compute.rental.modules.system.dto.AdminUserTeamResponse;
import com.compute.rental.modules.system.dto.AdminWalletResponse;
import com.compute.rental.modules.system.dto.AdminWalletTransactionResponse;
import com.compute.rental.modules.system.entity.SysAdminLog;
import com.compute.rental.modules.system.mapper.SysAdminLogMapper;
import com.compute.rental.modules.user.entity.AppUser;
import com.compute.rental.modules.user.entity.UserTeamRelation;
import com.compute.rental.modules.user.mapper.AppUserMapper;
import com.compute.rental.modules.user.mapper.UserTeamRelationMapper;
import com.compute.rental.modules.user.support.AppUserSearchSupport;
import com.compute.rental.modules.wallet.entity.UserWallet;
import com.compute.rental.modules.wallet.entity.WalletTransaction;
import com.compute.rental.modules.wallet.mapper.UserWalletMapper;
import com.compute.rental.modules.wallet.mapper.WalletTransactionMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminBusinessQueryService {

    private static final int MAX_HIERARCHY_LEVEL = 2;
    private static final String CURRENCY_USDT = "USDT";
    private static final String ORDER_STATUS_NONE = "NONE";
    private static final Set<String> ACTIVE_ORDER_STATUSES = Set.of(
            RentalOrderStatus.RUNNING.name(),
            RentalOrderStatus.PAUSED.name()
    );

    private final AppUserMapper appUserMapper;
    private final UserWalletMapper userWalletMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final RentalOrderMapper rentalOrderMapper;
    private final ApiCredentialMapper apiCredentialMapper;
    private final ApiDeployOrderMapper apiDeployOrderMapper;
    private final RentalProfitRecordMapper profitRecordMapper;
    private final RentalSettlementOrderMapper settlementOrderMapper;
    private final CommissionRecordMapper commissionRecordMapper;
    private final UserTeamRelationMapper teamRelationMapper;
    private final SysAdminLogMapper adminLogMapper;
    private final AdminLogService adminLogService;
    private final RentalOrderRunSegmentService runSegmentService;

    public AdminBusinessQueryService(
            AppUserMapper appUserMapper,
            UserWalletMapper userWalletMapper,
            WalletTransactionMapper walletTransactionMapper,
            RentalOrderMapper rentalOrderMapper,
            ApiCredentialMapper apiCredentialMapper,
            ApiDeployOrderMapper apiDeployOrderMapper,
            RentalProfitRecordMapper profitRecordMapper,
            RentalSettlementOrderMapper settlementOrderMapper,
            CommissionRecordMapper commissionRecordMapper,
            UserTeamRelationMapper teamRelationMapper,
            SysAdminLogMapper adminLogMapper,
            AdminLogService adminLogService,
            RentalOrderRunSegmentService runSegmentService
    ) {
        this.appUserMapper = appUserMapper;
        this.userWalletMapper = userWalletMapper;
        this.walletTransactionMapper = walletTransactionMapper;
        this.rentalOrderMapper = rentalOrderMapper;
        this.apiCredentialMapper = apiCredentialMapper;
        this.apiDeployOrderMapper = apiDeployOrderMapper;
        this.profitRecordMapper = profitRecordMapper;
        this.settlementOrderMapper = settlementOrderMapper;
        this.commissionRecordMapper = commissionRecordMapper;
        this.teamRelationMapper = teamRelationMapper;
        this.adminLogMapper = adminLogMapper;
        this.adminLogService = adminLogService;
        this.runSegmentService = runSegmentService;
    }

    public PageResult<AdminUserResponse> pageUsers(
            long pageNo,
            long pageSize,
            String keyword,
            String email,
            String userId,
            Integer status,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        var page = new Page<AppUser>(pageNo, pageSize);
        var normalizedKeyword = AppUserSearchSupport.normalize(keyword);
        var normalizedEmail = AppUserSearchSupport.normalize(email);
        var normalizedUserId = AppUserSearchSupport.normalize(userId);
        var wrapper = new LambdaQueryWrapper<AppUser>()
                .and(AppUserSearchSupport.hasText(normalizedKeyword),
                        userWrapper -> AppUserSearchSupport.applyKeyword(userWrapper, normalizedKeyword))
                .eq(AppUserSearchSupport.hasText(normalizedUserId), AppUser::getUserId, normalizedUserId)
                .likeRight(AppUserSearchSupport.hasText(normalizedEmail), AppUser::getEmail, normalizedEmail)
                .eq(status != null, AppUser::getStatus, status)
                .ge(startTime != null, AppUser::getCreatedAt, startTime)
                .le(endTime != null, AppUser::getCreatedAt, endTime)
                .orderByDesc(AppUser::getId);
        var result = appUserMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getRecords().stream().map(this::userResponse).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    public AdminUserResponse getUser(Long id) {
        return userResponse(requireUser(id));
    }

    @Transactional
    public AdminUserResponse disableUser(Long id, Long adminId, String ip) {
        var user = requireUser(id);
        var now = DateTimeUtils.now();
        appUserMapper.update(null, new LambdaUpdateWrapper<AppUser>()
                .eq(AppUser::getId, id)
                .set(AppUser::getStatus, 0)
                .set(AppUser::getUpdatedAt, now));
        var runningOrders = rentalOrderMapper.selectList(new LambdaQueryWrapper<RentalOrder>()
                .eq(RentalOrder::getUserId, id)
                .eq(RentalOrder::getOrderStatus, RentalOrderStatus.RUNNING.name()));
        for (var order : runningOrders) {
            var updatedOrder = rentalOrderMapper.update(null, new LambdaUpdateWrapper<RentalOrder>()
                    .eq(RentalOrder::getId, order.getId())
                    .eq(RentalOrder::getOrderStatus, RentalOrderStatus.RUNNING.name())
                    .set(RentalOrder::getOrderStatus, RentalOrderStatus.PAUSED.name())
                    .set(RentalOrder::getProfitStatus, ProfitStatus.PAUSED.name())
                    .set(RentalOrder::getPausedAt, now)
                    .set(RentalOrder::getUpdatedAt, now));
            if (updatedOrder > 0) {
                runSegmentService.closeOpenSegment(order.getId(), now, RunSegmentCloseReason.ADMIN_DISABLE);
            }
            apiCredentialMapper.update(null, new LambdaUpdateWrapper<ApiCredential>()
                    .eq(ApiCredential::getRentalOrderId, order.getId())
                    .set(ApiCredential::getTokenStatus, ApiTokenStatus.PAUSED.name())
                    .set(ApiCredential::getPausedAt, now)
                    .set(ApiCredential::getUpdatedAt, now));
        }
        adminLogService.log(adminId, "BAN_USER", "app_user", id, null, "status=0",
                "Paused running orders: " + runningOrders.size(), ip);
        return getUser(id);
    }

    @Transactional
    public AdminUserResponse enableUser(Long id, Long adminId, String ip) {
        requireUser(id);
        throw new BusinessException(ErrorCode.USER_REENABLE_NOT_ALLOWED);
    }

    public PageResult<AdminWalletResponse> pageWallets(
            long pageNo,
            long pageSize,
            String keyword,
            Long userId,
            String walletNo
    ) {
        var keywordUserIds = userIdsByKeyword(keyword);
        if (hasText(keyword) && keywordUserIds.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0, pageNo, pageSize);
        }
        var page = new Page<UserWallet>(pageNo, pageSize);
        var wrapper = new LambdaQueryWrapper<UserWallet>()
                .eq(userId != null, UserWallet::getUserId, userId)
                .in(!keywordUserIds.isEmpty(), UserWallet::getUserId, keywordUserIds)
                .eq(hasText(walletNo), UserWallet::getWalletNo, walletNo)
                .orderByDesc(UserWallet::getId);
        var result = userWalletMapper.selectPage(page, wrapper);
        var users = userBriefMap(result.getRecords().stream().map(UserWallet::getUserId).toList());
        return new PageResult<>(result.getRecords().stream()
                .map(wallet -> walletResponse(wallet, users.get(wallet.getUserId())))
                .toList(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public AdminWalletResponse getWalletByUser(Long userId) {
        var wallet = userWalletMapper.selectOne(new LambdaQueryWrapper<UserWallet>()
                .eq(UserWallet::getUserId, userId)
                .last("LIMIT 1"));
        if (wallet == null) {
            throw new BusinessException(ErrorCode.WALLET_NOT_FOUND);
        }
        var user = wallet.getUserId() == null ? null : appUserMapper.selectById(wallet.getUserId());
        return walletResponse(wallet, user == null ? null : new UserBrief(user.getUserName(), user.getEmail()));
    }

    public PageResult<AdminWalletTransactionResponse> pageWalletTransactions(
            long pageNo,
            long pageSize,
            String keyword,
            Long userId,
            String walletNo,
            String txType,
            String bizType,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        var keywordUserIds = userIdsByKeyword(keyword);
        if (hasText(keyword) && keywordUserIds.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0, pageNo, pageSize);
        }
        Long walletId = null;
        if (hasText(walletNo)) {
            var wallet = userWalletMapper.selectOne(new LambdaQueryWrapper<UserWallet>()
                    .eq(UserWallet::getWalletNo, walletNo)
                    .last("LIMIT 1"));
            if (wallet == null) {
                return new PageResult<>(Collections.emptyList(), 0, pageNo, pageSize);
            }
            walletId = wallet.getId();
        }
        var page = new Page<WalletTransaction>(pageNo, pageSize);
        var wrapper = new LambdaQueryWrapper<WalletTransaction>()
                .eq(userId != null, WalletTransaction::getUserId, userId)
                .in(!keywordUserIds.isEmpty(), WalletTransaction::getUserId, keywordUserIds)
                .eq(walletId != null, WalletTransaction::getWalletId, walletId)
                .eq(hasText(txType), WalletTransaction::getTxType, txType)
                .eq(hasText(bizType), WalletTransaction::getBizType, bizType)
                .ge(startTime != null, WalletTransaction::getCreatedAt, startTime)
                .le(endTime != null, WalletTransaction::getCreatedAt, endTime)
                .orderByDesc(WalletTransaction::getId);
        var result = walletTransactionMapper.selectPage(page, wrapper);
        var userNames = userNameMap(result.getRecords().stream().map(WalletTransaction::getUserId).toList());
        return new PageResult<>(result.getRecords().stream()
                .map(transaction -> walletTransactionResponse(transaction, userNames.get(transaction.getUserId())))
                .toList(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public PageResult<AdminRentalOrderResponse> pageRentalOrders(
            long pageNo,
            long pageSize,
            Long userId,
            String orderNo,
            String orderStatus,
            String profitStatus,
            String settlementStatus,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        var page = new Page<RentalOrder>(pageNo, pageSize);
        var wrapper = new LambdaQueryWrapper<RentalOrder>()
                .eq(userId != null, RentalOrder::getUserId, userId)
                .eq(hasText(orderNo), RentalOrder::getOrderNo, orderNo)
                .eq(hasText(orderStatus), RentalOrder::getOrderStatus, orderStatus)
                .eq(hasText(profitStatus), RentalOrder::getProfitStatus, profitStatus)
                .eq(hasText(settlementStatus), RentalOrder::getSettlementStatus, settlementStatus)
                .ge(startTime != null, RentalOrder::getCreatedAt, startTime)
                .le(endTime != null, RentalOrder::getCreatedAt, endTime)
                .orderByDesc(RentalOrder::getId);
        var result = rentalOrderMapper.selectPage(page, wrapper);
        var userNames = userNameMap(result.getRecords().stream().map(RentalOrder::getUserId).toList());
        return new PageResult<>(result.getRecords().stream()
                .map(order -> rentalOrderResponse(order, userNames.get(order.getUserId())))
                .toList(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public AdminRentalOrderDetailResponse getRentalOrder(String orderNo) {
        var order = rentalOrderMapper.selectOne(new LambdaQueryWrapper<RentalOrder>()
                .eq(RentalOrder::getOrderNo, orderNo)
                .last("LIMIT 1"));
        if (order == null) {
            throw new BusinessException(ErrorCode.RENTAL_ORDER_NOT_FOUND);
        }
        var credential = apiCredentialMapper.selectOne(new LambdaQueryWrapper<ApiCredential>()
                .eq(ApiCredential::getRentalOrderId, order.getId())
                .last("LIMIT 1"));
        var user = appUserMapper.selectById(order.getUserId());
        var deployOrder = apiDeployOrderMapper.selectOne(new LambdaQueryWrapper<ApiDeployOrder>()
                .eq(ApiDeployOrder::getRentalOrderId, order.getId())
                .last("LIMIT 1"));
        return rentalOrderResponse(order, user == null ? null : user.getUserName(), credential,
                deployOrder == null ? null : deployOrder.getStatus());
    }

    public PageResult<AdminApiCredentialResponse> pageApiCredentials(
            long pageNo,
            long pageSize,
            Long userId,
            String credentialNo,
            String tokenStatus,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        var page = new Page<ApiCredential>(pageNo, pageSize);
        var wrapper = new LambdaQueryWrapper<ApiCredential>()
                .eq(userId != null, ApiCredential::getUserId, userId)
                .eq(hasText(credentialNo), ApiCredential::getCredentialNo, credentialNo)
                .eq(hasText(tokenStatus), ApiCredential::getTokenStatus, tokenStatus)
                .ge(startTime != null, ApiCredential::getCreatedAt, startTime)
                .le(endTime != null, ApiCredential::getCreatedAt, endTime)
                .orderByDesc(ApiCredential::getId);
        var result = apiCredentialMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getRecords().stream().map(this::credentialResponse).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    public AdminApiCredentialResponse getApiCredential(String credentialNo) {
        var credential = apiCredentialMapper.selectOne(new LambdaQueryWrapper<ApiCredential>()
                .eq(ApiCredential::getCredentialNo, credentialNo)
                .last("LIMIT 1"));
        if (credential == null) {
            throw new BusinessException(ErrorCode.API_CREDENTIAL_NOT_FOUND);
        }
        return credentialResponse(credential);
    }

    public PageResult<AdminApiDeployOrderResponse> pageApiDeployOrders(
            long pageNo,
            long pageSize,
            Long userId,
            String deployNo,
            String status,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        var page = new Page<ApiDeployOrder>(pageNo, pageSize);
        var wrapper = new LambdaQueryWrapper<ApiDeployOrder>()
                .eq(userId != null, ApiDeployOrder::getUserId, userId)
                .eq(hasText(deployNo), ApiDeployOrder::getDeployNo, deployNo)
                .eq(hasText(status), ApiDeployOrder::getStatus, status)
                .ge(startTime != null, ApiDeployOrder::getCreatedAt, startTime)
                .le(endTime != null, ApiDeployOrder::getCreatedAt, endTime)
                .orderByDesc(ApiDeployOrder::getId);
        var result = apiDeployOrderMapper.selectPage(page, wrapper);
        var userNames = userNameMap(result.getRecords().stream().map(ApiDeployOrder::getUserId).toList());
        return new PageResult<>(result.getRecords().stream()
                .map(order -> apiDeployOrderResponse(order, userNames.get(order.getUserId())))
                .toList(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public AdminApiDeployOrderResponse getApiDeployOrder(String deployNo) {
        var order = apiDeployOrderMapper.selectOne(new LambdaQueryWrapper<ApiDeployOrder>()
                .eq(ApiDeployOrder::getDeployNo, deployNo)
                .last("LIMIT 1"));
        if (order == null) {
            throw new BusinessException(ErrorCode.API_DEPLOY_ORDER_NOT_FOUND);
        }
        return apiDeployOrderResponse(order, userName(order.getUserId()));
    }

    public PageResult<AdminProfitRecordResponse> pageProfitRecords(
            long pageNo,
            long pageSize,
            String keyword,
            Long userId,
            String orderNo,
            String status,
            LocalDate profitDate,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        var orderId = resolveOrderId(orderNo);
        if (hasText(orderNo) && orderId == null) {
            return new PageResult<>(Collections.emptyList(), 0, pageNo, pageSize);
        }
        var keywordUserIds = userIdsByKeyword(keyword);
        if (hasText(keyword) && keywordUserIds.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0, pageNo, pageSize);
        }
        var page = new Page<RentalProfitRecord>(pageNo, pageSize);
        var wrapper = new LambdaQueryWrapper<RentalProfitRecord>()
                .eq(userId != null, RentalProfitRecord::getUserId, userId)
                .in(!keywordUserIds.isEmpty(), RentalProfitRecord::getUserId, keywordUserIds)
                .eq(orderId != null, RentalProfitRecord::getRentalOrderId, orderId)
                .eq(hasText(status), RentalProfitRecord::getStatus, status)
                .eq(profitDate != null, RentalProfitRecord::getProfitDate, profitDate)
                .ge(startTime != null, RentalProfitRecord::getCreatedAt, startTime)
                .le(endTime != null, RentalProfitRecord::getCreatedAt, endTime)
                .orderByDesc(RentalProfitRecord::getId);
        var result = profitRecordMapper.selectPage(page, wrapper);
        var userNames = userNameMap(result.getRecords().stream().map(RentalProfitRecord::getUserId).toList());
        return new PageResult<>(result.getRecords().stream()
                .map(record -> profitRecordResponse(record, userNames.get(record.getUserId())))
                .toList(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public AdminProfitRecordResponse getProfitRecord(String profitNo) {
        var record = profitRecordMapper.selectOne(new LambdaQueryWrapper<RentalProfitRecord>()
                .eq(RentalProfitRecord::getProfitNo, profitNo)
                .last("LIMIT 1"));
        if (record == null) {
            throw new BusinessException(ErrorCode.PROFIT_RECORD_NOT_FOUND);
        }
        return profitRecordResponse(record, userName(record.getUserId()));
    }

    public PageResult<AdminSettlementOrderResponse> pageSettlementOrders(
            long pageNo,
            long pageSize,
            Long userId,
            String orderNo,
            String settlementType,
            String status,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        var orderId = resolveOrderId(orderNo);
        if (hasText(orderNo) && orderId == null) {
            return new PageResult<>(Collections.emptyList(), 0, pageNo, pageSize);
        }
        var page = new Page<RentalSettlementOrder>(pageNo, pageSize);
        var wrapper = new LambdaQueryWrapper<RentalSettlementOrder>()
                .eq(userId != null, RentalSettlementOrder::getUserId, userId)
                .eq(orderId != null, RentalSettlementOrder::getRentalOrderId, orderId)
                .eq(hasText(settlementType), RentalSettlementOrder::getSettlementType, settlementType)
                .eq(hasText(status), RentalSettlementOrder::getStatus, status)
                .ge(startTime != null, RentalSettlementOrder::getCreatedAt, startTime)
                .le(endTime != null, RentalSettlementOrder::getCreatedAt, endTime)
                .orderByDesc(RentalSettlementOrder::getId);
        var result = settlementOrderMapper.selectPage(page, wrapper);
        var userNames = userNameMap(result.getRecords().stream().map(RentalSettlementOrder::getUserId).toList());
        return new PageResult<>(result.getRecords().stream()
                .map(order -> settlementOrderResponse(order, userNames.get(order.getUserId())))
                .toList(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public AdminSettlementOrderResponse getSettlementOrder(String settlementNo) {
        var order = settlementOrderMapper.selectOne(new LambdaQueryWrapper<RentalSettlementOrder>()
                .eq(RentalSettlementOrder::getSettlementNo, settlementNo)
                .last("LIMIT 1"));
        if (order == null) {
            throw new BusinessException(ErrorCode.SETTLEMENT_ORDER_NOT_FOUND);
        }
        return settlementOrderResponse(order, userName(order.getUserId()));
    }

    public PageResult<AdminCommissionRecordResponse> pageCommissionRecords(
            long pageNo,
            long pageSize,
            Long userId,
            String orderNo,
            Integer levelNo,
            String status,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        if (unsupportedLevel(levelNo)) {
            return new PageResult<>(Collections.emptyList(), 0, pageNo, pageSize);
        }
        var orderId = resolveOrderId(orderNo);
        if (hasText(orderNo) && orderId == null) {
            return new PageResult<>(Collections.emptyList(), 0, pageNo, pageSize);
        }
        var page = new Page<CommissionRecord>(pageNo, pageSize);
        var wrapper = new LambdaQueryWrapper<CommissionRecord>()
                .eq(userId != null, CommissionRecord::getBenefitUserId, userId)
                .eq(orderId != null, CommissionRecord::getSourceOrderId, orderId)
                .le(CommissionRecord::getLevelNo, MAX_HIERARCHY_LEVEL)
                .eq(levelNo != null, CommissionRecord::getLevelNo, levelNo)
                .eq(hasText(status), CommissionRecord::getStatus, status)
                .ge(startTime != null, CommissionRecord::getCreatedAt, startTime)
                .le(endTime != null, CommissionRecord::getCreatedAt, endTime)
                .orderByDesc(CommissionRecord::getId);
        var result = commissionRecordMapper.selectPage(page, wrapper);
        var userNames = userNameMap(result.getRecords().stream().map(CommissionRecord::getSourceUserId).toList());
        return new PageResult<>(result.getRecords().stream()
                .map(record -> commissionRecordResponse(record, userNames.get(record.getSourceUserId())))
                .toList(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public AdminCommissionRecordResponse getCommissionRecord(String commissionNo) {
        var record = commissionRecordMapper.selectOne(new LambdaQueryWrapper<CommissionRecord>()
                .eq(CommissionRecord::getCommissionNo, commissionNo)
                .le(CommissionRecord::getLevelNo, MAX_HIERARCHY_LEVEL)
                .last("LIMIT 1"));
        if (record == null) {
            throw new BusinessException(ErrorCode.COMMISSION_RECORD_NOT_FOUND);
        }
        return commissionRecordResponse(record, userName(record.getSourceUserId()));
    }

    public AdminTeamOverviewResponse adminTeamOverview(LocalDateTime startTime, LocalDateTime endTime) {
        var today = DateTimeUtils.today();
        var startAt = startTime == null ? today.atStartOfDay() : startTime;
        var endAt = endTime == null ? today.plusDays(1).atStartOfDay() : endTime;
        return new AdminTeamOverviewResponse(
                activeTeamCount(),
                estimatedCommissionAmount(startAt, endAt),
                CURRENCY_USDT,
                null,
                null);
    }

    public PageResult<AdminTeamListRow> pageAdminTeamList(
            long pageNo,
            long pageSize,
            String keyword,
            Integer status,
            String sortBy
    ) {
        var current = normalizePageNo(pageNo);
        var size = normalizePageSize(pageSize, 100);
        var aggregates = adminTeamAggregates(keyword, status);
        aggregates.sort(teamListComparator(sortBy));
        var pageRecords = slice(aggregates, current, size).stream()
                .map(this::adminTeamListRow)
                .toList();
        return new PageResult<>(pageRecords, aggregates.size(), current, size);
    }

    public PageResult<AdminTeamLeaderboardRow> pageAdminTeamLeaderboard(
            long pageNo,
            long pageSize,
            String sortBy
    ) {
        var current = normalizePageNo(pageNo);
        var size = normalizePageSize(pageSize, 100);
        var aggregates = adminTeamAggregates(null, null);
        aggregates.sort(teamLeaderboardComparator(sortBy));
        var fromIndex = pageStartIndex(aggregates.size(), current, size);
        var toIndex = pageEndIndex(aggregates.size(), fromIndex, size);
        var records = new ArrayList<AdminTeamLeaderboardRow>();
        for (var index = fromIndex; index < toIndex; index++) {
            records.add(adminTeamLeaderboardRow(aggregates.get(index), index + 1L));
        }
        return new PageResult<>(records, aggregates.size(), current, size);
    }

    public AdminTeamUserSummaryResponse adminTeamUserSummary(Long userId) {
        var user = requireUser(userId);
        var aggregate = adminTeamAggregateMap(List.of(userId))
                .getOrDefault(userId, emptyAdminTeamAggregate(user));
        return adminTeamUserSummaryResponse(aggregate);
    }

    public PageResult<AdminTeamTreeNode> pageAdminTeamChildren(
            Long rootUserId,
            Long parentUserId,
            long pageNo,
            long pageSize
    ) {
        requireUser(rootUserId);
        requireUser(parentUserId);
        var current = normalizePageNo(pageNo);
        var size = normalizePageSize(pageSize, 100);
        Integer parentDepth = rootUserId.equals(parentUserId) ? Integer.valueOf(0)
                : relationDepth(rootUserId, parentUserId);
        if (parentDepth == null || parentDepth >= MAX_HIERARCHY_LEVEL) {
            return new PageResult<>(Collections.emptyList(), 0, current, size);
        }

        var page = new Page<UserTeamRelation>(current, size);
        var result = teamRelationMapper.selectPage(page, new LambdaQueryWrapper<UserTeamRelation>()
                .eq(UserTeamRelation::getAncestorUserId, parentUserId)
                .eq(UserTeamRelation::getLevelDepth, 1)
                .orderByDesc(UserTeamRelation::getId));
        var relations = result.getRecords();
        var childIds = relations.stream().map(UserTeamRelation::getDescendantUserId).toList();
        var users = appUserMap(childIds);
        var counts = teamCountsByAncestor(selectRelationsForAncestors(childIds));
        var totalContribution = settledContributionBySource(rootUserId, childIds, null, null);
        var yesterdayContribution = settledContributionBySource(rootUserId, childIds, yesterdayStart(),
                todayStart());
        var childDepth = parentDepth + 1;
        var nodes = relations.stream()
                .map(relation -> adminTeamTreeNode(
                        relation.getDescendantUserId(),
                        users.get(relation.getDescendantUserId()),
                        parentUserId,
                        childDepth,
                        counts.getOrDefault(relation.getDescendantUserId(), TeamCounts.ZERO),
                        totalContribution.getOrDefault(relation.getDescendantUserId(), BigDecimal.ZERO),
                        yesterdayContribution.getOrDefault(relation.getDescendantUserId(), BigDecimal.ZERO)))
                .toList();
        return new PageResult<>(nodes, result.getTotal(), result.getCurrent(), result.getSize());
    }

    public PageResult<AdminTeamMemberRow> pageAdminTeamMembers(
            Long ancestorUserId,
            long pageNo,
            long pageSize,
            Integer levelDepth,
            String keyword
    ) {
        requireUser(ancestorUserId);
        var current = normalizePageNo(pageNo);
        var size = normalizePageSize(pageSize, 100);
        if (unsupportedLevel(levelDepth)) {
            return new PageResult<>(Collections.emptyList(), 0, current, size);
        }
        var matchedUserIds = userIdsByAdminKeyword(keyword);
        if (hasText(keyword) && matchedUserIds.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0, current, size);
        }

        var page = new Page<UserTeamRelation>(current, size);
        var wrapper = new LambdaQueryWrapper<UserTeamRelation>()
                .eq(UserTeamRelation::getAncestorUserId, ancestorUserId)
                .le(UserTeamRelation::getLevelDepth, MAX_HIERARCHY_LEVEL)
                .eq(levelDepth != null, UserTeamRelation::getLevelDepth, levelDepth)
                .in(!matchedUserIds.isEmpty(), UserTeamRelation::getDescendantUserId, matchedUserIds)
                .orderByAsc(UserTeamRelation::getLevelDepth)
                .orderByDesc(UserTeamRelation::getId);
        var result = teamRelationMapper.selectPage(page, wrapper);
        var relations = result.getRecords();
        var descendantIds = relations.stream().map(UserTeamRelation::getDescendantUserId).toList();
        var users = appUserMap(descendantIds);
        var parentIds = directParentIdsByDescendant(descendantIds);
        var orderBriefs = activeOrderBriefByUser(descendantIds);
        var totalContribution = settledContributionBySource(ancestorUserId, descendantIds, null, null);
        var yesterdayContribution = settledContributionBySource(ancestorUserId, descendantIds, yesterdayStart(),
                todayStart());
        var records = relations.stream()
                .map(relation -> adminTeamMemberRow(
                        relation,
                        users.get(relation.getDescendantUserId()),
                        parentIds.getOrDefault(relation.getDescendantUserId(), ancestorUserId),
                        orderBriefs.getOrDefault(relation.getDescendantUserId(), TeamOrderBrief.none()),
                        totalContribution.getOrDefault(relation.getDescendantUserId(), BigDecimal.ZERO),
                        yesterdayContribution.getOrDefault(relation.getDescendantUserId(), BigDecimal.ZERO)))
                .toList();
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    public PageResult<AdminTeamRelationResponse> pageTeamRelations(
            long pageNo,
            long pageSize,
            Long ancestorUserId,
            Long descendantUserId,
            Integer levelDepth
    ) {
        if (unsupportedLevel(levelDepth)) {
            return new PageResult<>(Collections.emptyList(), 0, pageNo, pageSize);
        }
        var page = new Page<UserTeamRelation>(pageNo, pageSize);
        var wrapper = new LambdaQueryWrapper<UserTeamRelation>()
                .eq(ancestorUserId != null, UserTeamRelation::getAncestorUserId, ancestorUserId)
                .eq(descendantUserId != null, UserTeamRelation::getDescendantUserId, descendantUserId)
                .le(UserTeamRelation::getLevelDepth, MAX_HIERARCHY_LEVEL)
                .eq(levelDepth != null, UserTeamRelation::getLevelDepth, levelDepth)
                .orderByAsc(UserTeamRelation::getLevelDepth)
                .orderByDesc(UserTeamRelation::getId);
        var result = teamRelationMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getRecords().stream().map(this::teamRelationResponse).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    public AdminUserTeamResponse userTeam(Long userId) {
        requireUser(userId);
        var relations = teamRelationMapper.selectList(new LambdaQueryWrapper<UserTeamRelation>()
                .eq(UserTeamRelation::getAncestorUserId, userId)
                .le(UserTeamRelation::getLevelDepth, MAX_HIERARCHY_LEVEL)
                .orderByAsc(UserTeamRelation::getLevelDepth));
        return new AdminUserTeamResponse(
                userId,
                relations.size(),
                countDepth(relations, 1),
                countDepth(relations, 2),
                relations.stream().map(this::teamRelationResponse).toList());
    }

    public PageResult<AdminLogResponse> pageLogs(
            long pageNo,
            long pageSize,
            Long adminId,
            String action,
            String bizType,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        var page = new Page<SysAdminLog>(pageNo, pageSize);
        var wrapper = new LambdaQueryWrapper<SysAdminLog>()
                .eq(adminId != null, SysAdminLog::getAdminId, adminId)
                .eq(hasText(action), SysAdminLog::getAction, action)
                .eq(hasText(bizType), SysAdminLog::getTargetTable, bizType)
                .ge(startTime != null, SysAdminLog::getCreatedAt, startTime)
                .le(endTime != null, SysAdminLog::getCreatedAt, endTime)
                .orderByDesc(SysAdminLog::getId);
        var result = adminLogMapper.selectPage(page, wrapper);
        result.getRecords().forEach(this::fillAdminLogDisplayFields);
        return new PageResult<>(result.getRecords().stream().map(this::adminLogResponse).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    public AdminLogResponse getLog(Long id) {
        var log = adminLogMapper.selectById(id);
        if (log == null) {
            throw new BusinessException(ErrorCode.ADMIN_LOG_NOT_FOUND);
        }
        fillAdminLogDisplayFields(log);
        return adminLogResponse(log);
    }

    private void fillAdminLogDisplayFields(SysAdminLog log) {
        if (log != null) {
            log.setActionName(AdminLogService.actionName(log.getAction()));
        }
    }

    private AppUser requireUser(Long id) {
        var user = appUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private long activeTeamCount() {
        var relations = selectTwoLevelRelations();
        if (relations.isEmpty()) {
            return 0;
        }
        var descendantIds = relations.stream().map(UserTeamRelation::getDescendantUserId).toList();
        var enabledUserIds = new HashSet<Long>();
        for (var user : appUserMap(descendantIds).values()) {
            if (Integer.valueOf(CommonStatus.ENABLED.value()).equals(user.getStatus())) {
                enabledUserIds.add(user.getId());
            }
        }
        var activeAncestorIds = new HashSet<Long>();
        for (var relation : relations) {
            if (enabledUserIds.contains(relation.getDescendantUserId())) {
                activeAncestorIds.add(relation.getAncestorUserId());
            }
        }
        return activeAncestorIds.size();
    }

    private BigDecimal estimatedCommissionAmount(LocalDateTime startAt, LocalDateTime endAt) {
        var records = commissionRecordMapper.selectList(new LambdaQueryWrapper<CommissionRecord>()
                .le(CommissionRecord::getLevelNo, MAX_HIERARCHY_LEVEL)
                .ne(CommissionRecord::getStatus, RecordSettleStatus.CANCELED.name())
                .ge(startAt != null, CommissionRecord::getCreatedAt, startAt)
                .lt(endAt != null, CommissionRecord::getCreatedAt, endAt));
        return sumCommissionAmount(records);
    }

    private List<AdminTeamAggregate> adminTeamAggregates(String keyword, Integer status) {
        var candidateIds = teamAncestorIds();
        if (candidateIds.isEmpty()) {
            return new ArrayList<>();
        }
        if (hasText(keyword)) {
            candidateIds.retainAll(userIdsByAdminKeyword(keyword));
            if (candidateIds.isEmpty()) {
                return new ArrayList<>();
            }
        }

        var aggregates = new ArrayList<>(adminTeamAggregateMap(candidateIds).values());
        if (status != null) {
            aggregates.removeIf(aggregate -> !status.equals(aggregate.user().getStatus()));
        }
        return aggregates;
    }

    private Map<Long, AdminTeamAggregate> adminTeamAggregateMap(Collection<Long> userIds) {
        var ids = distinctIds(userIds);
        if (ids.isEmpty()) {
            return Map.of();
        }
        var users = appUserMap(ids);
        var relations = selectRelationsForAncestors(ids);
        var counts = teamCountsByAncestor(relations);
        var orderStats = teamOrderStatsByAncestor(relations);
        var totalCommission = settledCommissionByBenefit(ids, null, null);
        var yesterdayCommission = settledCommissionByBenefit(ids, yesterdayStart(), todayStart());
        var lastCommissionAt = lastCommissionAtByBenefit(ids);
        var result = new HashMap<Long, AdminTeamAggregate>();
        for (var id : ids) {
            var user = users.get(id);
            if (user == null) {
                continue;
            }
            result.put(id, new AdminTeamAggregate(
                    user,
                    counts.getOrDefault(id, TeamCounts.ZERO),
                    yesterdayCommission.getOrDefault(id, BigDecimal.ZERO),
                    totalCommission.getOrDefault(id, BigDecimal.ZERO),
                    orderStats.getOrDefault(id, TeamOrderStats.ZERO),
                    lastCommissionAt.get(id)));
        }
        return result;
    }

    private AdminTeamAggregate emptyAdminTeamAggregate(AppUser user) {
        return new AdminTeamAggregate(
                user,
                TeamCounts.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                TeamOrderStats.ZERO,
                null);
    }

    private Set<Long> teamAncestorIds() {
        var ancestorIds = new HashSet<Long>();
        for (var relation : selectTwoLevelRelations()) {
            ancestorIds.add(relation.getAncestorUserId());
        }
        return ancestorIds;
    }

    private List<UserTeamRelation> selectTwoLevelRelations() {
        return teamRelationMapper.selectList(new LambdaQueryWrapper<UserTeamRelation>()
                .le(UserTeamRelation::getLevelDepth, MAX_HIERARCHY_LEVEL));
    }

    private List<UserTeamRelation> selectRelationsForAncestors(Collection<Long> ancestorIds) {
        var ids = distinctIds(ancestorIds);
        if (ids.isEmpty()) {
            return List.of();
        }
        return teamRelationMapper.selectList(new LambdaQueryWrapper<UserTeamRelation>()
                .in(UserTeamRelation::getAncestorUserId, ids)
                .le(UserTeamRelation::getLevelDepth, MAX_HIERARCHY_LEVEL));
    }

    private Integer relationDepth(Long ancestorUserId, Long descendantUserId) {
        var relation = teamRelationMapper.selectOne(new LambdaQueryWrapper<UserTeamRelation>()
                .eq(UserTeamRelation::getAncestorUserId, ancestorUserId)
                .eq(UserTeamRelation::getDescendantUserId, descendantUserId)
                .le(UserTeamRelation::getLevelDepth, MAX_HIERARCHY_LEVEL)
                .last("LIMIT 1"));
        return relation == null ? null : relation.getLevelDepth();
    }

    private Map<Long, TeamCounts> teamCountsByAncestor(List<UserTeamRelation> relations) {
        var result = new HashMap<Long, TeamCounts>();
        for (var relation : relations) {
            var current = result.getOrDefault(relation.getAncestorUserId(), TeamCounts.ZERO);
            if (Integer.valueOf(1).equals(relation.getLevelDepth())) {
                result.put(relation.getAncestorUserId(),
                        new TeamCounts(current.directCount() + 1, current.indirectCount()));
            } else if (Integer.valueOf(2).equals(relation.getLevelDepth())) {
                result.put(relation.getAncestorUserId(),
                        new TeamCounts(current.directCount(), current.indirectCount() + 1));
            }
        }
        return result;
    }

    private Map<Long, TeamOrderStats> teamOrderStatsByAncestor(List<UserTeamRelation> relations) {
        if (relations.isEmpty()) {
            return Map.of();
        }
        var ancestorsByDescendant = new HashMap<Long, Set<Long>>();
        for (var relation : relations) {
            ancestorsByDescendant.computeIfAbsent(relation.getDescendantUserId(), ignored -> new HashSet<>())
                    .add(relation.getAncestorUserId());
        }
        var orders = selectActiveOrders(ancestorsByDescendant.keySet());
        var result = new HashMap<Long, TeamOrderStats>();
        for (var order : orders) {
            var ancestorIds = ancestorsByDescendant.get(order.getUserId());
            if (ancestorIds == null) {
                continue;
            }
            var running = RentalOrderStatus.RUNNING.name().equals(order.getOrderStatus());
            for (var ancestorId : ancestorIds) {
                var current = result.getOrDefault(ancestorId, TeamOrderStats.ZERO);
                result.put(ancestorId, new TeamOrderStats(
                        current.activeOrderCount() + 1,
                        current.runningOrderCount() + (running ? 1 : 0)));
            }
        }
        return result;
    }

    private List<RentalOrder> selectActiveOrders(Collection<Long> userIds) {
        var ids = distinctIds(userIds);
        if (ids.isEmpty()) {
            return List.of();
        }
        return rentalOrderMapper.selectList(new LambdaQueryWrapper<RentalOrder>()
                .in(RentalOrder::getUserId, ids)
                .in(RentalOrder::getOrderStatus, ACTIVE_ORDER_STATUSES)
                .orderByDesc(RentalOrder::getId));
    }

    private Map<Long, TeamOrderBrief> activeOrderBriefByUser(Collection<Long> userIds) {
        var result = new HashMap<Long, TeamOrderBrief>();
        for (var order : selectActiveOrders(userIds)) {
            var current = result.get(order.getUserId());
            if (RentalOrderStatus.RUNNING.name().equals(order.getOrderStatus())) {
                if (current == null || !RentalOrderStatus.RUNNING.name().equals(current.orderStatus())) {
                    result.put(order.getUserId(), new TeamOrderBrief(RentalOrderStatus.RUNNING.name(),
                            order.getOrderNo()));
                }
            } else if (current == null) {
                result.put(order.getUserId(), new TeamOrderBrief(RentalOrderStatus.PAUSED.name(), order.getOrderNo()));
            }
        }
        return result;
    }

    private Map<Long, BigDecimal> settledCommissionByBenefit(
            Collection<Long> benefitUserIds,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        var ids = distinctIds(benefitUserIds);
        if (ids.isEmpty()) {
            return Map.of();
        }
        var records = commissionRecordMapper.selectList(new LambdaQueryWrapper<CommissionRecord>()
                .in(CommissionRecord::getBenefitUserId, ids)
                .eq(CommissionRecord::getStatus, RecordSettleStatus.SETTLED.name())
                .le(CommissionRecord::getLevelNo, MAX_HIERARCHY_LEVEL)
                .ge(startAt != null, CommissionRecord::getSettledAt, startAt)
                .lt(endAt != null, CommissionRecord::getSettledAt, endAt));
        var result = new HashMap<Long, BigDecimal>();
        for (var record : records) {
            result.merge(record.getBenefitUserId(), nullToZero(record.getCommissionAmount()), BigDecimal::add);
        }
        return result;
    }

    private Map<Long, BigDecimal> settledContributionBySource(
            Long benefitUserId,
            Collection<Long> sourceUserIds,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        var ids = distinctIds(sourceUserIds);
        if (ids.isEmpty()) {
            return Map.of();
        }
        var records = commissionRecordMapper.selectList(new LambdaQueryWrapper<CommissionRecord>()
                .eq(CommissionRecord::getBenefitUserId, benefitUserId)
                .in(CommissionRecord::getSourceUserId, ids)
                .eq(CommissionRecord::getStatus, RecordSettleStatus.SETTLED.name())
                .le(CommissionRecord::getLevelNo, MAX_HIERARCHY_LEVEL)
                .ge(startAt != null, CommissionRecord::getSettledAt, startAt)
                .lt(endAt != null, CommissionRecord::getSettledAt, endAt));
        var result = new HashMap<Long, BigDecimal>();
        for (var record : records) {
            result.merge(record.getSourceUserId(), nullToZero(record.getCommissionAmount()), BigDecimal::add);
        }
        return result;
    }

    private Map<Long, LocalDateTime> lastCommissionAtByBenefit(Collection<Long> benefitUserIds) {
        var ids = distinctIds(benefitUserIds);
        if (ids.isEmpty()) {
            return Map.of();
        }
        var records = commissionRecordMapper.selectList(new LambdaQueryWrapper<CommissionRecord>()
                .in(CommissionRecord::getBenefitUserId, ids)
                .eq(CommissionRecord::getStatus, RecordSettleStatus.SETTLED.name())
                .le(CommissionRecord::getLevelNo, MAX_HIERARCHY_LEVEL)
                .isNotNull(CommissionRecord::getSettledAt));
        var result = new HashMap<Long, LocalDateTime>();
        for (var record : records) {
            result.merge(record.getBenefitUserId(), record.getSettledAt(),
                    (first, second) -> first.isAfter(second) ? first : second);
        }
        return result;
    }

    private Map<Long, Long> directParentIdsByDescendant(Collection<Long> descendantUserIds) {
        var ids = distinctIds(descendantUserIds);
        if (ids.isEmpty()) {
            return Map.of();
        }
        var relations = teamRelationMapper.selectList(new LambdaQueryWrapper<UserTeamRelation>()
                .in(UserTeamRelation::getDescendantUserId, ids)
                .eq(UserTeamRelation::getLevelDepth, 1));
        var result = new HashMap<Long, Long>();
        for (var relation : relations) {
            result.putIfAbsent(relation.getDescendantUserId(), relation.getAncestorUserId());
        }
        return result;
    }

    private Map<Long, AppUser> appUserMap(Collection<Long> userIds) {
        var ids = distinctIds(userIds);
        if (ids.isEmpty()) {
            return Map.of();
        }
        var users = new HashMap<Long, AppUser>();
        for (var user : appUserMapper.selectBatchIds(ids)) {
            users.put(user.getId(), user);
        }
        return users;
    }

    private List<Long> userIdsByAdminKeyword(String keyword) {
        var normalized = AppUserSearchSupport.normalize(keyword);
        if (!AppUserSearchSupport.hasText(normalized)) {
            return Collections.emptyList();
        }
        var internalId = parseLong(normalized);
        var wrapper = new LambdaQueryWrapper<AppUser>()
                .select(AppUser::getId)
                .and(userWrapper -> {
                    userWrapper.eq(AppUser::getUserId, normalized)
                            .or()
                            .eq(AppUser::getEmail, normalized)
                            .or()
                            .likeRight(AppUser::getUserName, normalized)
                            .or()
                            .likeRight(AppUser::getEmail, normalized);
                    if (internalId != null) {
                        userWrapper.or().eq(AppUser::getId, internalId);
                    }
                });
        return appUserMapper.selectList(wrapper).stream().map(AppUser::getId).toList();
    }

    private Long parseLong(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Comparator<AdminTeamAggregate> teamListComparator(String sortBy) {
        return switch (sortBy == null ? "" : sortBy) {
            case "totalCommission" -> teamCommissionComparator(AdminTeamAggregate::totalCommission);
            case "yesterdayCommission" -> teamCommissionComparator(AdminTeamAggregate::yesterdayCommission);
            case "directCount" -> Comparator.comparingLong(
                    (AdminTeamAggregate aggregate) -> aggregate.counts().directCount()).reversed()
                    .thenComparing(teamUserIdDescComparator());
            default -> Comparator.comparing(
                            AdminTeamAggregate::lastCommissionAt,
                            Comparator.nullsFirst(Comparator.naturalOrder()))
                    .reversed()
                    .thenComparing(teamUserIdDescComparator());
        };
    }

    private Comparator<AdminTeamAggregate> teamLeaderboardComparator(String sortBy) {
        return switch (sortBy == null ? "" : sortBy) {
            case "yesterdayCommission" -> teamCommissionComparator(AdminTeamAggregate::yesterdayCommission);
            case "directCount" -> Comparator.comparingLong(
                    (AdminTeamAggregate aggregate) -> aggregate.counts().directCount()).reversed()
                    .thenComparing(teamUserIdDescComparator());
            default -> teamCommissionComparator(AdminTeamAggregate::totalCommission);
        };
    }

    private Comparator<AdminTeamAggregate> teamCommissionComparator(
            java.util.function.Function<AdminTeamAggregate, BigDecimal> amountExtractor
    ) {
        return Comparator.comparing(amountExtractor).reversed().thenComparing(teamUserIdDescComparator());
    }

    private Comparator<AdminTeamAggregate> teamUserIdDescComparator() {
        return Comparator.comparing(AdminTeamAggregate::userId).reversed();
    }

    private LocalDateTime yesterdayStart() {
        return DateTimeUtils.today().minusDays(1).atStartOfDay();
    }

    private LocalDateTime todayStart() {
        return DateTimeUtils.today().atStartOfDay();
    }

    private long normalizePageNo(long pageNo) {
        return Math.max(1, pageNo);
    }

    private long normalizePageSize(long pageSize, long maxPageSize) {
        return Math.min(Math.max(1, pageSize), maxPageSize);
    }

    private int pageStartIndex(int total, long pageNo, long pageSize) {
        var start = (pageNo - 1) * pageSize;
        return (int) Math.min(start, total);
    }

    private int pageEndIndex(int total, int fromIndex, long pageSize) {
        return (int) Math.min(fromIndex + pageSize, total);
    }

    private <T> List<T> slice(List<T> records, long pageNo, long pageSize) {
        var fromIndex = pageStartIndex(records.size(), pageNo, pageSize);
        var toIndex = pageEndIndex(records.size(), fromIndex, pageSize);
        return records.subList(fromIndex, toIndex);
    }

    private Set<Long> distinctIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashSet<>();
        }
        var result = new HashSet<Long>();
        for (var id : ids) {
            if (id != null) {
                result.add(id);
            }
        }
        return result;
    }

    private BigDecimal sumCommissionAmount(List<CommissionRecord> records) {
        var result = BigDecimal.ZERO;
        for (var record : records) {
            result = result.add(nullToZero(record.getCommissionAmount()));
        }
        return result;
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Long resolveOrderId(String orderNo) {
        if (!hasText(orderNo)) {
            return null;
        }
        var order = rentalOrderMapper.selectOne(new LambdaQueryWrapper<RentalOrder>()
                .eq(RentalOrder::getOrderNo, orderNo)
                .last("LIMIT 1"));
        return order == null ? null : order.getId();
    }

    private AdminTeamListRow adminTeamListRow(AdminTeamAggregate aggregate) {
        var user = aggregate.user();
        var counts = aggregate.counts();
        var orderStats = aggregate.orderStats();
        return new AdminTeamListRow(
                user.getId(),
                user.getUserName(),
                user.getEmail(),
                user.getAvatarKey(),
                user.getStatus(),
                counts.directCount(),
                counts.indirectCount(),
                counts.totalCount(),
                aggregate.yesterdayCommission(),
                aggregate.totalCommission(),
                orderStats.activeOrderCount(),
                orderStats.runningOrderCount(),
                aggregate.lastCommissionAt(),
                CURRENCY_USDT);
    }

    private AdminTeamLeaderboardRow adminTeamLeaderboardRow(AdminTeamAggregate aggregate, long rankNo) {
        var user = aggregate.user();
        var counts = aggregate.counts();
        return new AdminTeamLeaderboardRow(
                rankNo,
                user.getId(),
                user.getUserName(),
                user.getAvatarKey(),
                user.getStatus(),
                counts.directCount(),
                counts.indirectCount(),
                aggregate.yesterdayCommission(),
                aggregate.totalCommission(),
                aggregate.orderStats().activeOrderCount(),
                CURRENCY_USDT);
    }

    private AdminTeamUserSummaryResponse adminTeamUserSummaryResponse(AdminTeamAggregate aggregate) {
        var user = aggregate.user();
        var counts = aggregate.counts();
        var orderStats = aggregate.orderStats();
        return new AdminTeamUserSummaryResponse(
                user.getId(),
                user.getUserName(),
                user.getEmail(),
                user.getAvatarKey(),
                user.getStatus(),
                counts.directCount(),
                counts.indirectCount(),
                counts.totalCount(),
                aggregate.yesterdayCommission(),
                aggregate.totalCommission(),
                orderStats.activeOrderCount(),
                orderStats.runningOrderCount(),
                CURRENCY_USDT);
    }

    private AdminTeamTreeNode adminTeamTreeNode(
            Long userId,
            AppUser user,
            Long parentUserId,
            int levelDepth,
            TeamCounts counts,
            BigDecimal totalContribution,
            BigDecimal yesterdayContribution
    ) {
        var childrenCount = levelDepth >= MAX_HIERARCHY_LEVEL ? 0 : counts.directCount();
        return new AdminTeamTreeNode(
                userId,
                user == null ? null : user.getUserName(),
                user == null ? null : user.getAvatarKey(),
                user == null ? null : user.getStatus(),
                levelDepth,
                parentUserId,
                counts.directCount(),
                counts.indirectCount(),
                childrenCount > 0,
                childrenCount,
                totalContribution,
                yesterdayContribution,
                CURRENCY_USDT);
    }

    private AdminTeamMemberRow adminTeamMemberRow(
            UserTeamRelation relation,
            AppUser user,
            Long parentUserId,
            TeamOrderBrief orderBrief,
            BigDecimal totalContribution,
            BigDecimal yesterdayContribution
    ) {
        return new AdminTeamMemberRow(
                relation.getId(),
                relation.getAncestorUserId(),
                parentUserId,
                relation.getDescendantUserId(),
                user == null ? null : user.getUserName(),
                user == null ? null : user.getAvatarKey(),
                user == null ? null : user.getStatus(),
                relation.getLevelDepth(),
                relation.getCreatedAt(),
                orderBrief.orderStatus(),
                orderBrief.latestOrderNo(),
                yesterdayContribution,
                totalContribution,
                CURRENCY_USDT);
    }

    private AdminUserResponse userResponse(AppUser user) {
        return new AdminUserResponse(
                user.getId(),
                user.getUserId(),
                user.getEmail(),
                user.getUserName(),
                user.getAvatarKey(),
                user.getStatus(),
                user.getEmailVerifiedAt(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    private AdminWalletResponse walletResponse(UserWallet wallet, UserBrief user) {
        return new AdminWalletResponse(
                wallet.getId(),
                wallet.getWalletNo(),
                wallet.getUserId(),
                user == null ? null : user.userName(),
                user == null ? null : user.email(),
                wallet.getCurrency(),
                wallet.getAvailableBalance(),
                wallet.getFrozenBalance(),
                wallet.getTotalRecharge(),
                wallet.getTotalWithdraw(),
                wallet.getTotalProfit(),
                wallet.getTotalCommission(),
                wallet.getStatus(),
                wallet.getVersionNo(),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt());
    }

    private AdminWalletTransactionResponse walletTransactionResponse(WalletTransaction transaction, String userName) {
        return new AdminWalletTransactionResponse(
                transaction.getId(),
                transaction.getTxNo(),
                transaction.getIdempotencyKey(),
                transaction.getUserId(),
                userName,
                transaction.getWalletId(),
                transaction.getCurrency(),
                transaction.getTxType(),
                transaction.getAmount(),
                transaction.getBeforeAvailableBalance(),
                transaction.getAfterAvailableBalance(),
                transaction.getBeforeFrozenBalance(),
                transaction.getAfterFrozenBalance(),
                transaction.getBizType(),
                transaction.getBizOrderNo(),
                WalletRemarkUtils.toChinese(transaction.getRemark()),
                transaction.getCreatedAt());
    }

    private AdminRentalOrderResponse rentalOrderResponse(RentalOrder order, String userName) {
        return new AdminRentalOrderResponse(
                order.getId(),
                order.getOrderNo(),
                order.getUserId(),
                userName,
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
                order.getUpdatedAt());
    }

    private AdminRentalOrderDetailResponse rentalOrderResponse(
            RentalOrder order,
            String userName,
            ApiCredential credential,
            String deployOrderStatus
    ) {
        return new AdminRentalOrderDetailResponse(
                order.getId(),
                order.getOrderNo(),
                order.getUserId(),
                userName,
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
                order.getUpdatedAt(),
                credential == null ? null : credential.getCredentialNo(),
                credential == null ? null : credential.getTokenStatus(),
                credential == null ? null : credential.getApiBaseUrl(),
                deployOrderStatus);
    }

    private AdminApiCredentialResponse credentialResponse(ApiCredential credential) {
        return new AdminApiCredentialResponse(
                credential.getId(),
                credential.getCredentialNo(),
                credential.getUserId(),
                credential.getRentalOrderId(),
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
                credential.getExpiredAt(),
                credential.getRevokedAt(),
                credential.getMockRequestCount(),
                credential.getMockTokenDisplay(),
                credential.getMockLastRefreshAt(),
                credential.getRemark(),
                credential.getCreatedAt(),
                credential.getUpdatedAt());
    }

    private AdminApiDeployOrderResponse apiDeployOrderResponse(ApiDeployOrder order, String userName) {
        return new AdminApiDeployOrderResponse(
                order.getId(),
                order.getDeployNo(),
                order.getUserId(),
                userName,
                order.getRentalOrderId(),
                order.getApiCredentialId(),
                order.getAiModelId(),
                order.getModelNameSnapshot(),
                order.getCurrency(),
                order.getDeployFeeAmount(),
                order.getStatus(),
                order.getWalletTxNo(),
                order.getPaidAt(),
                order.getCanceledAt(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }

    private AdminProfitRecordResponse profitRecordResponse(RentalProfitRecord record, String userName) {
        return new AdminProfitRecordResponse(
                record.getId(),
                record.getProfitNo(),
                record.getUserId(),
                userName,
                record.getRentalOrderId(),
                record.getProfitDate(),
                record.getEffectiveMinutes(),
                record.getPeriodStartAt(),
                record.getPeriodEndAt(),
                record.getGpuDailyTokenSnapshot(),
                record.getTokenPriceSnapshot(),
                record.getYieldMultiplierSnapshot(),
                record.getBaseProfitAmount(),
                record.getFinalProfitAmount(),
                record.getStatus(),
                record.getWalletTxNo(),
                record.getCommissionGenerated(),
                record.getSettledAt(),
                record.getRemark(),
                record.getCreatedAt(),
                record.getUpdatedAt());
    }

    private AdminSettlementOrderResponse settlementOrderResponse(RentalSettlementOrder order, String userName) {
        return new AdminSettlementOrderResponse(
                order.getId(),
                order.getSettlementNo(),
                order.getUserId(),
                userName,
                order.getRentalOrderId(),
                order.getSettlementType(),
                order.getCurrency(),
                order.getPrincipalAmount(),
                order.getProfitAmount(),
                order.getPenaltyAmount(),
                order.getActualSettleAmount(),
                order.getStatus(),
                order.getReviewedBy(),
                order.getReviewedAt(),
                order.getSettledAt(),
                order.getWalletTxNo(),
                order.getRemark(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }

    private AdminCommissionRecordResponse commissionRecordResponse(CommissionRecord record, String userName) {
        return new AdminCommissionRecordResponse(
                record.getId(),
                record.getCommissionNo(),
                record.getBenefitUserId(),
                record.getSourceUserId(),
                userName,
                record.getSourceOrderId(),
                record.getSourceProfitId(),
                record.getLevelNo(),
                record.getCurrency(),
                record.getSourceProfitAmount(),
                record.getCommissionRateSnapshot(),
                record.getCommissionAmount(),
                record.getStatus(),
                record.getWalletTxNo(),
                record.getSettledAt(),
                record.getCreatedAt(),
                record.getUpdatedAt());
    }

    private AdminTeamRelationResponse teamRelationResponse(UserTeamRelation relation) {
        return new AdminTeamRelationResponse(
                relation.getId(),
                relation.getAncestorUserId(),
                relation.getAncestorUserName(),
                relation.getDescendantUserId(),
                relation.getDescendantUserName(),
                relation.getLevelDepth(),
                relation.getCreatedAt());
    }

    private AdminLogResponse adminLogResponse(SysAdminLog log) {
        return new AdminLogResponse(
                log.getId(),
                log.getAdminId(),
                log.getOperatorName(),
                log.getAction(),
                log.getActionName(),
                log.getTargetTable(),
                log.getTargetId(),
                log.getBeforeValue(),
                log.getAfterValue(),
                log.getRemark(),
                log.getIp(),
                log.getCreatedAt());
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

    private Map<Long, UserBrief> userBriefMap(List<Long> userIds) {
        var ids = userIds.stream().filter(id -> id != null).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        var users = new HashMap<Long, UserBrief>();
        for (var user : appUserMapper.selectBatchIds(ids)) {
            users.put(user.getId(), new UserBrief(user.getUserName(), user.getEmail()));
        }
        return users;
    }

    private List<Long> userIdsByKeyword(String keyword) {
        var normalizedKeyword = AppUserSearchSupport.normalize(keyword);
        if (!AppUserSearchSupport.hasText(normalizedKeyword)) {
            return Collections.emptyList();
        }
        return appUserMapper.selectList(AppUserSearchSupport.idQuery(normalizedKeyword))
                .stream()
                .map(AppUser::getId)
                .toList();
    }

    private long countDepth(Iterable<UserTeamRelation> relations, int depth) {
        long count = 0;
        for (var relation : relations) {
            if (Integer.valueOf(depth).equals(relation.getLevelDepth())) {
                count++;
            }
        }
        return count;
    }

    private boolean unsupportedLevel(Integer level) {
        return level != null && (level < 1 || level > MAX_HIERARCHY_LEVEL);
    }

    private boolean hasText(String value) {
        return StringUtils.hasText(value);
    }

    private record AdminTeamAggregate(
            AppUser user,
            TeamCounts counts,
            BigDecimal yesterdayCommission,
            BigDecimal totalCommission,
            TeamOrderStats orderStats,
            LocalDateTime lastCommissionAt
    ) {
        private Long userId() {
            return user.getId();
        }
    }

    private record TeamCounts(long directCount, long indirectCount) {
        private static final TeamCounts ZERO = new TeamCounts(0, 0);

        private long totalCount() {
            return directCount + indirectCount;
        }
    }

    private record TeamOrderStats(long activeOrderCount, long runningOrderCount) {
        private static final TeamOrderStats ZERO = new TeamOrderStats(0, 0);
    }

    private record TeamOrderBrief(String orderStatus, String latestOrderNo) {
        private static TeamOrderBrief none() {
            return new TeamOrderBrief(ORDER_STATUS_NONE, null);
        }
    }

    private record UserBrief(String userName, String email) {
    }
}

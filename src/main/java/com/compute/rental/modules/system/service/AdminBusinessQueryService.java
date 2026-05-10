package com.compute.rental.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.compute.rental.common.enums.ApiTokenStatus;
import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.enums.ProfitStatus;
import com.compute.rental.common.enums.RentalOrderStatus;
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
import com.compute.rental.modules.system.dto.AdminTeamRelationResponse;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminBusinessQueryService {

    private static final int MAX_HIERARCHY_LEVEL = 2;

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

    private Long resolveOrderId(String orderNo) {
        if (!hasText(orderNo)) {
            return null;
        }
        var order = rentalOrderMapper.selectOne(new LambdaQueryWrapper<RentalOrder>()
                .eq(RentalOrder::getOrderNo, orderNo)
                .last("LIMIT 1"));
        return order == null ? null : order.getId();
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

    private record UserBrief(String userName, String email) {
    }
}

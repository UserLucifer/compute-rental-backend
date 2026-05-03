package com.compute.rental.modules.commission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.compute.rental.common.enums.CommissionLevel;
import com.compute.rental.common.enums.CommonStatus;
import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.enums.RecordSettleStatus;
import com.compute.rental.common.enums.WalletBusinessType;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.common.util.DateTimeUtils;
import com.compute.rental.common.util.MoneyUtils;
import com.compute.rental.modules.commission.dto.CommissionRecordQueryRequest;
import com.compute.rental.modules.commission.dto.CommissionRecordResponse;
import com.compute.rental.modules.commission.dto.CommissionSummaryResponse;
import com.compute.rental.modules.commission.entity.CommissionRecord;
import com.compute.rental.modules.commission.entity.CommissionRule;
import com.compute.rental.modules.commission.mapper.CommissionRecordMapper;
import com.compute.rental.modules.commission.mapper.CommissionRuleMapper;
import com.compute.rental.modules.order.entity.RentalProfitRecord;
import com.compute.rental.modules.order.mapper.RentalProfitRecordMapper;
import com.compute.rental.modules.user.entity.UserReferralRelation;
import com.compute.rental.modules.user.entity.AppUser;
import com.compute.rental.modules.user.mapper.AppUserMapper;
import com.compute.rental.modules.user.mapper.UserReferralRelationMapper;
import com.compute.rental.modules.wallet.entity.UserWallet;
import com.compute.rental.modules.wallet.service.WalletService;
import com.compute.rental.modules.wallet.mapper.UserWalletMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommissionService {

    private static final String CURRENCY_USDT = "USDT";
    private static final Map<Integer, BigDecimal> DEFAULT_RATES = Map.of(
            1, new BigDecimal("0.2000"),
            2, new BigDecimal("0.1000"),
            3, new BigDecimal("0.0500")
    );

    private final CommissionRuleMapper commissionRuleMapper;
    private final CommissionRecordMapper commissionRecordMapper;
    private final RentalProfitRecordMapper profitRecordMapper;
    private final UserReferralRelationMapper referralRelationMapper;
    private final AppUserMapper appUserMapper;
    private final UserWalletMapper userWalletMapper;
    private final WalletService walletService;

    public CommissionService(
            CommissionRuleMapper commissionRuleMapper,
            CommissionRecordMapper commissionRecordMapper,
            RentalProfitRecordMapper profitRecordMapper,
            UserReferralRelationMapper referralRelationMapper,
            AppUserMapper appUserMapper,
            UserWalletMapper userWalletMapper,
            WalletService walletService
    ) {
        this.commissionRuleMapper = commissionRuleMapper;
        this.commissionRecordMapper = commissionRecordMapper;
        this.profitRecordMapper = profitRecordMapper;
        this.referralRelationMapper = referralRelationMapper;
        this.appUserMapper = appUserMapper;
        this.userWalletMapper = userWalletMapper;
        this.walletService = walletService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateForProfit(Long profitId) {
        var profit = profitRecordMapper.selectById(profitId);
        if (profit == null || !RecordSettleStatus.SETTLED.name().equals(profit.getStatus())
                || !Integer.valueOf(0).equals(profit.getCommissionGenerated())) {
            return;
        }
        var referral = referralRelationMapper.selectOne(new LambdaQueryWrapper<UserReferralRelation>()
                .eq(UserReferralRelation::getUserId, profit.getUserId())
                .last("LIMIT 1"));
        var upperUsers = upperUsers(referral);
        if (upperUsers.isEmpty()) {
            markGenerated(profit.getId());
            return;
        }
        var rates = enabledRuleRates();
        for (var entry : upperUsers.entrySet()) {
            settleCommission(profit, entry.getKey(), entry.getValue(), rates.get(entry.getKey()));
        }
        markGenerated(profit.getId());
    }

    public PageResult<CommissionRecordResponse> pageUserRecords(Long userId, CommissionRecordQueryRequest request) {
        var page = new Page<CommissionRecord>(request.current(), request.size());
        var wrapper = new LambdaQueryWrapper<CommissionRecord>()
                .eq(CommissionRecord::getBenefitUserId, userId)
                .eq(request.levelNo() != null, CommissionRecord::getLevelNo, request.levelNo())
                .eq(request.status() != null, CommissionRecord::getStatus,
                        request.status() == null ? null : request.status().name())
                .ge(request.startTime() != null, CommissionRecord::getCreatedAt, request.startTime())
                .le(request.endTime() != null, CommissionRecord::getCreatedAt, request.endTime())
                .orderByDesc(CommissionRecord::getId);
        var result = commissionRecordMapper.selectPage(page, wrapper);
        var userNames = userNameMap(result.getRecords().stream().map(CommissionRecord::getSourceUserId).toList());
        return new PageResult<>(result.getRecords().stream()
                .map(record -> toResponse(record, userNames.get(record.getSourceUserId())))
                .toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    public CommissionSummaryResponse summary(Long userId) {
        var records = commissionRecordMapper.selectList(new LambdaQueryWrapper<CommissionRecord>()
                .eq(CommissionRecord::getBenefitUserId, userId)
                .eq(CommissionRecord::getStatus, RecordSettleStatus.SETTLED.name()));
        var today = DateTimeUtils.today();
        var yesterday = today.minusDays(1);
        var monthStart = today.withDayOfMonth(1);
        return new CommissionSummaryResponse(
                walletTotalCommission(userId),
                sumByDate(records, today),
                sumByDate(records, yesterday),
                sumFromDate(records, monthStart),
                sumByLevel(records, 1),
                sumByLevel(records, 2),
                sumByLevel(records, 3)
        );
    }

    private void settleCommission(RentalProfitRecord profit, Integer levelNo, Long benefitUserId, BigDecimal rate) {
        var existing = commissionRecordMapper.selectOne(new LambdaQueryWrapper<CommissionRecord>()
                .eq(CommissionRecord::getSourceProfitId, profit.getId())
                .eq(CommissionRecord::getLevelNo, levelNo)
                .last("LIMIT 1"));
        var record = existing == null ? buildRecord(profit, levelNo, benefitUserId, rate) : existing;
        if (existing == null) {
            try {
                commissionRecordMapper.insert(record);
            } catch (DuplicateKeyException ex) {
                record = commissionRecordMapper.selectOne(new LambdaQueryWrapper<CommissionRecord>()
                        .eq(CommissionRecord::getSourceProfitId, profit.getId())
                        .eq(CommissionRecord::getLevelNo, levelNo)
                        .last("LIMIT 1"));
            }
        }
        if (record == null || RecordSettleStatus.SETTLED.name().equals(record.getStatus())) {
            return;
        }
        var tx = walletService.creditWithIdempotencyKey(
                benefitUserId,
                record.getCommissionAmount(),
                WalletBusinessType.COMMISSION_PROFIT,
                record.getCommissionNo(),
                "COMMISSION_PROFIT:" + profit.getId() + ":" + levelNo,
                "Commission profit"
        );
        commissionRecordMapper.update(null, new LambdaUpdateWrapper<CommissionRecord>()
                .eq(CommissionRecord::getId, record.getId())
                .eq(CommissionRecord::getStatus, RecordSettleStatus.PENDING.name())
                .set(CommissionRecord::getStatus, RecordSettleStatus.SETTLED.name())
                .set(CommissionRecord::getWalletTxNo, tx.getTxNo())
                .set(CommissionRecord::getSettledAt, DateTimeUtils.now())
                .set(CommissionRecord::getUpdatedAt, DateTimeUtils.now()));
        // TODO: create sys_notification COMMISSION_SUCCESS after notification service is implemented.
    }

    private CommissionRecord buildRecord(RentalProfitRecord profit, Integer levelNo, Long benefitUserId, BigDecimal rate) {
        var now = DateTimeUtils.now();
        var record = new CommissionRecord();
        record.setCommissionNo(generateCommissionNo());
        record.setBenefitUserId(benefitUserId);
        record.setSourceUserId(profit.getUserId());
        record.setSourceOrderId(profit.getRentalOrderId());
        record.setSourceProfitId(profit.getId());
        record.setLevelNo(levelNo);
        record.setCurrency(CURRENCY_USDT);
        record.setSourceProfitAmount(MoneyUtils.scale(profit.getFinalProfitAmount()));
        record.setCommissionRateSnapshot(rate);
        record.setCommissionAmount(MoneyUtils.scale(profit.getFinalProfitAmount().multiply(rate)));
        record.setStatus(RecordSettleStatus.PENDING.name());
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        return record;
    }

    private Map<Integer, Long> upperUsers(UserReferralRelation referral) {
        var result = new HashMap<Integer, Long>();
        if (referral == null) {
            return result;
        }
        putIfPresent(result, CommissionLevel.LEVEL_1.levelNo(), referral.getLevel1UserId());
        putIfPresent(result, CommissionLevel.LEVEL_2.levelNo(), referral.getLevel2UserId());
        putIfPresent(result, CommissionLevel.LEVEL_3.levelNo(), referral.getLevel3UserId());
        return result;
    }

    private void putIfPresent(Map<Integer, Long> map, int levelNo, Long userId) {
        if (userId != null) {
            map.put(levelNo, userId);
        }
    }

    private Map<Integer, BigDecimal> enabledRuleRates() {
        var rates = new HashMap<>(DEFAULT_RATES);
        commissionRuleMapper.selectList(new LambdaQueryWrapper<CommissionRule>()
                        .in(CommissionRule::getLevelNo, 1, 2, 3)
                        .eq(CommissionRule::getStatus, CommonStatus.ENABLED.value()))
                .forEach(rule -> rates.put(rule.getLevelNo(), rule.getCommissionRate()));
        return rates;
    }

    private void markGenerated(Long profitId) {
        profitRecordMapper.update(null, new LambdaUpdateWrapper<RentalProfitRecord>()
                .eq(RentalProfitRecord::getId, profitId)
                .eq(RentalProfitRecord::getCommissionGenerated, 0)
                .set(RentalProfitRecord::getCommissionGenerated, 1)
                .set(RentalProfitRecord::getUpdatedAt, DateTimeUtils.now()));
    }

    private BigDecimal walletTotalCommission(Long userId) {
        var wallet = userWalletMapper.selectOne(new LambdaQueryWrapper<UserWallet>()
                .eq(UserWallet::getUserId, userId)
                .last("LIMIT 1"));
        return wallet == null ? MoneyUtils.ZERO : MoneyUtils.scale(wallet.getTotalCommission());
    }

    private BigDecimal sumByDate(List<CommissionRecord> records, LocalDate date) {
        return MoneyUtils.scale(records.stream()
                .filter(record -> record.getSettledAt() != null && date.equals(record.getSettledAt().toLocalDate()))
                .map(CommissionRecord::getCommissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal sumFromDate(List<CommissionRecord> records, LocalDate startDate) {
        return MoneyUtils.scale(records.stream()
                .filter(record -> record.getSettledAt() != null && !record.getSettledAt().toLocalDate().isBefore(startDate))
                .map(CommissionRecord::getCommissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal sumByLevel(List<CommissionRecord> records, int levelNo) {
        return MoneyUtils.scale(records.stream()
                .filter(record -> Integer.valueOf(levelNo).equals(record.getLevelNo()))
                .map(CommissionRecord::getCommissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private CommissionRecordResponse toResponse(CommissionRecord record) {
        return toResponse(record, userName(record.getSourceUserId()));
    }

    private CommissionRecordResponse toResponse(CommissionRecord record, String userName) {
        return new CommissionRecordResponse(
                record.getCommissionNo(),
                record.getSourceUserId(),
                userName,
                record.getSourceOrderId(),
                record.getSourceProfitId(),
                record.getLevelNo(),
                record.getSourceProfitAmount(),
                record.getCommissionRateSnapshot(),
                record.getCommissionAmount(),
                record.getStatus(),
                record.getWalletTxNo(),
                record.getSettledAt(),
                record.getCreatedAt()
        );
    }

    private String generateCommissionNo() {
        return "CM" + DateTimeUtils.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
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
}

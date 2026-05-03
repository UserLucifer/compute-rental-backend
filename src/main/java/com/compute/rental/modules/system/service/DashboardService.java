package com.compute.rental.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.compute.rental.common.enums.BlogPublishStatus;
import com.compute.rental.common.enums.CommonStatus;
import com.compute.rental.common.enums.ProfitStatus;
import com.compute.rental.common.enums.RechargeOrderStatus;
import com.compute.rental.common.enums.RecordSettleStatus;
import com.compute.rental.common.enums.RentalOrderStatus;
import com.compute.rental.common.enums.WithdrawOrderStatus;
import com.compute.rental.common.util.DateTimeUtils;
import com.compute.rental.common.util.MoneyUtils;
import com.compute.rental.modules.commission.entity.CommissionRecord;
import com.compute.rental.modules.commission.mapper.CommissionRecordMapper;
import com.compute.rental.modules.order.entity.RentalOrder;
import com.compute.rental.modules.order.entity.RentalProfitRecord;
import com.compute.rental.modules.order.mapper.RentalOrderMapper;
import com.compute.rental.modules.order.mapper.RentalProfitRecordMapper;
import com.compute.rental.modules.user.entity.AppUser;
import com.compute.rental.modules.user.entity.UserReferralRelation;
import com.compute.rental.modules.user.mapper.AppUserMapper;
import com.compute.rental.modules.user.mapper.UserReferralRelationMapper;
import com.compute.rental.modules.wallet.entity.RechargeOrder;
import com.compute.rental.modules.wallet.entity.UserWallet;
import com.compute.rental.modules.wallet.entity.WithdrawOrder;
import com.compute.rental.modules.wallet.mapper.RechargeOrderMapper;
import com.compute.rental.modules.wallet.mapper.UserWalletMapper;
import com.compute.rental.modules.wallet.mapper.WithdrawOrderMapper;
import com.compute.rental.modules.system.dto.DashboardFinanceResponse;
import com.compute.rental.modules.system.dto.DashboardOrdersResponse;
import com.compute.rental.modules.system.dto.DashboardOverviewResponse;
import com.compute.rental.modules.system.dto.DashboardStatusCountResponse;
import com.compute.rental.modules.system.dto.DashboardUsersResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final AppUserMapper appUserMapper;
    private final UserReferralRelationMapper referralRelationMapper;
    private final UserWalletMapper userWalletMapper;
    private final RechargeOrderMapper rechargeOrderMapper;
    private final WithdrawOrderMapper withdrawOrderMapper;
    private final RentalOrderMapper rentalOrderMapper;
    private final RentalProfitRecordMapper profitRecordMapper;
    private final CommissionRecordMapper commissionRecordMapper;

    public DashboardService(
            AppUserMapper appUserMapper,
            UserReferralRelationMapper referralRelationMapper,
            UserWalletMapper userWalletMapper,
            RechargeOrderMapper rechargeOrderMapper,
            WithdrawOrderMapper withdrawOrderMapper,
            RentalOrderMapper rentalOrderMapper,
            RentalProfitRecordMapper profitRecordMapper,
            CommissionRecordMapper commissionRecordMapper
    ) {
        this.appUserMapper = appUserMapper;
        this.referralRelationMapper = referralRelationMapper;
        this.userWalletMapper = userWalletMapper;
        this.rechargeOrderMapper = rechargeOrderMapper;
        this.withdrawOrderMapper = withdrawOrderMapper;
        this.rentalOrderMapper = rentalOrderMapper;
        this.profitRecordMapper = profitRecordMapper;
        this.commissionRecordMapper = commissionRecordMapper;
    }

    public DashboardOverviewResponse overview() {
        return new DashboardOverviewResponse(
                appUserMapper.selectCount(null),
                countUsers(CommonStatus.ENABLED.value()),
                countUsers(CommonStatus.DISABLED.value()),
                totalRechargeAmount(),
                totalWithdrawAmount(),
                totalOrderAmount(),
                totalProfitAmount(),
                totalCommissionAmount(),
                countOrdersByStatus(RentalOrderStatus.RUNNING.name()),
                countRechargeByStatus(RechargeOrderStatus.SUBMITTED.name()),
                countWithdrawByStatus(WithdrawOrderStatus.PENDING_REVIEW.name()));
    }

    public DashboardFinanceResponse finance() {
        var today = DateTimeUtils.today();
        return new DashboardFinanceResponse(
                todayRechargeAmount(today),
                todayWithdrawAmount(today),
                todayProfitAmount(today),
                todayCommissionAmount(today),
                walletAvailableBalance(),
                walletFrozenBalance());
    }

    public DashboardOrdersResponse orders() {
        var todayRange = todayRange();
        return new DashboardOrdersResponse(
                orderStatusCounts(),
                profitStatusCounts(),
                rentalOrderMapper.selectCount(new LambdaQueryWrapper<RentalOrder>()
                        .ge(RentalOrder::getCreatedAt, todayRange.start())
                        .le(RentalOrder::getCreatedAt, todayRange.end())),
                rentalOrderMapper.selectCount(new LambdaQueryWrapper<RentalOrder>()
                        .ge(RentalOrder::getPaidAt, todayRange.start())
                        .le(RentalOrder::getPaidAt, todayRange.end())),
                countOrdersByStatus(RentalOrderStatus.RUNNING.name()),
                countOrdersByStatus(RentalOrderStatus.PAUSED.name()));
    }

    public DashboardUsersResponse users() {
        var todayRange = todayRange();
        var monthStart = DateTimeUtils.today().withDayOfMonth(1).atStartOfDay();
        return new DashboardUsersResponse(
                appUserMapper.selectCount(new LambdaQueryWrapper<AppUser>()
                        .ge(AppUser::getCreatedAt, todayRange.start())
                        .le(AppUser::getCreatedAt, todayRange.end())),
                appUserMapper.selectCount(new LambdaQueryWrapper<AppUser>()
                        .ge(AppUser::getCreatedAt, monthStart)
                        .le(AppUser::getCreatedAt, todayRange.end())),
                countUsers(CommonStatus.ENABLED.value()),
                countUsers(CommonStatus.DISABLED.value()),
                referralRelationMapper.selectCount(new LambdaQueryWrapper<UserReferralRelation>()
                        .isNotNull(UserReferralRelation::getParentUserId)),
                referralRelationMapper.selectCount(new LambdaQueryWrapper<UserReferralRelation>()
                        .isNull(UserReferralRelation::getParentUserId)));
    }

    private Long countUsers(Integer status) {
        return appUserMapper.selectCount(new LambdaQueryWrapper<AppUser>().eq(AppUser::getStatus, status));
    }

    private Long countOrdersByStatus(String status) {
        return rentalOrderMapper.selectCount(new LambdaQueryWrapper<RentalOrder>().eq(RentalOrder::getOrderStatus, status));
    }

    private Long countRechargeByStatus(String status) {
        return rechargeOrderMapper.selectCount(new LambdaQueryWrapper<RechargeOrder>().eq(RechargeOrder::getStatus, status));
    }

    private Long countWithdrawByStatus(String status) {
        return withdrawOrderMapper.selectCount(new LambdaQueryWrapper<WithdrawOrder>().eq(WithdrawOrder::getStatus, status));
    }

    private BigDecimal totalRechargeAmount() {
        return sumRecharge(rechargeOrderMapper.selectList(new LambdaQueryWrapper<RechargeOrder>()
                .eq(RechargeOrder::getStatus, RechargeOrderStatus.APPROVED.name())));
    }

    private BigDecimal totalWithdrawAmount() {
        return sumWithdraw(withdrawOrderMapper.selectList(new LambdaQueryWrapper<WithdrawOrder>()
                .eq(WithdrawOrder::getStatus, WithdrawOrderStatus.PAID.name())));
    }

    private BigDecimal totalOrderAmount() {
        return MoneyUtils.scale(rentalOrderMapper.selectList(new LambdaQueryWrapper<RentalOrder>()
                        .isNotNull(RentalOrder::getPaidAt))
                .stream()
                .map(RentalOrder::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal totalProfitAmount() {
        return MoneyUtils.scale(profitRecordMapper.selectList(new LambdaQueryWrapper<RentalProfitRecord>()
                        .eq(RentalProfitRecord::getStatus, RecordSettleStatus.SETTLED.name()))
                .stream()
                .map(RentalProfitRecord::getFinalProfitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal totalCommissionAmount() {
        return MoneyUtils.scale(commissionRecordMapper.selectList(new LambdaQueryWrapper<CommissionRecord>()
                        .eq(CommissionRecord::getStatus, RecordSettleStatus.SETTLED.name()))
                .stream()
                .map(CommissionRecord::getCommissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal todayRechargeAmount(LocalDate today) {
        return sumRecharge(rechargeOrderMapper.selectList(new LambdaQueryWrapper<RechargeOrder>()
                .eq(RechargeOrder::getStatus, RechargeOrderStatus.APPROVED.name())
                .ge(RechargeOrder::getCreditedAt, today.atStartOfDay())
                .le(RechargeOrder::getCreditedAt, today.atTime(LocalTime.MAX))));
    }

    private BigDecimal todayWithdrawAmount(LocalDate today) {
        return sumWithdraw(withdrawOrderMapper.selectList(new LambdaQueryWrapper<WithdrawOrder>()
                .eq(WithdrawOrder::getStatus, WithdrawOrderStatus.PAID.name())
                .ge(WithdrawOrder::getPaidAt, today.atStartOfDay())
                .le(WithdrawOrder::getPaidAt, today.atTime(LocalTime.MAX))));
    }

    private BigDecimal todayProfitAmount(LocalDate today) {
        return MoneyUtils.scale(profitRecordMapper.selectList(new LambdaQueryWrapper<RentalProfitRecord>()
                        .eq(RentalProfitRecord::getStatus, RecordSettleStatus.SETTLED.name())
                        .eq(RentalProfitRecord::getProfitDate, today))
                .stream()
                .map(RentalProfitRecord::getFinalProfitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal todayCommissionAmount(LocalDate today) {
        return MoneyUtils.scale(commissionRecordMapper.selectList(new LambdaQueryWrapper<CommissionRecord>()
                        .eq(CommissionRecord::getStatus, RecordSettleStatus.SETTLED.name())
                        .ge(CommissionRecord::getSettledAt, today.atStartOfDay())
                        .le(CommissionRecord::getSettledAt, today.atTime(LocalTime.MAX)))
                .stream()
                .map(CommissionRecord::getCommissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal walletAvailableBalance() {
        return MoneyUtils.scale(userWalletMapper.selectList(new LambdaQueryWrapper<UserWallet>())
                .stream()
                .map(UserWallet::getAvailableBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal walletFrozenBalance() {
        return MoneyUtils.scale(userWalletMapper.selectList(new LambdaQueryWrapper<UserWallet>())
                .stream()
                .map(UserWallet::getFrozenBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal sumRecharge(Iterable<RechargeOrder> orders) {
        var sum = BigDecimal.ZERO;
        for (var order : orders) {
            if (order.getActualAmount() != null) {
                sum = sum.add(order.getActualAmount());
            }
        }
        return MoneyUtils.scale(sum);
    }

    private BigDecimal sumWithdraw(Iterable<WithdrawOrder> orders) {
        var sum = BigDecimal.ZERO;
        for (var order : orders) {
            if (order.getActualAmount() != null) {
                sum = sum.add(order.getActualAmount());
            }
        }
        return MoneyUtils.scale(sum);
    }

    private List<DashboardStatusCountResponse> orderStatusCounts() {
        return Arrays.stream(RentalOrderStatus.values())
                .map(status -> new DashboardStatusCountResponse(status.name(), countOrdersByStatus(status.name())))
                .toList();
    }

    private List<DashboardStatusCountResponse> profitStatusCounts() {
        return Arrays.stream(ProfitStatus.values())
                .map(status -> new DashboardStatusCountResponse(status.name(), rentalOrderMapper.selectCount(
                        new LambdaQueryWrapper<RentalOrder>().eq(RentalOrder::getProfitStatus, status.name()))))
                .toList();
    }

    private DateRange todayRange() {
        var today = DateTimeUtils.today();
        return new DateRange(today.atStartOfDay(), today.atTime(LocalTime.MAX));
    }

    private record DateRange(LocalDateTime start, LocalDateTime end) {
    }
}

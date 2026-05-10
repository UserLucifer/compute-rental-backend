package com.compute.rental.modules.system.service;

import com.compute.rental.common.enums.ApiDeployOrderStatus;
import com.compute.rental.common.enums.ApiTokenStatus;
import com.compute.rental.common.enums.BlogPublishStatus;
import com.compute.rental.common.enums.CommissionLevel;
import com.compute.rental.common.enums.CommonStatus;
import com.compute.rental.common.enums.DeviceType;
import com.compute.rental.common.enums.DocPublishStatus;
import com.compute.rental.common.enums.DocLanguage;
import com.compute.rental.common.enums.DocSection;
import com.compute.rental.common.enums.EmailVerifyScene;
import com.compute.rental.common.enums.NotificationBizType;
import com.compute.rental.common.enums.NotificationType;
import com.compute.rental.common.enums.ProfitStatus;
import com.compute.rental.common.enums.ReadStatus;
import com.compute.rental.common.enums.RechargeOrderStatus;
import com.compute.rental.common.enums.RecordSettleStatus;
import com.compute.rental.common.enums.RentalOrderSettlementStatus;
import com.compute.rental.common.enums.RentalOrderStatus;
import com.compute.rental.common.enums.RentalSettlementOrderStatus;
import com.compute.rental.common.enums.RentalSettlementType;
import com.compute.rental.common.enums.WalletBusinessType;
import com.compute.rental.common.enums.WalletTransactionType;
import com.compute.rental.common.enums.WithdrawOrderStatus;
import com.compute.rental.common.i18n.LanguageResolver;
import com.compute.rental.modules.system.dto.EnumOptionResponse;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SystemEnumService {

    private static final List<RentalOrderStatus> FRONT_RENTAL_ORDER_STATUSES = List.of(
            RentalOrderStatus.PENDING_PAY,
            RentalOrderStatus.PENDING_ACTIVATION,
            RentalOrderStatus.ACTIVATING,
            RentalOrderStatus.PAUSED,
            RentalOrderStatus.RUNNING,
            RentalOrderStatus.EXPIRED,
            RentalOrderStatus.EARLY_CLOSED,
            RentalOrderStatus.CANCELED
    );

    public Map<String, List<EnumOptionResponse>> frontendEnums() {
        return frontendEnums(LanguageResolver.DEFAULT_LANGUAGE);
    }

    public Map<String, List<EnumOptionResponse>> frontendEnums(String locale) {
        var result = new LinkedHashMap<String, List<EnumOptionResponse>>();
        result.put("commonStatus", options(CommonStatus.values(), locale));
        result.put("blogPublishStatus", options(BlogPublishStatus.values(), locale));
        result.put("docPublishStatus", options(DocPublishStatus.values(), locale));
        result.put("docLanguage", options(DocLanguage.values(), locale));
        result.put("docSection", options(DocSection.values(), locale));
        result.put("commissionLevel", options(CommissionLevel.values(), locale));
        result.put("deviceType", options(DeviceType.values(), locale));
        result.put("emailVerifyScene", options(EmailVerifyScene.values(), locale));
        result.put("notificationBizType", options(NotificationBizType.values(), locale));
        result.put("notificationType", options(NotificationType.values(), locale));
        result.put("profitStatus", options(ProfitStatus.values(), locale));
        result.put("readStatus", options(ReadStatus.values(), locale));
        result.put("rechargeOrderStatus", options(RechargeOrderStatus.values(), locale));
        result.put("recordSettleStatus", options(RecordSettleStatus.values(), locale));
        result.put("rentalOrderSettlementStatus", options(RentalOrderSettlementStatus.values(), locale));
        result.put("rentalOrderStatus", options(FRONT_RENTAL_ORDER_STATUSES, locale));
        result.put("rentalSettlementOrderStatus", options(RentalSettlementOrderStatus.values(), locale));
        result.put("rentalSettlementType", options(RentalSettlementType.values(), locale));
        result.put("walletBusinessType", options(WalletBusinessType.values(), locale));
        result.put("walletTransactionType", options(WalletTransactionType.values(), locale));
        result.put("withdrawOrderStatus", options(WithdrawOrderStatus.values(), locale));
        result.put("apiDeployOrderStatus", options(ApiDeployOrderStatus.values(), locale));
        result.put("apiTokenStatus", options(ApiTokenStatus.values(), locale));
        return result;
    }

    private List<EnumOptionResponse> options(Enum<?>[] values, String locale) {
        return options(Arrays.asList(values), locale);
    }

    private List<EnumOptionResponse> options(List<? extends Enum<?>> values, String locale) {
        return values.stream()
                .map(value -> new EnumOptionResponse(value.name(), frontendValue(value), label(value, locale)))
                .toList();
    }

    private Object frontendValue(Enum<?> value) {
        for (var methodName : List.of("value", "levelNo")) {
            try {
                Method method = value.getClass().getMethod(methodName);
                return method.invoke(value);
            } catch (ReflectiveOperationException ignored) {
                // Fall through to the next supported value accessor.
            }
        }
        return value.name();
    }

    private String label(Enum<?> value, String locale) {
        if (LanguageResolver.EN_US.equals(locale)) {
            return englishLabel(value);
        }
        return chineseLabel(value);
    }

    private String chineseLabel(Enum<?> value) {
        return switch (value.name()) {
            case "ENABLED" -> "启用";
            case "DISABLED" -> "停用";
            case "DRAFT" -> "草稿";
            case "PUBLISHED" -> "已发布";
            case "OFFLINE" -> "已下线";
            case "ZH_CN" -> "中文";
            case "EN_US" -> "英文";
            case "GUIDE" -> "向导";
            case "INTEGRATION" -> "集成";
            case "FAQ" -> "常见问题";
            case "SUPPORT" -> "支持";
            case "LEVEL_1" -> "一级";
            case "LEVEL_2" -> "二级";
            case "IOS" -> "iOS";
            case "ANDROID" -> "Android";
            case "SIGNUP" -> "注册";
            case "RESET_PASSWORD" -> "重置密码";
            case "FINANCIAL" -> "财务";
            case "SYSTEM" -> "系统";
            case "BLOG" -> "博客";
            case "NOT_STARTED" -> "未开始";
            case "RUNNING" -> "运行中";
            case "PAUSED" -> "已暂停";
            case "FINISHED" -> "已完成";
            case "UNREAD" -> "未读";
            case "READ" -> "已读";
            case "SUBMITTED" -> "已提交";
            case "APPROVED" -> "已审核";
            case "REJECTED" -> "已拒绝";
            case "CANCELED" -> "已取消";
            case "PENDING" -> "待处理";
            case "SETTLED" -> "已结算";
            case "UNSETTLED" -> "未结算";
            case "SETTLING" -> "结算中";
            case "PENDING_PAY" -> "待支付";
            case "PENDING_ACTIVATION" -> "待支付部署费";
            case "ACTIVATING" -> "部署中";
            case "EXPIRED" -> "已到期";
            case "EARLY_CLOSED" -> "提前关闭";
            case "EXPIRE" -> "到期结算";
            case "EARLY_TERMINATE" -> "提前终止";
            case "MANUAL" -> "人工处理";
            case "IN" -> "入账";
            case "OUT" -> "支出";
            case "FREEZE" -> "冻结";
            case "UNFREEZE" -> "解冻";
            case "PENDING_REVIEW" -> "待审核";
            case "PAID" -> "已支付";
            case "REFUNDED" -> "已退款";
            case "GENERATED" -> "已生成";
            case "ACTIVE" -> "生效中";
            case "REVOKED" -> "已撤销";
            case "RECHARGE_SUCCESS" -> "充值成功";
            case "WITHDRAW_SUCCESS" -> "提现成功";
            case "WITHDRAW_REJECTED" -> "提现拒绝";
            case "PROFIT_SUCCESS" -> "收益到账";
            case "COMMISSION_SUCCESS" -> "佣金到账";
            case "API_ACTIVATED" -> "API 已激活";
            case "ORDER_CANCELED" -> "订单取消";
            case "ORDER_EXPIRED" -> "订单到期";
            case "BLOG_UPDATE" -> "博客更新";
            case "RECHARGE" -> "充值";
            case "WITHDRAW" -> "提现";
            case "RENT_PAY" -> "租赁支付";
            case "API_DEPLOY_FEE" -> "API 部署费";
            case "RENT_PROFIT" -> "租赁收益";
            case "COMMISSION_PROFIT" -> "推广佣金";
            case "SETTLEMENT" -> "结算";
            case "EARLY_PENALTY" -> "提前终止违约金";
            case "REFUND" -> "退款";
            case "ADJUST" -> "调账";
            default -> value.name();
        };
    }

    private String englishLabel(Enum<?> value) {
        return switch (value.name()) {
            case "ENABLED" -> "Enabled";
            case "DISABLED" -> "Disabled";
            case "DRAFT" -> "Draft";
            case "PUBLISHED" -> "Published";
            case "OFFLINE" -> "Offline";
            case "ZH_CN" -> "Chinese";
            case "EN_US" -> "English";
            case "GUIDE" -> "Guide";
            case "INTEGRATION" -> "Integration";
            case "FAQ" -> "FAQ";
            case "SUPPORT" -> "Support";
            case "LEVEL_1" -> "Level 1";
            case "LEVEL_2" -> "Level 2";
            case "IOS" -> "iOS";
            case "ANDROID" -> "Android";
            case "SIGNUP" -> "Sign up";
            case "RESET_PASSWORD" -> "Reset password";
            case "FINANCIAL" -> "Financial";
            case "SYSTEM" -> "System";
            case "BLOG" -> "Blog";
            case "NOT_STARTED" -> "Not started";
            case "RUNNING" -> "Running";
            case "PAUSED" -> "Paused";
            case "FINISHED" -> "Finished";
            case "UNREAD" -> "Unread";
            case "READ" -> "Read";
            case "SUBMITTED" -> "Submitted";
            case "APPROVED" -> "Approved";
            case "REJECTED" -> "Rejected";
            case "CANCELED" -> "Canceled";
            case "PENDING" -> "Pending";
            case "SETTLED" -> "Settled";
            case "UNSETTLED" -> "Unsettled";
            case "SETTLING" -> "Settling";
            case "PENDING_PAY" -> "Pending payment";
            case "PENDING_ACTIVATION" -> "Pending deployment payment";
            case "ACTIVATING" -> "Deploying";
            case "EXPIRED" -> "Expired";
            case "EARLY_CLOSED" -> "Closed early";
            case "EXPIRE" -> "Expiration settlement";
            case "EARLY_TERMINATE" -> "Early termination";
            case "MANUAL" -> "Manual";
            case "IN" -> "Credit";
            case "OUT" -> "Debit";
            case "FREEZE" -> "Freeze";
            case "UNFREEZE" -> "Unfreeze";
            case "PENDING_REVIEW" -> "Pending review";
            case "PAID" -> "Paid";
            case "REFUNDED" -> "Refunded";
            case "GENERATED" -> "Generated";
            case "ACTIVE" -> "Active";
            case "REVOKED" -> "Revoked";
            case "RECHARGE_SUCCESS" -> "Recharge successful";
            case "WITHDRAW_SUCCESS" -> "Withdraw successful";
            case "WITHDRAW_REJECTED" -> "Withdraw rejected";
            case "PROFIT_SUCCESS" -> "Profit credited";
            case "COMMISSION_SUCCESS" -> "Commission credited";
            case "API_ACTIVATED" -> "API activated";
            case "ORDER_CANCELED" -> "Order canceled";
            case "ORDER_EXPIRED" -> "Order expired";
            case "BLOG_UPDATE" -> "Blog update";
            case "RECHARGE" -> "Recharge";
            case "WITHDRAW" -> "Withdraw";
            case "RENT_PAY" -> "Rental payment";
            case "API_DEPLOY_FEE" -> "API deployment fee";
            case "RENT_PROFIT" -> "Rental profit";
            case "COMMISSION_PROFIT" -> "Referral commission";
            case "SETTLEMENT" -> "Settlement";
            case "EARLY_PENALTY" -> "Early termination penalty";
            case "REFUND" -> "Refund";
            case "ADJUST" -> "Adjustment";
            default -> value.name();
        };
    }
}

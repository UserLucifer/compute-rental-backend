package com.compute.rental.common.util;

import java.util.Map;

public final class WalletRemarkUtils {

    private static final Map<String, String> REMARKS = Map.ofEntries(
            Map.entry("Activation timeout refund", "激活超时退款"),
            Map.entry("Daily rental profit", "每日租赁收益"),
            Map.entry("Commission profit", "佣金收益"),
            Map.entry("Withdraw freeze", "提现冻结"),
            Map.entry("Withdraw canceled", "提现取消解冻"),
            Map.entry("Withdraw rejected", "提现驳回解冻"),
            Map.entry("Withdraw paid", "提现已打款"),
            Map.entry("Early settlement penalty retained from principal", "提前结算违约金从本金中扣除"),
            Map.entry("Early settlement principal returned", "提前结算本金返还"),
            Map.entry("Expired rental principal returned", "租赁到期本金返还"),
            Map.entry("Rental machine fee paid", "租赁机器费用支付"),
            Map.entry("API deploy fee paid", "API 部署费用支付"),
            Map.entry("Rental order canceled refund", "租赁订单取消退款"),
            Map.entry("Recharge approved", "充值审核通过")
    );

    private WalletRemarkUtils() {
    }

    public static String toChinese(String remark) {
        return remark == null ? null : REMARKS.getOrDefault(remark, remark);
    }
}

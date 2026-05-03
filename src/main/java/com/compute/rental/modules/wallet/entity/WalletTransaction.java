package com.compute.rental.modules.wallet.entity;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;

@Getter
@Setter
@TableName("wallet_transaction")
public class WalletTransaction {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("tx_no")
    private String txNo;

    @TableField("idempotency_key")
    private String idempotencyKey;

    @TableField("user_id")
    private Long userId;

    @TableField(exist = false)
    private String userName;

    @TableField("wallet_id")
    private Long walletId;

    @TableField("currency")
    private String currency;

    @TableField("tx_type")
    private String txType;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("before_available_balance")
    private BigDecimal beforeAvailableBalance;

    @TableField("after_available_balance")
    private BigDecimal afterAvailableBalance;

    @TableField("before_frozen_balance")
    private BigDecimal beforeFrozenBalance;

    @TableField("after_frozen_balance")
    private BigDecimal afterFrozenBalance;

    @TableField("biz_type")
    private String bizType;

    @TableField("biz_order_no")
    private String bizOrderNo;

    @TableField("remark")
    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;

}

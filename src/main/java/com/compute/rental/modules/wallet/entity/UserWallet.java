package com.compute.rental.modules.wallet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("user_wallet")
public class UserWallet {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("wallet_no")
    private String walletNo;

    @TableField("user_id")
    private Long userId;

    @TableField("currency")
    private String currency;

    @TableField("available_balance")
    private BigDecimal availableBalance;

    @TableField("frozen_balance")
    private BigDecimal frozenBalance;

    @TableField("total_recharge")
    private BigDecimal totalRecharge;

    @TableField("total_withdraw")
    private BigDecimal totalWithdraw;

    @TableField("total_profit")
    private BigDecimal totalProfit;

    @TableField("total_commission")
    private BigDecimal totalCommission;

    @TableField("status")
    private Integer status;

    @Version
    @TableField("version_no")
    private Integer versionNo;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

}

package com.compute.rental.modules.wallet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("withdraw_order")
public class WithdrawOrder {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("withdraw_no")
    private String withdrawNo;

    @TableField("user_id")
    private Long userId;

    @TableField(exist = false)
    private String userName;

    @TableField("wallet_id")
    private Long walletId;

    @TableField("currency")
    private String currency;

    @TableField("withdraw_method")
    private String withdrawMethod;

    @TableField("network")
    private String network;

    @TableField("account_name")
    private String accountName;

    @TableField("account_no")
    private String accountNo;

    @TableField("apply_amount")
    private BigDecimal applyAmount;

    @TableField("fee_amount")
    private BigDecimal feeAmount;

    @TableField("actual_amount")
    private BigDecimal actualAmount;

    @TableField("status")
    private String status;

    @TableField("freeze_tx_no")
    private String freezeTxNo;

    @TableField("unfreeze_tx_no")
    private String unfreezeTxNo;

    @TableField("paid_tx_no")
    private String paidTxNo;

    @TableField("reviewed_by")
    private Long reviewedBy;

    @TableField("reviewed_at")
    private LocalDateTime reviewedAt;

    @TableField("review_remark")
    private String reviewRemark;

    @TableField("paid_at")
    private LocalDateTime paidAt;

    @TableField("pay_proof_no")
    private String payProofNo;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

}

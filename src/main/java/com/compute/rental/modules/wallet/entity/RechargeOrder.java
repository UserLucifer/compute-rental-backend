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
@TableName("recharge_order")
public class RechargeOrder {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("recharge_no")
    private String rechargeNo;

    @TableField("user_id")
    private Long userId;

    @TableField(exist = false)
    private String userName;

    @TableField("wallet_id")
    private Long walletId;

    @TableField("channel_id")
    private Long channelId;

    @TableField("currency")
    private String currency;

    @TableField("channel_name_snapshot")
    private String channelNameSnapshot;

    @TableField("network_snapshot")
    private String networkSnapshot;

    @TableField("display_url_snapshot")
    private String displayUrlSnapshot;

    @TableField("account_no_snapshot")
    private String accountNoSnapshot;

    @TableField("apply_amount")
    private BigDecimal applyAmount;

    @TableField("actual_amount")
    private BigDecimal actualAmount;

    @TableField("external_tx_no")
    private String externalTxNo;

    @TableField("payment_proof_url")
    private String paymentProofUrl;

    @TableField("user_remark")
    private String userRemark;

    @TableField("status")
    private String status;

    @TableField("reviewed_by")
    private Long reviewedBy;

    @TableField("reviewed_at")
    private LocalDateTime reviewedAt;

    @TableField("review_remark")
    private String reviewRemark;

    @TableField("credited_at")
    private LocalDateTime creditedAt;

    @TableField("wallet_tx_no")
    private String walletTxNo;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

}

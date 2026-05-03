package com.compute.rental.modules.order.entity;

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
@TableName("rental_settlement_order")
public class RentalSettlementOrder {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("settlement_no")
    private String settlementNo;

    @TableField("user_id")
    private Long userId;

    @TableField(exist = false)
    private String userName;

    @TableField("rental_order_id")
    private Long rentalOrderId;

    @TableField("settlement_type")
    private String settlementType;

    @TableField("currency")
    private String currency;

    @TableField("principal_amount")
    private BigDecimal principalAmount;

    @TableField("profit_amount")
    private BigDecimal profitAmount;

    @TableField("penalty_amount")
    private BigDecimal penaltyAmount;

    @TableField("actual_settle_amount")
    private BigDecimal actualSettleAmount;

    @TableField("status")
    private String status;

    @TableField("reviewed_by")
    private Long reviewedBy;

    @TableField("reviewed_at")
    private LocalDateTime reviewedAt;

    @TableField("settled_at")
    private LocalDateTime settledAt;

    @TableField("wallet_tx_no")
    private String walletTxNo;

    @TableField("remark")
    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

}

package com.compute.rental.modules.commission.entity;

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
@TableName("commission_record")
public class CommissionRecord {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("commission_no")
    private String commissionNo;

    @TableField("benefit_user_id")
    private Long benefitUserId;

    @TableField("source_user_id")
    private Long sourceUserId;

    @TableField(exist = false)
    private String userName;

    @TableField("source_order_id")
    private Long sourceOrderId;

    @TableField("source_profit_id")
    private Long sourceProfitId;

    @TableField("level_no")
    private Integer levelNo;

    @TableField("currency")
    private String currency;

    @TableField("source_profit_amount")
    private BigDecimal sourceProfitAmount;

    @TableField("commission_rate_snapshot")
    private BigDecimal commissionRateSnapshot;

    @TableField("commission_amount")
    private BigDecimal commissionAmount;

    @TableField("status")
    private String status;

    @TableField("wallet_tx_no")
    private String walletTxNo;

    @TableField("settled_at")
    private LocalDateTime settledAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

}

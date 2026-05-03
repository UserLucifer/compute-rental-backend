package com.compute.rental.modules.order.entity;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("api_deploy_order")
public class ApiDeployOrder {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("deploy_no")
    private String deployNo;

    @TableField("user_id")
    private Long userId;

    @TableField(exist = false)
    private String userName;

    @TableField("rental_order_id")
    private Long rentalOrderId;

    @TableField("api_credential_id")
    private Long apiCredentialId;

    @TableField("ai_model_id")
    private Long aiModelId;

    @TableField("model_name_snapshot")
    private String modelNameSnapshot;

    @TableField("currency")
    private String currency;

    @TableField("deploy_fee_amount")
    private BigDecimal deployFeeAmount;

    @TableField("status")
    private String status;

    @TableField("wallet_tx_no")
    private String walletTxNo;

    @TableField("paid_at")
    private LocalDateTime paidAt;

    @TableField("canceled_at")
    private LocalDateTime canceledAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

}

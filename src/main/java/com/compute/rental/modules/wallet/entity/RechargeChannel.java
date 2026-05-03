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
@TableName("recharge_channel")
public class RechargeChannel {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("channel_code")
    private String channelCode;

    @TableField("channel_name")
    private String channelName;

    @TableField("network")
    private String network;

    @TableField("display_url")
    private String displayUrl;

    @TableField("account_name")
    private String accountName;

    @TableField("account_no")
    private String accountNo;

    @TableField("min_amount")
    private BigDecimal minAmount;

    @TableField("max_amount")
    private BigDecimal maxAmount;

    @TableField("fee_rate")
    private BigDecimal feeRate;

    @TableField("sort_no")
    private Integer sortNo;

    @TableField("status")
    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

}

package com.compute.rental.modules.order.entity;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("rental_profit_record")
public class RentalProfitRecord {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("profit_no")
    private String profitNo;

    @TableField("user_id")
    private Long userId;

    @TableField(exist = false)
    private String userName;

    @TableField("rental_order_id")
    private Long rentalOrderId;

    @TableField("profit_date")
    private LocalDate profitDate;

    @TableField("gpu_daily_token_snapshot")
    private Long gpuDailyTokenSnapshot;

    @TableField("token_price_snapshot")
    private BigDecimal tokenPriceSnapshot;

    @TableField("yield_multiplier_snapshot")
    private BigDecimal yieldMultiplierSnapshot;

    @TableField("base_profit_amount")
    private BigDecimal baseProfitAmount;

    @TableField("final_profit_amount")
    private BigDecimal finalProfitAmount;

    @TableField("status")
    private String status;

    @TableField("wallet_tx_no")
    private String walletTxNo;

    @TableField("commission_generated")
    private Integer commissionGenerated;

    @TableField("settled_at")
    private LocalDateTime settledAt;

    @TableField("remark")
    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

}

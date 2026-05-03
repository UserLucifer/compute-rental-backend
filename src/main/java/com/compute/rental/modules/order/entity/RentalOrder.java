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
@TableName("rental_order")
public class RentalOrder {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("order_no")
    private String orderNo;

    @TableField("user_id")
    private Long userId;

    @TableField(exist = false)
    private String userName;

    @TableField("product_id")
    private Long productId;

    @TableField("ai_model_id")
    private Long aiModelId;

    @TableField("cycle_rule_id")
    private Long cycleRuleId;

    @TableField("product_code_snapshot")
    private String productCodeSnapshot;

    @TableField("product_name_snapshot")
    private String productNameSnapshot;

    @TableField("machine_code_snapshot")
    private String machineCodeSnapshot;

    @TableField("machine_alias_snapshot")
    private String machineAliasSnapshot;

    @TableField("region_name_snapshot")
    private String regionNameSnapshot;

    @TableField("gpu_model_snapshot")
    private String gpuModelSnapshot;

    @TableField("gpu_memory_snapshot_gb")
    private Integer gpuMemorySnapshotGb;

    @TableField("gpu_power_tops_snapshot")
    private BigDecimal gpuPowerTopsSnapshot;

    @TableField("gpu_rent_price_snapshot")
    private BigDecimal gpuRentPriceSnapshot;

    @TableField("token_output_per_day_snapshot")
    private Long tokenOutputPerDaySnapshot;

    @TableField("ai_model_name_snapshot")
    private String aiModelNameSnapshot;

    @TableField("ai_vendor_name_snapshot")
    private String aiVendorNameSnapshot;

    @TableField("monthly_token_consumption_snapshot")
    private BigDecimal monthlyTokenConsumptionSnapshot;

    @TableField("token_unit_price_snapshot")
    private BigDecimal tokenUnitPriceSnapshot;

    @TableField("deploy_fee_snapshot")
    private BigDecimal deployFeeSnapshot;

    @TableField("cycle_days_snapshot")
    private Integer cycleDaysSnapshot;

    @TableField("yield_multiplier_snapshot")
    private BigDecimal yieldMultiplierSnapshot;

    @TableField("early_penalty_rate_snapshot")
    private BigDecimal earlyPenaltyRateSnapshot;

    @TableField("currency")
    private String currency;

    @TableField("order_amount")
    private BigDecimal orderAmount;

    @TableField("paid_amount")
    private BigDecimal paidAmount;

    @TableField("expected_daily_profit")
    private BigDecimal expectedDailyProfit;

    @TableField("expected_total_profit")
    private BigDecimal expectedTotalProfit;

    @TableField("order_status")
    private String orderStatus;

    @TableField("profit_status")
    private String profitStatus;

    @TableField("settlement_status")
    private String settlementStatus;

    @TableField("machine_pay_tx_no")
    private String machinePayTxNo;

    @TableField("paid_at")
    private LocalDateTime paidAt;

    @TableField("api_generated_at")
    private LocalDateTime apiGeneratedAt;

    @TableField("deploy_fee_paid_at")
    private LocalDateTime deployFeePaidAt;

    @TableField("activated_at")
    private LocalDateTime activatedAt;

    @TableField("auto_pause_at")
    private LocalDateTime autoPauseAt;

    @TableField("paused_at")
    private LocalDateTime pausedAt;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("profit_start_at")
    private LocalDateTime profitStartAt;

    @TableField("profit_end_at")
    private LocalDateTime profitEndAt;

    @TableField("expired_at")
    private LocalDateTime expiredAt;

    @TableField("canceled_at")
    private LocalDateTime canceledAt;

    @TableField("finished_at")
    private LocalDateTime finishedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

}

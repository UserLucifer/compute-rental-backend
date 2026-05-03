package com.compute.rental.modules.product.entity;

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
@TableName("rental_cycle_rule")
public class RentalCycleRule {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("cycle_code")
    private String cycleCode;

    @TableField("cycle_name")
    private String cycleName;

    @TableField("cycle_days")
    private Integer cycleDays;

    @TableField("yield_multiplier")
    private BigDecimal yieldMultiplier;

    @TableField("early_penalty_rate")
    private BigDecimal earlyPenaltyRate;

    @TableField("status")
    private Integer status;

    @TableField("sort_no")
    private Integer sortNo;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

}

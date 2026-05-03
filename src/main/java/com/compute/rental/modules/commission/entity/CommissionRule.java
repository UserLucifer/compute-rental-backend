package com.compute.rental.modules.commission.entity;

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
@TableName("commission_rule")
public class CommissionRule {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("level_no")
    private Integer levelNo;

    @TableField("commission_rate")
    private BigDecimal commissionRate;

    @TableField("status")
    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

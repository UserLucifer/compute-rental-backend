package com.compute.rental.modules.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("rental_cycle_rule_translation")
public class RentalCycleRuleTranslation {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("cycle_rule_id")
    private Long cycleRuleId;

    @TableField("locale")
    private String locale;

    @TableField("cycle_name")
    private String cycleName;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

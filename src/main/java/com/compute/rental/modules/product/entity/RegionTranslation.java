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
@TableName("region_translation")
public class RegionTranslation {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("region_id")
    private Long regionId;

    @TableField("locale")
    private String locale;

    @TableField("region_name")
    private String regionName;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

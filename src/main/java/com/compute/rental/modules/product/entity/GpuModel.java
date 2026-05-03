package com.compute.rental.modules.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("gpu_model")
public class GpuModel {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("model_code")
    private String modelCode;

    @TableField("model_name")
    private String modelName;

    @TableField("sort_no")
    private Integer sortNo;

    @TableField("status")
    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

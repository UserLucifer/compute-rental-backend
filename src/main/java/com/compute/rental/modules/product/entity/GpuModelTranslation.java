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
@TableName("gpu_model_translation")
public class GpuModelTranslation {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("gpu_model_id")
    private Long gpuModelId;

    @TableField("locale")
    private String locale;

    @TableField("model_name")
    private String modelName;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

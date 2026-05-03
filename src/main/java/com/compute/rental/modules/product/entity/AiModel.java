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
@TableName("ai_model")
public class AiModel {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("model_code")
    private String modelCode;

    @TableField("model_name")
    private String modelName;

    @TableField("vendor_name")
    private String vendorName;

    @TableField("logo_url")
    private String logoUrl;

    @TableField("monthly_token_consumption_trillion")
    private BigDecimal monthlyTokenConsumptionTrillion;

    @TableField("token_unit_price")
    private BigDecimal tokenUnitPrice;

    @TableField("deploy_tech_fee")
    private BigDecimal deployTechFee;

    @TableField("status")
    private Integer status;

    @TableField("sort_no")
    private Integer sortNo;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

}

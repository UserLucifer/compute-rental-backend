package com.compute.rental.modules.product.entity;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("product")
public class Product {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("product_code")
    private String productCode;

    @TableField("product_name")
    private String productName;

    @TableField("machine_code")
    private String machineCode;

    @TableField("machine_alias")
    private String machineAlias;

    @TableField("region_id")
    private Long regionId;

    @TableField("gpu_model_id")
    private Long gpuModelId;

    @TableField("gpu_memory_gb")
    private Integer gpuMemoryGb;

    @TableField("gpu_power_tops")
    private BigDecimal gpuPowerTops;

    @TableField("rent_price")
    private BigDecimal rentPrice;

    @TableField("token_output_per_minute")
    private Long tokenOutputPerMinute;

    @TableField("token_output_per_day")
    private Long tokenOutputPerDay;

    @TableField("rentable_until")
    private LocalDate rentableUntil;

    @TableField("total_stock")
    private Integer totalStock;

    @TableField("available_stock")
    private Integer availableStock;

    @TableField("rented_stock")
    private Integer rentedStock;

    @TableField("cpu_model")
    private String cpuModel;

    @TableField("cpu_cores")
    private Integer cpuCores;

    @TableField("memory_gb")
    private Integer memoryGb;

    @TableField("system_disk_gb")
    private Integer systemDiskGb;

    @TableField("data_disk_gb")
    private Integer dataDiskGb;

    @TableField("max_expand_disk_gb")
    private Integer maxExpandDiskGb;

    @TableField("driver_version")
    private String driverVersion;

    @TableField("cuda_version")
    private String cudaVersion;

    @TableField("has_cache_optimization")
    private Integer hasCacheOptimization;

    @TableField("status")
    private Integer status;

    @TableField("sort_no")
    private Integer sortNo;

    @Version
    @TableField("version_no")
    private Integer versionNo;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

}

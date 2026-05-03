package com.compute.rental.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AdminProductRequest(
        @Schema(description = "Product code")
        String productCode,
        @Schema(description = "Product name")
        String productName,
        @Schema(description = "Machine code")
        String machineCode,
        @Schema(description = "Machine alias")
        String machineAlias,
        @Schema(description = "Region internal ID")
        Long regionId,
        @Schema(description = "GPU model internal ID")
        Long gpuModelId,
        @Schema(description = "GPU memory in GB")
        Integer gpuMemoryGb,
        @Schema(description = "GPU power in TOPS")
        BigDecimal gpuPowerTops,
        @Schema(description = "Rent price")
        BigDecimal rentPrice,
        @Schema(description = "Token output per minute")
        Long tokenOutputPerMinute,
        @Schema(description = "Token output per day")
        Long tokenOutputPerDay,
        @Schema(description = "Rentable until date")
        LocalDate rentableUntil,
        @Schema(description = "Total stock")
        Integer totalStock,
        @Schema(description = "Available stock")
        Integer availableStock,
        @Schema(description = "Rented stock")
        Integer rentedStock,
        @Schema(description = "CPU model")
        String cpuModel,
        @Schema(description = "CPU cores")
        Integer cpuCores,
        @Schema(description = "Memory in GB")
        Integer memoryGb,
        @Schema(description = "System disk in GB")
        Integer systemDiskGb,
        @Schema(description = "Data disk in GB")
        Integer dataDiskGb,
        @Schema(description = "Maximum expandable disk in GB")
        Integer maxExpandDiskGb,
        @Schema(description = "Driver version")
        String driverVersion,
        @Schema(description = "CUDA version")
        String cudaVersion,
        @Schema(description = "Whether cache optimization is enabled")
        Integer hasCacheOptimization,
        @Schema(description = "Status")
        Integer status,
        @Schema(description = "Sort number")
        Integer sortNo,
        @Schema(description = "Version number for product optimistic locking")
        Integer versionNo
) {
}

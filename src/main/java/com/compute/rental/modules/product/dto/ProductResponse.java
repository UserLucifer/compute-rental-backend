package com.compute.rental.modules.product.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductResponse(
        Long id,
        String productCode,
        String productName,
        String machineCode,
        String machineAlias,
        String regionName,
        String gpuModelName,
        Integer gpuMemoryGb,
        BigDecimal gpuPowerTops,
        BigDecimal rentPrice,
        Long tokenOutputPerMinute,
        Long tokenOutputPerDay,
        LocalDate rentableUntil,
        Integer totalStock,
        Integer availableStock,
        Integer rentedStock,
        String cpuModel,
        Integer cpuCores,
        Integer memoryGb,
        Integer systemDiskGb,
        Integer dataDiskGb,
        Integer maxExpandDiskGb,
        String driverVersion,
        String cudaVersion,
        Integer hasCacheOptimization
) {
}

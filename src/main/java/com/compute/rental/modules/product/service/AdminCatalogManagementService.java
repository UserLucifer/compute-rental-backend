package com.compute.rental.modules.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.compute.rental.common.enums.CommonStatus;
import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.common.util.DateTimeUtils;
import com.compute.rental.modules.product.dto.AdminAiModelRequest;
import com.compute.rental.modules.product.dto.AdminAiModelResponse;
import com.compute.rental.modules.product.dto.AdminGpuModelRequest;
import com.compute.rental.modules.product.dto.AdminGpuModelResponse;
import com.compute.rental.modules.product.dto.AdminProductRequest;
import com.compute.rental.modules.product.dto.AdminProductResponse;
import com.compute.rental.modules.product.dto.AdminRegionRequest;
import com.compute.rental.modules.product.dto.AdminRegionResponse;
import com.compute.rental.modules.product.dto.AdminRentalCycleRuleRequest;
import com.compute.rental.modules.product.dto.AdminRentalCycleRuleResponse;
import com.compute.rental.modules.product.entity.AiModel;
import com.compute.rental.modules.product.entity.GpuModel;
import com.compute.rental.modules.product.entity.Product;
import com.compute.rental.modules.product.entity.Region;
import com.compute.rental.modules.product.entity.RentalCycleRule;
import com.compute.rental.modules.product.mapper.AiModelMapper;
import com.compute.rental.modules.product.mapper.GpuModelMapper;
import com.compute.rental.modules.product.mapper.ProductMapper;
import com.compute.rental.modules.product.mapper.RegionMapper;
import com.compute.rental.modules.product.mapper.RentalCycleRuleMapper;
import com.compute.rental.modules.system.service.AdminLogService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
public class AdminCatalogManagementService {

    private final RegionMapper regionMapper;
    private final GpuModelMapper gpuModelMapper;
    private final ProductMapper productMapper;
    private final AiModelMapper aiModelMapper;
    private final RentalCycleRuleMapper rentalCycleRuleMapper;
    private final AdminLogService adminLogService;
    private final ProductCatalogCacheService productCatalogCacheService;

    public AdminCatalogManagementService(
            RegionMapper regionMapper,
            GpuModelMapper gpuModelMapper,
            ProductMapper productMapper,
            AiModelMapper aiModelMapper,
            RentalCycleRuleMapper rentalCycleRuleMapper,
            AdminLogService adminLogService,
            ProductCatalogCacheService productCatalogCacheService
    ) {
        this.regionMapper = regionMapper;
        this.gpuModelMapper = gpuModelMapper;
        this.productMapper = productMapper;
        this.aiModelMapper = aiModelMapper;
        this.rentalCycleRuleMapper = rentalCycleRuleMapper;
        this.adminLogService = adminLogService;
        this.productCatalogCacheService = productCatalogCacheService;
    }

    public PageResult<AdminRegionResponse> pageRegions(long pageNo, long pageSize, String regionCode, Integer status) {
        var page = new Page<Region>(pageNo, pageSize);
        var wrapper = new LambdaQueryWrapper<Region>()
                .eq(hasText(regionCode), Region::getRegionCode, regionCode)
                .eq(status != null, Region::getStatus, status)
                .orderByAsc(Region::getSortNo)
                .orderByDesc(Region::getId);
        var result = regionMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getRecords().stream().map(this::toAdminRegionResponse).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public AdminRegionResponse createRegion(AdminRegionRequest request, Long adminId, String ip) {
        var region = toRegion(request);
        var now = DateTimeUtils.now();
        region.setId(null);
        region.setStatus(defaultStatus(region.getStatus()));
        region.setCreatedAt(now);
        region.setUpdatedAt(now);
        regionMapper.insert(region);
        log(adminId, "CREATE_REGION", "region", region.getId(), region.getRegionCode(), ip);
        evictCatalogCacheAfterCommit();
        return toAdminRegionResponse(region);
    }

    @Transactional
    public AdminRegionResponse updateRegion(Long id, AdminRegionRequest request, Long adminId, String ip) {
        requireRegion(id);
        var region = toRegion(request);
        region.setId(id);
        region.setUpdatedAt(DateTimeUtils.now());
        regionMapper.updateById(region);
        log(adminId, "UPDATE_REGION", "region", id, region.getRegionCode(), ip);
        evictCatalogCacheAfterCommit();
        return toAdminRegionResponse(requireRegion(id));
    }

    @Transactional
    public AdminRegionResponse setRegionStatus(Long id, Integer status, Long adminId, String ip) {
        requireRegion(id);
        regionMapper.update(null, new LambdaUpdateWrapper<Region>()
                .eq(Region::getId, id)
                .set(Region::getStatus, status)
                .set(Region::getUpdatedAt, DateTimeUtils.now()));
        log(adminId, statusAction("REGION", status), "region", id, "status=" + status, ip);
        evictCatalogCacheAfterCommit();
        return toAdminRegionResponse(requireRegion(id));
    }

    public PageResult<AdminGpuModelResponse> pageGpuModels(long pageNo, long pageSize, String modelCode, Integer status) {
        var page = new Page<GpuModel>(pageNo, pageSize);
        var wrapper = new LambdaQueryWrapper<GpuModel>()
                .eq(hasText(modelCode), GpuModel::getModelCode, modelCode)
                .eq(status != null, GpuModel::getStatus, status)
                .orderByAsc(GpuModel::getSortNo)
                .orderByDesc(GpuModel::getId);
        var result = gpuModelMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getRecords().stream().map(this::toAdminGpuModelResponse).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public AdminGpuModelResponse createGpuModel(AdminGpuModelRequest request, Long adminId, String ip) {
        var gpuModel = toGpuModel(request);
        var now = DateTimeUtils.now();
        gpuModel.setId(null);
        gpuModel.setStatus(defaultStatus(gpuModel.getStatus()));
        gpuModel.setCreatedAt(now);
        gpuModel.setUpdatedAt(now);
        gpuModelMapper.insert(gpuModel);
        log(adminId, "CREATE_GPU_MODEL", "gpu_model", gpuModel.getId(), gpuModel.getModelCode(), ip);
        evictCatalogCacheAfterCommit();
        return toAdminGpuModelResponse(gpuModel);
    }

    @Transactional
    public AdminGpuModelResponse updateGpuModel(Long id, AdminGpuModelRequest request, Long adminId, String ip) {
        requireGpuModel(id);
        var gpuModel = toGpuModel(request);
        gpuModel.setId(id);
        gpuModel.setUpdatedAt(DateTimeUtils.now());
        gpuModelMapper.updateById(gpuModel);
        log(adminId, "UPDATE_GPU_MODEL", "gpu_model", id, gpuModel.getModelCode(), ip);
        evictCatalogCacheAfterCommit();
        return toAdminGpuModelResponse(requireGpuModel(id));
    }

    @Transactional
    public AdminGpuModelResponse setGpuModelStatus(Long id, Integer status, Long adminId, String ip) {
        requireGpuModel(id);
        gpuModelMapper.update(null, new LambdaUpdateWrapper<GpuModel>()
                .eq(GpuModel::getId, id)
                .set(GpuModel::getStatus, status)
                .set(GpuModel::getUpdatedAt, DateTimeUtils.now()));
        log(adminId, statusAction("GPU_MODEL", status), "gpu_model", id, "status=" + status, ip);
        evictCatalogCacheAfterCommit();
        return toAdminGpuModelResponse(requireGpuModel(id));
    }

    public PageResult<AdminProductResponse> pageProducts(long pageNo, long pageSize, String productCode,
                                                         Long regionId, Long gpuModelId, Integer status) {
        var page = new Page<Product>(pageNo, pageSize);
        var wrapper = new LambdaQueryWrapper<Product>()
                .eq(hasText(productCode), Product::getProductCode, productCode)
                .eq(regionId != null, Product::getRegionId, regionId)
                .eq(gpuModelId != null, Product::getGpuModelId, gpuModelId)
                .eq(status != null, Product::getStatus, status)
                .orderByAsc(Product::getSortNo)
                .orderByDesc(Product::getId);
        var result = productMapper.selectPage(page, wrapper);
        var products = result.getRecords();
        var regionMap = regionMap(products);
        var gpuModelMap = gpuModelMap(products);
        var records = products.stream()
                .map(product -> toAdminProductResponse(product, regionMap, gpuModelMap))
                .toList();
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    public AdminProductResponse getProduct(String productCode) {
        var product = requireProduct(productCode);
        return toAdminProductResponse(product,
                product.getRegionId() == null ? Collections.emptyMap()
                        : Map.of(product.getRegionId(), requireRegion(product.getRegionId())),
                product.getGpuModelId() == null ? Collections.emptyMap()
                        : Map.of(product.getGpuModelId(), requireGpuModel(product.getGpuModelId())));
    }

    @Transactional
    public AdminProductResponse createProduct(AdminProductRequest request, Long adminId, String ip) {
        var product = toProduct(request);
        var now = DateTimeUtils.now();
        product.setId(null);
        product.setStatus(defaultStatus(product.getStatus()));
        product.setVersionNo(product.getVersionNo() == null ? 0 : product.getVersionNo());
        product.setCreatedAt(now);
        product.setUpdatedAt(now);
        productMapper.insert(product);
        log(adminId, "CREATE_PRODUCT", "product", product.getId(), product.getProductCode(), ip);
        evictCatalogCacheAfterCommit();
        return getProduct(product.getProductCode());
    }

    @Transactional
    public AdminProductResponse updateProduct(String productCode, AdminProductRequest request, Long adminId, String ip) {
        var existing = requireProduct(productCode);
        var product = toProduct(request);
        product.setId(existing.getId());
        product.setProductCode(productCode);
        product.setUpdatedAt(DateTimeUtils.now());
        productMapper.updateById(product);
        log(adminId, "UPDATE_PRODUCT", "product", existing.getId(), productCode, ip);
        evictCatalogCacheAfterCommit();
        return getProduct(productCode);
    }

    @Transactional
    public AdminProductResponse setProductStatus(String productCode, Integer status, Long adminId, String ip) {
        var product = requireProduct(productCode);
        productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, product.getId())
                .set(Product::getStatus, status)
                .set(Product::getUpdatedAt, DateTimeUtils.now()));
        log(adminId, statusAction("PRODUCT", status), "product", product.getId(), "status=" + status, ip);
        evictCatalogCacheAfterCommit();
        return getProduct(productCode);
    }

    public PageResult<AdminAiModelResponse> pageAiModels(long pageNo, long pageSize, String modelCode, Integer status) {
        var page = new Page<AiModel>(pageNo, pageSize);
        var wrapper = new LambdaQueryWrapper<AiModel>()
                .eq(hasText(modelCode), AiModel::getModelCode, modelCode)
                .eq(status != null, AiModel::getStatus, status)
                .orderByAsc(AiModel::getSortNo)
                .orderByDesc(AiModel::getId);
        var result = aiModelMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getRecords().stream().map(this::toAdminAiModelResponse).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public AdminAiModelResponse createAiModel(AdminAiModelRequest request, Long adminId, String ip) {
        var aiModel = toAiModel(request);
        var now = DateTimeUtils.now();
        aiModel.setId(null);
        aiModel.setStatus(defaultStatus(aiModel.getStatus()));
        aiModel.setCreatedAt(now);
        aiModel.setUpdatedAt(now);
        aiModelMapper.insert(aiModel);
        log(adminId, "CREATE_AI_MODEL", "ai_model", aiModel.getId(), aiModel.getModelCode(), ip);
        evictCatalogCacheAfterCommit();
        return toAdminAiModelResponse(aiModel);
    }

    @Transactional
    public AdminAiModelResponse updateAiModel(String modelCode, AdminAiModelRequest request, Long adminId, String ip) {
        var existing = requireAiModel(modelCode);
        var aiModel = toAiModel(request);
        aiModel.setId(existing.getId());
        aiModel.setModelCode(modelCode);
        aiModel.setUpdatedAt(DateTimeUtils.now());
        aiModelMapper.updateById(aiModel);
        log(adminId, "UPDATE_AI_MODEL", "ai_model", existing.getId(), modelCode, ip);
        evictCatalogCacheAfterCommit();
        return toAdminAiModelResponse(requireAiModel(modelCode));
    }

    @Transactional
    public AdminAiModelResponse setAiModelStatus(String modelCode, Integer status, Long adminId, String ip) {
        var model = requireAiModel(modelCode);
        aiModelMapper.update(null, new LambdaUpdateWrapper<AiModel>()
                .eq(AiModel::getId, model.getId())
                .set(AiModel::getStatus, status)
                .set(AiModel::getUpdatedAt, DateTimeUtils.now()));
        log(adminId, statusAction("AI_MODEL", status), "ai_model", model.getId(), "status=" + status, ip);
        evictCatalogCacheAfterCommit();
        return toAdminAiModelResponse(requireAiModel(modelCode));
    }

    public PageResult<AdminRentalCycleRuleResponse> pageCycleRules(long pageNo, long pageSize, String cycleCode, Integer status) {
        var page = new Page<RentalCycleRule>(pageNo, pageSize);
        var wrapper = new LambdaQueryWrapper<RentalCycleRule>()
                .eq(hasText(cycleCode), RentalCycleRule::getCycleCode, cycleCode)
                .eq(status != null, RentalCycleRule::getStatus, status)
                .orderByAsc(RentalCycleRule::getSortNo)
                .orderByDesc(RentalCycleRule::getId);
        var result = rentalCycleRuleMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getRecords().stream().map(this::toAdminRentalCycleRuleResponse).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public AdminRentalCycleRuleResponse createCycleRule(AdminRentalCycleRuleRequest request, Long adminId, String ip) {
        var rule = toRentalCycleRule(request);
        var now = DateTimeUtils.now();
        rule.setId(null);
        rule.setStatus(defaultStatus(rule.getStatus()));
        rule.setCreatedAt(now);
        rule.setUpdatedAt(now);
        rentalCycleRuleMapper.insert(rule);
        log(adminId, "CREATE_RENTAL_CYCLE_RULE", "rental_cycle_rule", rule.getId(), rule.getCycleCode(), ip);
        evictCatalogCacheAfterCommit();
        return toAdminRentalCycleRuleResponse(rule);
    }

    @Transactional
    public AdminRentalCycleRuleResponse updateCycleRule(String cycleCode, AdminRentalCycleRuleRequest request, Long adminId, String ip) {
        var existing = requireCycleRule(cycleCode);
        var rule = toRentalCycleRule(request);
        rule.setId(existing.getId());
        rule.setCycleCode(cycleCode);
        rule.setUpdatedAt(DateTimeUtils.now());
        rentalCycleRuleMapper.updateById(rule);
        log(adminId, "UPDATE_RENTAL_CYCLE_RULE", "rental_cycle_rule", existing.getId(), cycleCode, ip);
        evictCatalogCacheAfterCommit();
        return toAdminRentalCycleRuleResponse(requireCycleRule(cycleCode));
    }

    @Transactional
    public AdminRentalCycleRuleResponse setCycleRuleStatus(String cycleCode, Integer status, Long adminId, String ip) {
        var rule = requireCycleRule(cycleCode);
        rentalCycleRuleMapper.update(null, new LambdaUpdateWrapper<RentalCycleRule>()
                .eq(RentalCycleRule::getId, rule.getId())
                .set(RentalCycleRule::getStatus, status)
                .set(RentalCycleRule::getUpdatedAt, DateTimeUtils.now()));
        log(adminId, statusAction("RENTAL_CYCLE_RULE", status), "rental_cycle_rule", rule.getId(), "status=" + status, ip);
        evictCatalogCacheAfterCommit();
        return toAdminRentalCycleRuleResponse(requireCycleRule(cycleCode));
    }

    private Region requireRegion(Long id) {
        var region = regionMapper.selectById(id);
        if (region == null) {
            throw new BusinessException(ErrorCode.REGION_NOT_FOUND);
        }
        return region;
    }

    private GpuModel requireGpuModel(Long id) {
        var gpuModel = gpuModelMapper.selectById(id);
        if (gpuModel == null) {
            throw new BusinessException(ErrorCode.GPU_MODEL_NOT_FOUND);
        }
        return gpuModel;
    }

    private Product requireProduct(String productCode) {
        var product = productMapper.selectOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getProductCode, productCode)
                .last("LIMIT 1"));
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return product;
    }

    private AiModel requireAiModel(String modelCode) {
        var model = aiModelMapper.selectOne(new LambdaQueryWrapper<AiModel>()
                .eq(AiModel::getModelCode, modelCode)
                .last("LIMIT 1"));
        if (model == null) {
            throw new BusinessException(ErrorCode.AI_MODEL_NOT_FOUND);
        }
        return model;
    }

    private RentalCycleRule requireCycleRule(String cycleCode) {
        var rule = rentalCycleRuleMapper.selectOne(new LambdaQueryWrapper<RentalCycleRule>()
                .eq(RentalCycleRule::getCycleCode, cycleCode)
                .last("LIMIT 1"));
        if (rule == null) {
            throw new BusinessException(ErrorCode.RENTAL_CYCLE_RULE_NOT_FOUND);
        }
        return rule;
    }

    private Map<Long, Region> regionMap(List<Product> products) {
        var ids = products.stream()
                .map(Product::getRegionId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return regionMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Region::getId, Function.identity()));
    }

    private Map<Long, GpuModel> gpuModelMap(List<Product> products) {
        var ids = products.stream()
                .map(Product::getGpuModelId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return gpuModelMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(GpuModel::getId, Function.identity()));
    }

    private Region toRegion(AdminRegionRequest request) {
        var region = new Region();
        region.setRegionCode(request.regionCode());
        region.setRegionName(request.regionName());
        region.setSortNo(request.sortNo());
        region.setStatus(request.status());
        return region;
    }

    private GpuModel toGpuModel(AdminGpuModelRequest request) {
        var gpuModel = new GpuModel();
        gpuModel.setModelCode(request.modelCode());
        gpuModel.setModelName(request.modelName());
        gpuModel.setSortNo(request.sortNo());
        gpuModel.setStatus(request.status());
        return gpuModel;
    }

    private Product toProduct(AdminProductRequest request) {
        var product = new Product();
        product.setProductCode(request.productCode());
        product.setProductName(request.productName());
        product.setMachineCode(request.machineCode());
        product.setMachineAlias(request.machineAlias());
        product.setRegionId(request.regionId());
        product.setGpuModelId(request.gpuModelId());
        product.setGpuMemoryGb(request.gpuMemoryGb());
        product.setGpuPowerTops(request.gpuPowerTops());
        product.setRentPrice(request.rentPrice());
        product.setTokenOutputPerMinute(request.tokenOutputPerMinute());
        product.setTokenOutputPerDay(request.tokenOutputPerDay());
        product.setRentableUntil(request.rentableUntil());
        product.setTotalStock(request.totalStock());
        product.setAvailableStock(request.availableStock());
        product.setRentedStock(request.rentedStock());
        product.setCpuModel(request.cpuModel());
        product.setCpuCores(request.cpuCores());
        product.setMemoryGb(request.memoryGb());
        product.setSystemDiskGb(request.systemDiskGb());
        product.setDataDiskGb(request.dataDiskGb());
        product.setMaxExpandDiskGb(request.maxExpandDiskGb());
        product.setDriverVersion(request.driverVersion());
        product.setCudaVersion(request.cudaVersion());
        product.setHasCacheOptimization(request.hasCacheOptimization());
        product.setStatus(request.status());
        product.setSortNo(request.sortNo());
        product.setVersionNo(request.versionNo());
        return product;
    }

    private AiModel toAiModel(AdminAiModelRequest request) {
        var aiModel = new AiModel();
        aiModel.setModelCode(request.modelCode());
        aiModel.setModelName(request.modelName());
        aiModel.setVendorName(request.vendorName());
        aiModel.setLogoUrl(request.logoUrl());
        aiModel.setMonthlyTokenConsumptionTrillion(request.monthlyTokenConsumptionTrillion());
        aiModel.setTokenUnitPrice(request.tokenUnitPrice());
        aiModel.setDeployTechFee(request.deployTechFee());
        aiModel.setStatus(request.status());
        aiModel.setSortNo(request.sortNo());
        return aiModel;
    }

    private RentalCycleRule toRentalCycleRule(AdminRentalCycleRuleRequest request) {
        var rule = new RentalCycleRule();
        rule.setCycleCode(request.cycleCode());
        rule.setCycleName(request.cycleName());
        rule.setCycleDays(request.cycleDays());
        rule.setYieldMultiplier(request.yieldMultiplier());
        rule.setEarlyPenaltyRate(request.earlyPenaltyRate());
        rule.setStatus(request.status());
        rule.setSortNo(request.sortNo());
        return rule;
    }

    private AdminProductResponse toAdminProductResponse(Product product, Map<Long, Region> regionMap,
                                                       Map<Long, GpuModel> gpuModelMap) {
        var region = regionMap.get(product.getRegionId());
        var gpuModel = gpuModelMap.get(product.getGpuModelId());
        return new AdminProductResponse(
                product.getId(),
                product.getProductCode(),
                product.getProductName(),
                product.getMachineCode(),
                product.getMachineAlias(),
                product.getRegionId(),
                region == null ? null : region.getRegionName(),
                product.getGpuModelId(),
                gpuModel == null ? null : gpuModel.getModelName(),
                product.getGpuMemoryGb(),
                product.getGpuPowerTops(),
                product.getRentPrice(),
                product.getTokenOutputPerMinute(),
                product.getTokenOutputPerDay(),
                product.getRentableUntil(),
                product.getTotalStock(),
                product.getAvailableStock(),
                product.getRentedStock(),
                product.getCpuModel(),
                product.getCpuCores(),
                product.getMemoryGb(),
                product.getSystemDiskGb(),
                product.getDataDiskGb(),
                product.getMaxExpandDiskGb(),
                product.getDriverVersion(),
                product.getCudaVersion(),
                product.getHasCacheOptimization(),
                product.getStatus(),
                product.getSortNo(),
                product.getVersionNo(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    private AdminRegionResponse toAdminRegionResponse(Region region) {
        return new AdminRegionResponse(
                region.getId(),
                region.getRegionCode(),
                region.getRegionName(),
                region.getSortNo(),
                region.getStatus(),
                region.getCreatedAt(),
                region.getUpdatedAt()
        );
    }

    private AdminGpuModelResponse toAdminGpuModelResponse(GpuModel gpuModel) {
        return new AdminGpuModelResponse(
                gpuModel.getId(),
                gpuModel.getModelCode(),
                gpuModel.getModelName(),
                gpuModel.getSortNo(),
                gpuModel.getStatus(),
                gpuModel.getCreatedAt(),
                gpuModel.getUpdatedAt()
        );
    }

    private AdminAiModelResponse toAdminAiModelResponse(AiModel aiModel) {
        return new AdminAiModelResponse(
                aiModel.getId(),
                aiModel.getModelCode(),
                aiModel.getModelName(),
                aiModel.getVendorName(),
                aiModel.getLogoUrl(),
                aiModel.getMonthlyTokenConsumptionTrillion(),
                aiModel.getTokenUnitPrice(),
                aiModel.getDeployTechFee(),
                aiModel.getStatus(),
                aiModel.getSortNo(),
                aiModel.getCreatedAt(),
                aiModel.getUpdatedAt()
        );
    }

    private AdminRentalCycleRuleResponse toAdminRentalCycleRuleResponse(RentalCycleRule rule) {
        return new AdminRentalCycleRuleResponse(
                rule.getId(),
                rule.getCycleCode(),
                rule.getCycleName(),
                rule.getCycleDays(),
                rule.getYieldMultiplier(),
                rule.getEarlyPenaltyRate(),
                rule.getStatus(),
                rule.getSortNo(),
                rule.getCreatedAt(),
                rule.getUpdatedAt()
        );
    }

    private Integer defaultStatus(Integer status) {
        return status == null ? CommonStatus.ENABLED.value() : status;
    }

    private String statusAction(String subject, Integer status) {
        return Integer.valueOf(CommonStatus.ENABLED.value()).equals(status) ? "ENABLE_" + subject : "DISABLE_" + subject;
    }

    private void log(Long adminId, String action, String table, Long targetId, String remark, String ip) {
        adminLogService.log(adminId, action, table, targetId, null, null, remark, ip);
    }

    private void evictCatalogCacheAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            productCatalogCacheService.evictCatalog();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                productCatalogCacheService.evictCatalog();
            }
        });
    }

    private boolean hasText(String value) {
        return StringUtils.hasText(value);
    }
}

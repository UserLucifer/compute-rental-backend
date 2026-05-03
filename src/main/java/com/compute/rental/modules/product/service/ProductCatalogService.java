package com.compute.rental.modules.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.compute.rental.common.enums.CommonStatus;
import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.common.util.RedisKeys;
import com.compute.rental.modules.product.dto.AiModelResponse;
import com.compute.rental.modules.product.dto.GpuModelResponse;
import com.compute.rental.modules.product.dto.ProductQueryRequest;
import com.compute.rental.modules.product.dto.ProductResponse;
import com.compute.rental.modules.product.dto.RegionResponse;
import com.compute.rental.modules.product.dto.RentalCycleRuleResponse;
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
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ProductCatalogService {

    private static final TypeReference<List<RegionResponse>> REGION_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<GpuModelResponse>> GPU_MODEL_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<PageResult<ProductResponse>> PRODUCT_PAGE_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<ProductResponse> PRODUCT_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<AiModelResponse>> AI_MODEL_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<RentalCycleRuleResponse>> CYCLE_RULE_LIST_TYPE = new TypeReference<>() {
    };

    private final RegionMapper regionMapper;
    private final GpuModelMapper gpuModelMapper;
    private final ProductMapper productMapper;
    private final AiModelMapper aiModelMapper;
    private final RentalCycleRuleMapper rentalCycleRuleMapper;
    private final ProductCatalogCacheService productCatalogCacheService;

    public ProductCatalogService(
            RegionMapper regionMapper,
            GpuModelMapper gpuModelMapper,
            ProductMapper productMapper,
            AiModelMapper aiModelMapper,
            RentalCycleRuleMapper rentalCycleRuleMapper,
            ProductCatalogCacheService productCatalogCacheService
    ) {
        this.regionMapper = regionMapper;
        this.gpuModelMapper = gpuModelMapper;
        this.productMapper = productMapper;
        this.aiModelMapper = aiModelMapper;
        this.rentalCycleRuleMapper = rentalCycleRuleMapper;
        this.productCatalogCacheService = productCatalogCacheService;
    }

    public List<RegionResponse> listEnabledRegions() {
        var key = RedisKeys.catalogRegions();
        var cached = productCatalogCacheService.get(key, REGION_LIST_TYPE);
        if (cached != null) {
            return cached;
        }
        var result = regionMapper.selectList(new LambdaQueryWrapper<Region>()
                        .eq(Region::getStatus, CommonStatus.ENABLED.value())
                        .orderByAsc(Region::getSortNo))
                .stream()
                .map(region -> new RegionResponse(region.getId(), region.getRegionCode(), region.getRegionName()))
                .toList();
        productCatalogCacheService.put(key, result);
        return result;
    }

    public List<GpuModelResponse> listEnabledGpuModels(Long regionId) {
        var key = RedisKeys.catalogGpuModels(regionId);
        var cached = productCatalogCacheService.get(key, GPU_MODEL_LIST_TYPE);
        if (cached != null) {
            return cached;
        }
        var models = regionId == null
                ? gpuModelMapper.selectList(new LambdaQueryWrapper<GpuModel>()
                        .eq(GpuModel::getStatus, CommonStatus.ENABLED.value())
                        .orderByAsc(GpuModel::getSortNo))
                : gpuModelMapper.selectEnabledByRegionId(regionId);
        var result = models
                .stream()
                .map(model -> new GpuModelResponse(model.getId(), model.getModelCode(), model.getModelName()))
                .toList();
        productCatalogCacheService.put(key, result);
        return result;
    }

    public PageResult<ProductResponse> pageEnabledProducts(ProductQueryRequest request) {
        var key = RedisKeys.catalogProductPage(request.current(), request.size(), request.regionId(), request.gpuModelId());
        var cached = productCatalogCacheService.get(key, PRODUCT_PAGE_TYPE);
        if (cached != null) {
            return cached;
        }
        var page = new Page<Product>(request.current(), request.size());
        var wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, CommonStatus.ENABLED.value())
                .eq(request.regionId() != null, Product::getRegionId, request.regionId())
                .eq(request.gpuModelId() != null, Product::getGpuModelId, request.gpuModelId())
                .orderByAsc(Product::getSortNo);
        var result = productMapper.selectPage(page, wrapper);
        var products = result.getRecords();
        var regionMap = regionMap(products);
        var gpuModelMap = gpuModelMap(products);
        var response = new PageResult<>(products.stream()
                .map(product -> toProductResponse(product, regionMap, gpuModelMap))
                .toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
        productCatalogCacheService.put(key, response);
        return response;
    }

    public ProductResponse getEnabledProduct(String productCode) {
        var key = RedisKeys.catalogProduct(productCode);
        var cached = productCatalogCacheService.get(key, PRODUCT_TYPE);
        if (cached != null) {
            return cached;
        }
        var product = productMapper.selectOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getProductCode, productCode)
                .eq(Product::getStatus, CommonStatus.ENABLED.value())
                .last("LIMIT 1"));
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        var response = toProductResponse(product);
        productCatalogCacheService.put(key, response);
        return response;
    }

    public List<AiModelResponse> listEnabledAiModels() {
        var key = RedisKeys.catalogAiModels();
        var cached = productCatalogCacheService.get(key, AI_MODEL_LIST_TYPE);
        if (cached != null) {
            return cached;
        }
        var result = aiModelMapper.selectList(new LambdaQueryWrapper<AiModel>()
                        .eq(AiModel::getStatus, CommonStatus.ENABLED.value())
                        .orderByAsc(AiModel::getSortNo))
                .stream()
                .map(model -> new AiModelResponse(
                        model.getId(),
                        model.getModelCode(),
                        model.getModelName(),
                        model.getVendorName(),
                        model.getLogoUrl(),
                        model.getMonthlyTokenConsumptionTrillion(),
                        model.getTokenUnitPrice(),
                        model.getDeployTechFee()
                ))
                .toList();
        productCatalogCacheService.put(key, result);
        return result;
    }

    public List<RentalCycleRuleResponse> listEnabledCycleRules() {
        var key = RedisKeys.catalogCycleRules();
        var cached = productCatalogCacheService.get(key, CYCLE_RULE_LIST_TYPE);
        if (cached != null) {
            return cached;
        }
        var result = rentalCycleRuleMapper.selectList(new LambdaQueryWrapper<RentalCycleRule>()
                        .eq(RentalCycleRule::getStatus, CommonStatus.ENABLED.value())
                        .orderByAsc(RentalCycleRule::getSortNo))
                .stream()
                .map(rule -> new RentalCycleRuleResponse(
                        rule.getId(),
                        rule.getCycleCode(),
                        rule.getCycleName(),
                        rule.getCycleDays(),
                        rule.getYieldMultiplier(),
                        rule.getEarlyPenaltyRate()
                ))
                .toList();
        productCatalogCacheService.put(key, result);
        return result;
    }

    private ProductResponse toProductResponse(Product product) {
        var regions = product.getRegionId() == null
                ? Collections.<Long, Region>emptyMap()
                : regionMap(List.of(product));
        var gpuModels = product.getGpuModelId() == null
                ? Collections.<Long, GpuModel>emptyMap()
                : gpuModelMap(List.of(product));
        return toProductResponse(product, regions, gpuModels);
    }

    private ProductResponse toProductResponse(Product product, Map<Long, Region> regionMap,
                                              Map<Long, GpuModel> gpuModelMap) {
        var region = regionMap.get(product.getRegionId());
        var gpuModel = gpuModelMap.get(product.getGpuModelId());
        return new ProductResponse(
                product.getId(),
                product.getProductCode(),
                product.getProductName(),
                product.getMachineCode(),
                product.getMachineAlias(),
                region == null ? null : region.getRegionName(),
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
                product.getHasCacheOptimization()
        );
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
}

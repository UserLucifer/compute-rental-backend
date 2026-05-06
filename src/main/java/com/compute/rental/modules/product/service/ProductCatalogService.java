package com.compute.rental.modules.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.compute.rental.common.enums.CommonStatus;
import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.common.i18n.LanguageResolver;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.common.util.RedisKeys;
import com.compute.rental.modules.product.dto.AiModelResponse;
import com.compute.rental.modules.product.dto.GpuModelResponse;
import com.compute.rental.modules.product.dto.ProductQueryRequest;
import com.compute.rental.modules.product.dto.ProductResponse;
import com.compute.rental.modules.product.dto.RegionResponse;
import com.compute.rental.modules.product.dto.RentalCycleRuleResponse;
import com.compute.rental.modules.product.entity.AiModel;
import com.compute.rental.modules.product.entity.AiModelTranslation;
import com.compute.rental.modules.product.entity.GpuModel;
import com.compute.rental.modules.product.entity.GpuModelTranslation;
import com.compute.rental.modules.product.entity.Product;
import com.compute.rental.modules.product.entity.ProductTranslation;
import com.compute.rental.modules.product.entity.Region;
import com.compute.rental.modules.product.entity.RegionTranslation;
import com.compute.rental.modules.product.entity.RentalCycleRule;
import com.compute.rental.modules.product.entity.RentalCycleRuleTranslation;
import com.compute.rental.modules.product.mapper.AiModelMapper;
import com.compute.rental.modules.product.mapper.AiModelTranslationMapper;
import com.compute.rental.modules.product.mapper.GpuModelMapper;
import com.compute.rental.modules.product.mapper.GpuModelTranslationMapper;
import com.compute.rental.modules.product.mapper.ProductMapper;
import com.compute.rental.modules.product.mapper.ProductTranslationMapper;
import com.compute.rental.modules.product.mapper.RegionMapper;
import com.compute.rental.modules.product.mapper.RegionTranslationMapper;
import com.compute.rental.modules.product.mapper.RentalCycleRuleMapper;
import com.compute.rental.modules.product.mapper.RentalCycleRuleTranslationMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
    private final RegionTranslationMapper regionTranslationMapper;
    private final GpuModelTranslationMapper gpuModelTranslationMapper;
    private final ProductTranslationMapper productTranslationMapper;
    private final AiModelTranslationMapper aiModelTranslationMapper;
    private final RentalCycleRuleTranslationMapper rentalCycleRuleTranslationMapper;
    private final ProductCatalogCacheService productCatalogCacheService;

    public ProductCatalogService(
            RegionMapper regionMapper,
            GpuModelMapper gpuModelMapper,
            ProductMapper productMapper,
            AiModelMapper aiModelMapper,
            RentalCycleRuleMapper rentalCycleRuleMapper,
            RegionTranslationMapper regionTranslationMapper,
            GpuModelTranslationMapper gpuModelTranslationMapper,
            ProductTranslationMapper productTranslationMapper,
            AiModelTranslationMapper aiModelTranslationMapper,
            RentalCycleRuleTranslationMapper rentalCycleRuleTranslationMapper,
            ProductCatalogCacheService productCatalogCacheService
    ) {
        this.regionMapper = regionMapper;
        this.gpuModelMapper = gpuModelMapper;
        this.productMapper = productMapper;
        this.aiModelMapper = aiModelMapper;
        this.rentalCycleRuleMapper = rentalCycleRuleMapper;
        this.regionTranslationMapper = regionTranslationMapper;
        this.gpuModelTranslationMapper = gpuModelTranslationMapper;
        this.productTranslationMapper = productTranslationMapper;
        this.aiModelTranslationMapper = aiModelTranslationMapper;
        this.rentalCycleRuleTranslationMapper = rentalCycleRuleTranslationMapper;
        this.productCatalogCacheService = productCatalogCacheService;
    }

    public List<RegionResponse> listEnabledRegions() {
        return listEnabledRegions(LanguageResolver.DEFAULT_LANGUAGE);
    }

    public List<RegionResponse> listEnabledRegions(String locale) {
        var key = RedisKeys.catalogRegions(locale);
        var cached = productCatalogCacheService.get(key, REGION_LIST_TYPE);
        if (cached != null) {
            return cached;
        }
        var regions = regionMapper.selectList(new LambdaQueryWrapper<Region>()
                .eq(Region::getStatus, CommonStatus.ENABLED.value())
                .orderByAsc(Region::getSortNo));
        var translations = regionTranslationMap(regions.stream().map(Region::getId).toList(), locale);
        var result = regions.stream()
                .map(region -> toRegionResponse(region, translations.get(region.getId()), locale))
                .toList();
        productCatalogCacheService.put(key, result);
        return result;
    }

    public List<GpuModelResponse> listEnabledGpuModels(Long regionId) {
        return listEnabledGpuModels(regionId, LanguageResolver.DEFAULT_LANGUAGE);
    }

    public List<GpuModelResponse> listEnabledGpuModels(Long regionId, String locale) {
        var key = RedisKeys.catalogGpuModels(regionId, locale);
        var cached = productCatalogCacheService.get(key, GPU_MODEL_LIST_TYPE);
        if (cached != null) {
            return cached;
        }
        var models = regionId == null
                ? gpuModelMapper.selectList(new LambdaQueryWrapper<GpuModel>()
                        .eq(GpuModel::getStatus, CommonStatus.ENABLED.value())
                        .orderByAsc(GpuModel::getSortNo))
                : gpuModelMapper.selectEnabledByRegionId(regionId);
        var translations = gpuModelTranslationMap(models.stream().map(GpuModel::getId).toList(), locale);
        var result = models.stream()
                .map(model -> toGpuModelResponse(model, translations.get(model.getId()), locale))
                .toList();
        productCatalogCacheService.put(key, result);
        return result;
    }

    public PageResult<ProductResponse> pageEnabledProducts(ProductQueryRequest request) {
        var locale = StringUtils.hasText(request.language()) ? request.language() : LanguageResolver.DEFAULT_LANGUAGE;
        return pageEnabledProducts(request, locale);
    }

    public PageResult<ProductResponse> pageEnabledProducts(ProductQueryRequest request, String locale) {
        var key = RedisKeys.catalogProductPage(request.current(), request.size(), request.regionId(), request.gpuModelId(), locale);
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
        var productTranslations = productTranslationMap(products.stream().map(Product::getId).toList(), locale);
        var regionTranslations = regionTranslationMap(regionMap.keySet(), locale);
        var gpuModelTranslations = gpuModelTranslationMap(gpuModelMap.keySet(), locale);
        var response = new PageResult<>(products.stream()
                .map(product -> toProductResponse(product, regionMap, gpuModelMap, productTranslations,
                        regionTranslations, gpuModelTranslations, locale))
                .toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
        productCatalogCacheService.put(key, response);
        return response;
    }

    public ProductResponse getEnabledProduct(String productCode) {
        return getEnabledProduct(productCode, LanguageResolver.DEFAULT_LANGUAGE);
    }

    public ProductResponse getEnabledProduct(String productCode, String locale) {
        var key = RedisKeys.catalogProduct(productCode, locale);
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
        var response = toProductResponse(product, locale);
        productCatalogCacheService.put(key, response);
        return response;
    }

    public List<AiModelResponse> listEnabledAiModels() {
        return listEnabledAiModels(LanguageResolver.DEFAULT_LANGUAGE);
    }

    public List<AiModelResponse> listEnabledAiModels(String locale) {
        var key = RedisKeys.catalogAiModels(locale);
        var cached = productCatalogCacheService.get(key, AI_MODEL_LIST_TYPE);
        if (cached != null) {
            return cached;
        }
        var models = aiModelMapper.selectList(new LambdaQueryWrapper<AiModel>()
                .eq(AiModel::getStatus, CommonStatus.ENABLED.value())
                .orderByAsc(AiModel::getSortNo));
        var translations = aiModelTranslationMap(models.stream().map(AiModel::getId).toList(), locale);
        var result = models.stream()
                .map(model -> toAiModelResponse(model, translations.get(model.getId()), locale))
                .toList();
        productCatalogCacheService.put(key, result);
        return result;
    }

    public List<RentalCycleRuleResponse> listEnabledCycleRules() {
        return listEnabledCycleRules(LanguageResolver.DEFAULT_LANGUAGE);
    }

    public List<RentalCycleRuleResponse> listEnabledCycleRules(String locale) {
        var key = RedisKeys.catalogCycleRules(locale);
        var cached = productCatalogCacheService.get(key, CYCLE_RULE_LIST_TYPE);
        if (cached != null) {
            return cached;
        }
        var rules = rentalCycleRuleMapper.selectList(new LambdaQueryWrapper<RentalCycleRule>()
                .eq(RentalCycleRule::getStatus, CommonStatus.ENABLED.value())
                .orderByAsc(RentalCycleRule::getSortNo));
        var translations = rentalCycleRuleTranslationMap(rules.stream().map(RentalCycleRule::getId).toList(), locale);
        var result = rules.stream()
                .map(rule -> toCycleRuleResponse(rule, translations.get(rule.getId()), locale))
                .toList();
        productCatalogCacheService.put(key, result);
        return result;
    }

    private ProductResponse toProductResponse(Product product, String locale) {
        var regions = product.getRegionId() == null
                ? Collections.<Long, Region>emptyMap()
                : regionMap(List.of(product));
        var gpuModels = product.getGpuModelId() == null
                ? Collections.<Long, GpuModel>emptyMap()
                : gpuModelMap(List.of(product));
        return toProductResponse(product, regions, gpuModels,
                productTranslationMap(List.of(product.getId()), locale),
                regionTranslationMap(regions.keySet(), locale),
                gpuModelTranslationMap(gpuModels.keySet(), locale),
                locale);
    }

    private ProductResponse toProductResponse(Product product, Map<Long, Region> regionMap,
                                              Map<Long, GpuModel> gpuModelMap,
                                              Map<Long, ProductTranslation> productTranslations,
                                              Map<Long, RegionTranslation> regionTranslations,
                                              Map<Long, GpuModelTranslation> gpuModelTranslations,
                                              String requestedLocale) {
        var region = regionMap.get(product.getRegionId());
        var gpuModel = gpuModelMap.get(product.getGpuModelId());
        var productName = localized(product.getProductName(), requestedLocale,
                productTranslationName(productTranslations.get(product.getId())));
        var regionName = region == null
                ? LocalizedText.empty(requestedLocale)
                : localized(region.getRegionName(), requestedLocale,
                        regionTranslationName(regionTranslations.get(region.getId())));
        var gpuModelName = gpuModel == null
                ? LocalizedText.empty(requestedLocale)
                : localized(gpuModel.getModelName(), requestedLocale,
                        gpuModelTranslationName(gpuModelTranslations.get(gpuModel.getId())));
        var localeFallback = productName.fallback() || regionName.fallback() || gpuModelName.fallback();
        return new ProductResponse(
                product.getId(),
                product.getProductCode(),
                productName.value(),
                product.getMachineCode(),
                product.getMachineAlias(),
                regionName.value(),
                gpuModelName.value(),
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
                localeFallback ? LanguageResolver.DEFAULT_LANGUAGE : requestedLocale,
                requestedLocale,
                localeFallback
        );
    }

    private RegionResponse toRegionResponse(Region region, RegionTranslation translation, String requestedLocale) {
        var regionName = localized(region.getRegionName(), requestedLocale, regionTranslationName(translation));
        return new RegionResponse(region.getId(), region.getRegionCode(), regionName.value(),
                regionName.locale(), requestedLocale, regionName.fallback());
    }

    private GpuModelResponse toGpuModelResponse(GpuModel model, GpuModelTranslation translation, String requestedLocale) {
        var modelName = localized(model.getModelName(), requestedLocale, gpuModelTranslationName(translation));
        return new GpuModelResponse(model.getId(), model.getModelCode(), modelName.value(),
                modelName.locale(), requestedLocale, modelName.fallback());
    }

    private AiModelResponse toAiModelResponse(AiModel model, AiModelTranslation translation, String requestedLocale) {
        var modelName = localized(model.getModelName(), requestedLocale, aiModelTranslationName(translation));
        var vendorName = localized(model.getVendorName(), requestedLocale, aiModelTranslationVendor(translation));
        var localeFallback = modelName.fallback() || vendorName.fallback();
        return new AiModelResponse(
                model.getId(),
                model.getModelCode(),
                modelName.value(),
                vendorName.value(),
                model.getLogoUrl(),
                model.getMonthlyTokenConsumptionTrillion(),
                model.getTokenUnitPrice(),
                model.getDeployTechFee(),
                localeFallback ? LanguageResolver.DEFAULT_LANGUAGE : requestedLocale,
                requestedLocale,
                localeFallback
        );
    }

    private RentalCycleRuleResponse toCycleRuleResponse(RentalCycleRule rule, RentalCycleRuleTranslation translation,
                                                        String requestedLocale) {
        var cycleName = localized(rule.getCycleName(), requestedLocale, cycleRuleTranslationName(translation));
        return new RentalCycleRuleResponse(
                rule.getId(),
                rule.getCycleCode(),
                cycleName.value(),
                rule.getCycleDays(),
                rule.getYieldMultiplier(),
                rule.getEarlyPenaltyRate(),
                cycleName.locale(),
                requestedLocale,
                cycleName.fallback()
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

    private Map<Long, RegionTranslation> regionTranslationMap(Collection<Long> ids, String locale) {
        if (LanguageResolver.DEFAULT_LANGUAGE.equals(locale) || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return regionTranslationMapper.selectList(new LambdaQueryWrapper<RegionTranslation>()
                        .in(RegionTranslation::getRegionId, ids)
                        .eq(RegionTranslation::getLocale, locale))
                .stream()
                .collect(Collectors.toMap(RegionTranslation::getRegionId, Function.identity()));
    }

    private Map<Long, GpuModelTranslation> gpuModelTranslationMap(Collection<Long> ids, String locale) {
        if (LanguageResolver.DEFAULT_LANGUAGE.equals(locale) || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return gpuModelTranslationMapper.selectList(new LambdaQueryWrapper<GpuModelTranslation>()
                        .in(GpuModelTranslation::getGpuModelId, ids)
                        .eq(GpuModelTranslation::getLocale, locale))
                .stream()
                .collect(Collectors.toMap(GpuModelTranslation::getGpuModelId, Function.identity()));
    }

    private Map<Long, ProductTranslation> productTranslationMap(Collection<Long> ids, String locale) {
        if (LanguageResolver.DEFAULT_LANGUAGE.equals(locale) || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return productTranslationMapper.selectList(new LambdaQueryWrapper<ProductTranslation>()
                        .in(ProductTranslation::getProductId, ids)
                        .eq(ProductTranslation::getLocale, locale))
                .stream()
                .collect(Collectors.toMap(ProductTranslation::getProductId, Function.identity()));
    }

    private Map<Long, AiModelTranslation> aiModelTranslationMap(Collection<Long> ids, String locale) {
        if (LanguageResolver.DEFAULT_LANGUAGE.equals(locale) || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return aiModelTranslationMapper.selectList(new LambdaQueryWrapper<AiModelTranslation>()
                        .in(AiModelTranslation::getAiModelId, ids)
                        .eq(AiModelTranslation::getLocale, locale))
                .stream()
                .collect(Collectors.toMap(AiModelTranslation::getAiModelId, Function.identity()));
    }

    private Map<Long, RentalCycleRuleTranslation> rentalCycleRuleTranslationMap(Collection<Long> ids, String locale) {
        if (LanguageResolver.DEFAULT_LANGUAGE.equals(locale) || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return rentalCycleRuleTranslationMapper.selectList(new LambdaQueryWrapper<RentalCycleRuleTranslation>()
                        .in(RentalCycleRuleTranslation::getCycleRuleId, ids)
                        .eq(RentalCycleRuleTranslation::getLocale, locale))
                .stream()
                .collect(Collectors.toMap(RentalCycleRuleTranslation::getCycleRuleId, Function.identity()));
    }

    private LocalizedText localized(String defaultValue, String requestedLocale, String translatedValue) {
        if (LanguageResolver.DEFAULT_LANGUAGE.equals(requestedLocale)) {
            return new LocalizedText(defaultValue, requestedLocale, false);
        }
        if (StringUtils.hasText(translatedValue)) {
            return new LocalizedText(translatedValue, requestedLocale, false);
        }
        if (!StringUtils.hasText(defaultValue)) {
            return new LocalizedText(defaultValue, requestedLocale, false);
        }
        return new LocalizedText(defaultValue, LanguageResolver.DEFAULT_LANGUAGE, true);
    }

    private String regionTranslationName(RegionTranslation translation) {
        return translation == null ? null : translation.getRegionName();
    }

    private String gpuModelTranslationName(GpuModelTranslation translation) {
        return translation == null ? null : translation.getModelName();
    }

    private String productTranslationName(ProductTranslation translation) {
        return translation == null ? null : translation.getProductName();
    }

    private String aiModelTranslationName(AiModelTranslation translation) {
        return translation == null ? null : translation.getModelName();
    }

    private String aiModelTranslationVendor(AiModelTranslation translation) {
        return translation == null ? null : translation.getVendorName();
    }

    private String cycleRuleTranslationName(RentalCycleRuleTranslation translation) {
        return translation == null ? null : translation.getCycleName();
    }

    private record LocalizedText(String value, String locale, boolean fallback) {

        private static LocalizedText empty(String locale) {
            return new LocalizedText(null, locale, false);
        }
    }
}

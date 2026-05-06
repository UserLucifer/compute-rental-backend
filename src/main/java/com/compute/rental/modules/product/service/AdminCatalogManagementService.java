package com.compute.rental.modules.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.compute.rental.common.enums.CommonStatus;
import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.common.i18n.LanguageResolver;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.common.util.DateTimeUtils;
import com.compute.rental.modules.product.dto.AiModelTranslationRequest;
import com.compute.rental.modules.product.dto.AiModelTranslationResponse;
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
import com.compute.rental.modules.product.dto.GpuModelTranslationRequest;
import com.compute.rental.modules.product.dto.GpuModelTranslationResponse;
import com.compute.rental.modules.product.dto.ProductTranslationRequest;
import com.compute.rental.modules.product.dto.ProductTranslationResponse;
import com.compute.rental.modules.product.dto.RegionTranslationRequest;
import com.compute.rental.modules.product.dto.RegionTranslationResponse;
import com.compute.rental.modules.product.dto.RentalCycleRuleTranslationRequest;
import com.compute.rental.modules.product.dto.RentalCycleRuleTranslationResponse;
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
    private final RegionTranslationMapper regionTranslationMapper;
    private final GpuModelTranslationMapper gpuModelTranslationMapper;
    private final ProductTranslationMapper productTranslationMapper;
    private final AiModelTranslationMapper aiModelTranslationMapper;
    private final RentalCycleRuleTranslationMapper rentalCycleRuleTranslationMapper;
    private final AdminLogService adminLogService;
    private final ProductCatalogCacheService productCatalogCacheService;

    public AdminCatalogManagementService(
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
            AdminLogService adminLogService,
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

    public List<RegionTranslationResponse> listRegionTranslations(Long id) {
        var region = requireRegion(id);
        var english = regionTranslation(id, LanguageResolver.EN_US);
        return List.of(
                new RegionTranslationResponse(id, LanguageResolver.DEFAULT_LANGUAGE, region.getRegionName(), true,
                        region.getCreatedAt(), region.getUpdatedAt()),
                regionTranslationResponse(id, LanguageResolver.EN_US, english)
        );
    }

    @Transactional
    public RegionTranslationResponse updateRegionTranslation(
            Long id,
            RegionTranslationRequest request,
            Long adminId,
            String ip
    ) {
        var region = requireRegion(id);
        var locale = requireSupportedLocale(request.locale());
        var regionName = trimToNull(request.regionName());
        if (LanguageResolver.DEFAULT_LANGUAGE.equals(locale) && regionName != null) {
            var now = DateTimeUtils.now();
            regionMapper.update(null, new LambdaUpdateWrapper<Region>()
                    .eq(Region::getId, id)
                    .set(Region::getRegionName, regionName)
                    .set(Region::getUpdatedAt, now));
            region.setRegionName(regionName);
            region.setUpdatedAt(now);
        }
        var response = upsertRegionTranslation(id, locale, region.getRegionName(), regionName);
        log(adminId, "UPDATE_REGION_TRANSLATION", "region", id, response.locale(), ip);
        evictCatalogCacheAfterCommit();
        return response;
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

    public List<GpuModelTranslationResponse> listGpuModelTranslations(Long id) {
        var model = requireGpuModel(id);
        var english = gpuModelTranslation(id, LanguageResolver.EN_US);
        return List.of(
                new GpuModelTranslationResponse(id, LanguageResolver.DEFAULT_LANGUAGE, model.getModelName(), true,
                        model.getCreatedAt(), model.getUpdatedAt()),
                gpuModelTranslationResponse(id, LanguageResolver.EN_US, english)
        );
    }

    @Transactional
    public GpuModelTranslationResponse updateGpuModelTranslation(
            Long id,
            GpuModelTranslationRequest request,
            Long adminId,
            String ip
    ) {
        var model = requireGpuModel(id);
        var locale = requireSupportedLocale(request.locale());
        var modelName = trimToNull(request.modelName());
        if (LanguageResolver.DEFAULT_LANGUAGE.equals(locale) && modelName != null) {
            var now = DateTimeUtils.now();
            gpuModelMapper.update(null, new LambdaUpdateWrapper<GpuModel>()
                    .eq(GpuModel::getId, id)
                    .set(GpuModel::getModelName, modelName)
                    .set(GpuModel::getUpdatedAt, now));
            model.setModelName(modelName);
            model.setUpdatedAt(now);
        }
        var response = upsertGpuModelTranslation(id, locale, model.getModelName(), modelName);
        log(adminId, "UPDATE_GPU_MODEL_TRANSLATION", "gpu_model", id, response.locale(), ip);
        evictCatalogCacheAfterCommit();
        return response;
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

    public List<ProductTranslationResponse> listProductTranslations(String productCode) {
        var product = requireProduct(productCode);
        var english = productTranslation(product.getId(), LanguageResolver.EN_US);
        return List.of(
                new ProductTranslationResponse(product.getId(), productCode, LanguageResolver.DEFAULT_LANGUAGE,
                        product.getProductName(), true, product.getCreatedAt(), product.getUpdatedAt()),
                productTranslationResponse(product, LanguageResolver.EN_US, english)
        );
    }

    @Transactional
    public ProductTranslationResponse updateProductTranslation(
            String productCode,
            ProductTranslationRequest request,
            Long adminId,
            String ip
    ) {
        var product = requireProduct(productCode);
        var locale = requireSupportedLocale(request.locale());
        var productName = trimToNull(request.productName());
        if (LanguageResolver.DEFAULT_LANGUAGE.equals(locale) && productName != null) {
            var now = DateTimeUtils.now();
            productMapper.update(null, new LambdaUpdateWrapper<Product>()
                    .eq(Product::getId, product.getId())
                    .set(Product::getProductName, productName)
                    .set(Product::getUpdatedAt, now));
            product.setProductName(productName);
            product.setUpdatedAt(now);
        }
        var response = upsertProductTranslation(product, locale, product.getProductName(), productName);
        log(adminId, "UPDATE_PRODUCT_TRANSLATION", "product", product.getId(), response.locale(), ip);
        evictCatalogCacheAfterCommit();
        return response;
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

    public List<AiModelTranslationResponse> listAiModelTranslations(String modelCode) {
        var model = requireAiModel(modelCode);
        var english = aiModelTranslation(model.getId(), LanguageResolver.EN_US);
        return List.of(
                new AiModelTranslationResponse(model.getId(), modelCode, LanguageResolver.DEFAULT_LANGUAGE,
                        model.getModelName(), model.getVendorName(), true, model.getCreatedAt(), model.getUpdatedAt()),
                aiModelTranslationResponse(model, LanguageResolver.EN_US, english)
        );
    }

    @Transactional
    public AiModelTranslationResponse updateAiModelTranslation(
            String modelCode,
            AiModelTranslationRequest request,
            Long adminId,
            String ip
    ) {
        var model = requireAiModel(modelCode);
        var locale = requireSupportedLocale(request.locale());
        var modelName = trimToNull(request.modelName());
        var vendorName = trimToNull(request.vendorName());
        if (LanguageResolver.DEFAULT_LANGUAGE.equals(locale) && (modelName != null || vendorName != null)) {
            var now = DateTimeUtils.now();
            aiModelMapper.update(null, new LambdaUpdateWrapper<AiModel>()
                    .eq(AiModel::getId, model.getId())
                    .set(AiModel::getModelName, modelName == null ? model.getModelName() : modelName)
                    .set(AiModel::getVendorName, vendorName == null ? model.getVendorName() : vendorName)
                    .set(AiModel::getUpdatedAt, now));
            model.setModelName(modelName == null ? model.getModelName() : modelName);
            model.setVendorName(vendorName == null ? model.getVendorName() : vendorName);
            model.setUpdatedAt(now);
        }
        var response = upsertAiModelTranslation(model, locale, model.getModelName(), model.getVendorName(),
                modelName, vendorName);
        log(adminId, "UPDATE_AI_MODEL_TRANSLATION", "ai_model", model.getId(), response.locale(), ip);
        evictCatalogCacheAfterCommit();
        return response;
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

    public List<RentalCycleRuleTranslationResponse> listCycleRuleTranslations(String cycleCode) {
        var rule = requireCycleRule(cycleCode);
        var english = rentalCycleRuleTranslation(rule.getId(), LanguageResolver.EN_US);
        return List.of(
                new RentalCycleRuleTranslationResponse(rule.getId(), cycleCode, LanguageResolver.DEFAULT_LANGUAGE,
                        rule.getCycleName(), true, rule.getCreatedAt(), rule.getUpdatedAt()),
                rentalCycleRuleTranslationResponse(rule, LanguageResolver.EN_US, english)
        );
    }

    @Transactional
    public RentalCycleRuleTranslationResponse updateCycleRuleTranslation(
            String cycleCode,
            RentalCycleRuleTranslationRequest request,
            Long adminId,
            String ip
    ) {
        var rule = requireCycleRule(cycleCode);
        var locale = requireSupportedLocale(request.locale());
        var cycleName = trimToNull(request.cycleName());
        if (LanguageResolver.DEFAULT_LANGUAGE.equals(locale) && cycleName != null) {
            var now = DateTimeUtils.now();
            rentalCycleRuleMapper.update(null, new LambdaUpdateWrapper<RentalCycleRule>()
                    .eq(RentalCycleRule::getId, rule.getId())
                    .set(RentalCycleRule::getCycleName, cycleName)
                    .set(RentalCycleRule::getUpdatedAt, now));
            rule.setCycleName(cycleName);
            rule.setUpdatedAt(now);
        }
        var response = upsertRentalCycleRuleTranslation(rule, locale, rule.getCycleName(), cycleName);
        log(adminId, "UPDATE_RENTAL_CYCLE_RULE_TRANSLATION", "rental_cycle_rule", rule.getId(),
                response.locale(), ip);
        evictCatalogCacheAfterCommit();
        return response;
    }

    private RegionTranslation regionTranslation(Long regionId, String locale) {
        return regionTranslationMapper.selectOne(new LambdaQueryWrapper<RegionTranslation>()
                .eq(RegionTranslation::getRegionId, regionId)
                .eq(RegionTranslation::getLocale, locale)
                .last("LIMIT 1"));
    }

    private GpuModelTranslation gpuModelTranslation(Long gpuModelId, String locale) {
        return gpuModelTranslationMapper.selectOne(new LambdaQueryWrapper<GpuModelTranslation>()
                .eq(GpuModelTranslation::getGpuModelId, gpuModelId)
                .eq(GpuModelTranslation::getLocale, locale)
                .last("LIMIT 1"));
    }

    private ProductTranslation productTranslation(Long productId, String locale) {
        return productTranslationMapper.selectOne(new LambdaQueryWrapper<ProductTranslation>()
                .eq(ProductTranslation::getProductId, productId)
                .eq(ProductTranslation::getLocale, locale)
                .last("LIMIT 1"));
    }

    private AiModelTranslation aiModelTranslation(Long aiModelId, String locale) {
        return aiModelTranslationMapper.selectOne(new LambdaQueryWrapper<AiModelTranslation>()
                .eq(AiModelTranslation::getAiModelId, aiModelId)
                .eq(AiModelTranslation::getLocale, locale)
                .last("LIMIT 1"));
    }

    private RentalCycleRuleTranslation rentalCycleRuleTranslation(Long cycleRuleId, String locale) {
        return rentalCycleRuleTranslationMapper.selectOne(new LambdaQueryWrapper<RentalCycleRuleTranslation>()
                .eq(RentalCycleRuleTranslation::getCycleRuleId, cycleRuleId)
                .eq(RentalCycleRuleTranslation::getLocale, locale)
                .last("LIMIT 1"));
    }

    private RegionTranslationResponse upsertRegionTranslation(
            Long regionId,
            String locale,
            String defaultRegionName,
            String regionName
    ) {
        var now = DateTimeUtils.now();
        var translation = regionTranslation(regionId, locale);
        if (translation == null) {
            translation = new RegionTranslation();
            translation.setRegionId(regionId);
            translation.setLocale(locale);
            translation.setRegionName(regionName == null ? defaultRegionName : regionName);
            translation.setCreatedAt(now);
            translation.setUpdatedAt(now);
            regionTranslationMapper.insert(translation);
        } else {
            translation.setRegionName(regionName == null ? translation.getRegionName() : regionName);
            translation.setUpdatedAt(now);
            regionTranslationMapper.updateById(translation);
        }
        return regionTranslationResponse(regionId, locale, translation);
    }

    private GpuModelTranslationResponse upsertGpuModelTranslation(
            Long gpuModelId,
            String locale,
            String defaultModelName,
            String modelName
    ) {
        var now = DateTimeUtils.now();
        var translation = gpuModelTranslation(gpuModelId, locale);
        if (translation == null) {
            translation = new GpuModelTranslation();
            translation.setGpuModelId(gpuModelId);
            translation.setLocale(locale);
            translation.setModelName(modelName == null ? defaultModelName : modelName);
            translation.setCreatedAt(now);
            translation.setUpdatedAt(now);
            gpuModelTranslationMapper.insert(translation);
        } else {
            translation.setModelName(modelName == null ? translation.getModelName() : modelName);
            translation.setUpdatedAt(now);
            gpuModelTranslationMapper.updateById(translation);
        }
        return gpuModelTranslationResponse(gpuModelId, locale, translation);
    }

    private ProductTranslationResponse upsertProductTranslation(
            Product product,
            String locale,
            String defaultProductName,
            String productName
    ) {
        var now = DateTimeUtils.now();
        var translation = productTranslation(product.getId(), locale);
        if (translation == null) {
            translation = new ProductTranslation();
            translation.setProductId(product.getId());
            translation.setLocale(locale);
            translation.setProductName(productName == null ? defaultProductName : productName);
            translation.setCreatedAt(now);
            translation.setUpdatedAt(now);
            productTranslationMapper.insert(translation);
        } else {
            translation.setProductName(productName == null ? translation.getProductName() : productName);
            translation.setUpdatedAt(now);
            productTranslationMapper.updateById(translation);
        }
        return productTranslationResponse(product, locale, translation);
    }

    private AiModelTranslationResponse upsertAiModelTranslation(
            AiModel model,
            String locale,
            String defaultModelName,
            String defaultVendorName,
            String modelName,
            String vendorName
    ) {
        var now = DateTimeUtils.now();
        var translation = aiModelTranslation(model.getId(), locale);
        if (translation == null) {
            translation = new AiModelTranslation();
            translation.setAiModelId(model.getId());
            translation.setLocale(locale);
            translation.setModelName(modelName == null ? defaultModelName : modelName);
            translation.setVendorName(vendorName == null ? defaultVendorName : vendorName);
            translation.setCreatedAt(now);
            translation.setUpdatedAt(now);
            aiModelTranslationMapper.insert(translation);
        } else {
            translation.setModelName(modelName == null ? translation.getModelName() : modelName);
            translation.setVendorName(vendorName == null ? translation.getVendorName() : vendorName);
            translation.setUpdatedAt(now);
            aiModelTranslationMapper.updateById(translation);
        }
        return aiModelTranslationResponse(model, locale, translation);
    }

    private RentalCycleRuleTranslationResponse upsertRentalCycleRuleTranslation(
            RentalCycleRule rule,
            String locale,
            String defaultCycleName,
            String cycleName
    ) {
        var now = DateTimeUtils.now();
        var translation = rentalCycleRuleTranslation(rule.getId(), locale);
        if (translation == null) {
            translation = new RentalCycleRuleTranslation();
            translation.setCycleRuleId(rule.getId());
            translation.setLocale(locale);
            translation.setCycleName(cycleName == null ? defaultCycleName : cycleName);
            translation.setCreatedAt(now);
            translation.setUpdatedAt(now);
            rentalCycleRuleTranslationMapper.insert(translation);
        } else {
            translation.setCycleName(cycleName == null ? translation.getCycleName() : cycleName);
            translation.setUpdatedAt(now);
            rentalCycleRuleTranslationMapper.updateById(translation);
        }
        return rentalCycleRuleTranslationResponse(rule, locale, translation);
    }

    private RegionTranslationResponse regionTranslationResponse(
            Long regionId,
            String locale,
            RegionTranslation translation
    ) {
        return new RegionTranslationResponse(regionId, locale,
                translation == null ? null : translation.getRegionName(), translation != null,
                translation == null ? null : translation.getCreatedAt(),
                translation == null ? null : translation.getUpdatedAt());
    }

    private GpuModelTranslationResponse gpuModelTranslationResponse(
            Long gpuModelId,
            String locale,
            GpuModelTranslation translation
    ) {
        return new GpuModelTranslationResponse(gpuModelId, locale,
                translation == null ? null : translation.getModelName(), translation != null,
                translation == null ? null : translation.getCreatedAt(),
                translation == null ? null : translation.getUpdatedAt());
    }

    private ProductTranslationResponse productTranslationResponse(
            Product product,
            String locale,
            ProductTranslation translation
    ) {
        return new ProductTranslationResponse(product.getId(), product.getProductCode(), locale,
                translation == null ? null : translation.getProductName(), translation != null,
                translation == null ? null : translation.getCreatedAt(),
                translation == null ? null : translation.getUpdatedAt());
    }

    private AiModelTranslationResponse aiModelTranslationResponse(
            AiModel model,
            String locale,
            AiModelTranslation translation
    ) {
        return new AiModelTranslationResponse(model.getId(), model.getModelCode(), locale,
                translation == null ? null : translation.getModelName(),
                translation == null ? null : translation.getVendorName(),
                translation != null,
                translation == null ? null : translation.getCreatedAt(),
                translation == null ? null : translation.getUpdatedAt());
    }

    private RentalCycleRuleTranslationResponse rentalCycleRuleTranslationResponse(
            RentalCycleRule rule,
            String locale,
            RentalCycleRuleTranslation translation
    ) {
        return new RentalCycleRuleTranslationResponse(rule.getId(), rule.getCycleCode(), locale,
                translation == null ? null : translation.getCycleName(), translation != null,
                translation == null ? null : translation.getCreatedAt(),
                translation == null ? null : translation.getUpdatedAt());
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

    private String requireSupportedLocale(String locale) {
        var normalized = StringUtils.hasText(locale) ? locale.trim().replace('_', '-') : null;
        if (LanguageResolver.DEFAULT_LANGUAGE.equals(normalized) || LanguageResolver.EN_US.equals(normalized)) {
            return normalized;
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "Unsupported locale: " + locale);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return StringUtils.hasText(value);
    }
}

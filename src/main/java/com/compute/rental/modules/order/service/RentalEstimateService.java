package com.compute.rental.modules.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.compute.rental.common.enums.CommonStatus;
import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.common.i18n.LanguageResolver;
import com.compute.rental.common.util.MoneyUtils;
import com.compute.rental.modules.order.dto.RentalEstimateRequest;
import com.compute.rental.modules.order.dto.RentalEstimateResponse;
import com.compute.rental.modules.product.entity.AiModel;
import com.compute.rental.modules.product.entity.AiModelTranslation;
import com.compute.rental.modules.product.entity.Product;
import com.compute.rental.modules.product.entity.ProductTranslation;
import com.compute.rental.modules.product.entity.RentalCycleRule;
import com.compute.rental.modules.product.entity.RentalCycleRuleTranslation;
import com.compute.rental.modules.product.mapper.AiModelMapper;
import com.compute.rental.modules.product.mapper.AiModelTranslationMapper;
import com.compute.rental.modules.product.mapper.ProductMapper;
import com.compute.rental.modules.product.mapper.ProductTranslationMapper;
import com.compute.rental.modules.product.mapper.RentalCycleRuleMapper;
import com.compute.rental.modules.product.mapper.RentalCycleRuleTranslationMapper;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RentalEstimateService {

    private final ProductMapper productMapper;
    private final AiModelMapper aiModelMapper;
    private final RentalCycleRuleMapper rentalCycleRuleMapper;
    private final ProductTranslationMapper productTranslationMapper;
    private final AiModelTranslationMapper aiModelTranslationMapper;
    private final RentalCycleRuleTranslationMapper rentalCycleRuleTranslationMapper;

    public RentalEstimateService(
            ProductMapper productMapper,
            AiModelMapper aiModelMapper,
            RentalCycleRuleMapper rentalCycleRuleMapper,
            ProductTranslationMapper productTranslationMapper,
            AiModelTranslationMapper aiModelTranslationMapper,
            RentalCycleRuleTranslationMapper rentalCycleRuleTranslationMapper
    ) {
        this.productMapper = productMapper;
        this.aiModelMapper = aiModelMapper;
        this.rentalCycleRuleMapper = rentalCycleRuleMapper;
        this.productTranslationMapper = productTranslationMapper;
        this.aiModelTranslationMapper = aiModelTranslationMapper;
        this.rentalCycleRuleTranslationMapper = rentalCycleRuleTranslationMapper;
    }

    public RentalEstimateResponse estimate(RentalEstimateRequest request) {
        var locale = StringUtils.hasText(request.language()) ? request.language() : LanguageResolver.DEFAULT_LANGUAGE;
        return estimate(request, locale);
    }

    public RentalEstimateResponse estimate(RentalEstimateRequest request, String locale) {
        var product = requireEnabledProduct(request.productId());
        var aiModel = requireEnabledAiModel(request.aiModelId());
        var cycleRule = requireEnabledCycleRule(request.cycleRuleId());
        var productName = localized(product.getProductName(), locale, productTranslation(product.getId(), locale));
        var aiModelName = localized(aiModel.getModelName(), locale, aiModelTranslation(aiModel.getId(), locale));
        var cycleName = localized(cycleRule.getCycleName(), locale, cycleRuleTranslation(cycleRule.getId(), locale));
        var localeFallback = productName.fallback() || aiModelName.fallback() || cycleName.fallback();

        var tokenOutputPerDay = product.getTokenOutputPerDay() == null ? 0L : product.getTokenOutputPerDay();
        var expectedDailyProfit = MoneyUtils.scale(BigDecimal.valueOf(tokenOutputPerDay)
                .multiply(aiModel.getTokenUnitPrice())
                .multiply(cycleRule.getYieldMultiplier()));
        var expectedTotalProfit = MoneyUtils.scale(expectedDailyProfit.multiply(BigDecimal.valueOf(cycleRule.getCycleDays())));

        return new RentalEstimateResponse(
                product.getId(),
                productName.value(),
                aiModel.getId(),
                aiModelName.value(),
                cycleRule.getId(),
                cycleName.value(),
                cycleRule.getCycleDays(),
                product.getRentPrice(),
                aiModel.getDeployTechFee(),
                tokenOutputPerDay,
                aiModel.getTokenUnitPrice(),
                cycleRule.getYieldMultiplier(),
                expectedDailyProfit,
                expectedTotalProfit,
                localeFallback ? LanguageResolver.DEFAULT_LANGUAGE : locale,
                locale,
                localeFallback
        );
    }

    private Product requireEnabledProduct(Long productId) {
        var product = productMapper.selectById(productId);
        if (product == null || !Integer.valueOf(CommonStatus.ENABLED.value()).equals(product.getStatus())) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return product;
    }

    private AiModel requireEnabledAiModel(Long aiModelId) {
        var aiModel = aiModelMapper.selectById(aiModelId);
        if (aiModel == null || !Integer.valueOf(CommonStatus.ENABLED.value()).equals(aiModel.getStatus())) {
            throw new BusinessException(ErrorCode.AI_MODEL_NOT_FOUND);
        }
        return aiModel;
    }

    private RentalCycleRule requireEnabledCycleRule(Long cycleRuleId) {
        var cycleRule = rentalCycleRuleMapper.selectById(cycleRuleId);
        if (cycleRule == null || !Integer.valueOf(CommonStatus.ENABLED.value()).equals(cycleRule.getStatus())) {
            throw new BusinessException(ErrorCode.RENTAL_CYCLE_RULE_NOT_FOUND);
        }
        return cycleRule;
    }

    private String productTranslation(Long productId, String locale) {
        if (LanguageResolver.DEFAULT_LANGUAGE.equals(locale)) {
            return null;
        }
        var translation = productTranslationMapper.selectOne(new LambdaQueryWrapper<ProductTranslation>()
                .eq(ProductTranslation::getProductId, productId)
                .eq(ProductTranslation::getLocale, locale)
                .last("LIMIT 1"));
        return translation == null ? null : translation.getProductName();
    }

    private String aiModelTranslation(Long aiModelId, String locale) {
        if (LanguageResolver.DEFAULT_LANGUAGE.equals(locale)) {
            return null;
        }
        var translation = aiModelTranslationMapper.selectOne(new LambdaQueryWrapper<AiModelTranslation>()
                .eq(AiModelTranslation::getAiModelId, aiModelId)
                .eq(AiModelTranslation::getLocale, locale)
                .last("LIMIT 1"));
        return translation == null ? null : translation.getModelName();
    }

    private String cycleRuleTranslation(Long cycleRuleId, String locale) {
        if (LanguageResolver.DEFAULT_LANGUAGE.equals(locale)) {
            return null;
        }
        var translation = rentalCycleRuleTranslationMapper.selectOne(new LambdaQueryWrapper<RentalCycleRuleTranslation>()
                .eq(RentalCycleRuleTranslation::getCycleRuleId, cycleRuleId)
                .eq(RentalCycleRuleTranslation::getLocale, locale)
                .last("LIMIT 1"));
        return translation == null ? null : translation.getCycleName();
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

    private record LocalizedText(String value, String locale, boolean fallback) {
    }
}

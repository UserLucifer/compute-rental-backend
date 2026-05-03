package com.compute.rental.modules.order.service;

import com.compute.rental.common.enums.CommonStatus;
import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.common.util.MoneyUtils;
import com.compute.rental.modules.order.dto.RentalEstimateRequest;
import com.compute.rental.modules.order.dto.RentalEstimateResponse;
import com.compute.rental.modules.product.entity.AiModel;
import com.compute.rental.modules.product.entity.Product;
import com.compute.rental.modules.product.entity.RentalCycleRule;
import com.compute.rental.modules.product.mapper.AiModelMapper;
import com.compute.rental.modules.product.mapper.ProductMapper;
import com.compute.rental.modules.product.mapper.RentalCycleRuleMapper;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class RentalEstimateService {

    private final ProductMapper productMapper;
    private final AiModelMapper aiModelMapper;
    private final RentalCycleRuleMapper rentalCycleRuleMapper;

    public RentalEstimateService(
            ProductMapper productMapper,
            AiModelMapper aiModelMapper,
            RentalCycleRuleMapper rentalCycleRuleMapper
    ) {
        this.productMapper = productMapper;
        this.aiModelMapper = aiModelMapper;
        this.rentalCycleRuleMapper = rentalCycleRuleMapper;
    }

    public RentalEstimateResponse estimate(RentalEstimateRequest request) {
        var product = requireEnabledProduct(request.productId());
        var aiModel = requireEnabledAiModel(request.aiModelId());
        var cycleRule = requireEnabledCycleRule(request.cycleRuleId());

        var tokenOutputPerDay = product.getTokenOutputPerDay() == null ? 0L : product.getTokenOutputPerDay();
        var expectedDailyProfit = MoneyUtils.scale(BigDecimal.valueOf(tokenOutputPerDay)
                .multiply(aiModel.getTokenUnitPrice())
                .multiply(cycleRule.getYieldMultiplier()));
        var expectedTotalProfit = MoneyUtils.scale(expectedDailyProfit.multiply(BigDecimal.valueOf(cycleRule.getCycleDays())));

        return new RentalEstimateResponse(
                product.getId(),
                product.getProductName(),
                aiModel.getId(),
                aiModel.getModelName(),
                cycleRule.getId(),
                cycleRule.getCycleName(),
                cycleRule.getCycleDays(),
                product.getRentPrice(),
                aiModel.getDeployTechFee(),
                tokenOutputPerDay,
                aiModel.getTokenUnitPrice(),
                cycleRule.getYieldMultiplier(),
                expectedDailyProfit,
                expectedTotalProfit
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
}

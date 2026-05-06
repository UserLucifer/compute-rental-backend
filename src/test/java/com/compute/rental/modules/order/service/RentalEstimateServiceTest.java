package com.compute.rental.modules.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.compute.rental.common.enums.CommonStatus;
import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.modules.order.dto.RentalEstimateRequest;
import com.compute.rental.modules.product.entity.AiModel;
import com.compute.rental.modules.product.entity.Product;
import com.compute.rental.modules.product.entity.RentalCycleRule;
import com.compute.rental.modules.product.mapper.AiModelMapper;
import com.compute.rental.modules.product.mapper.AiModelTranslationMapper;
import com.compute.rental.modules.product.mapper.ProductMapper;
import com.compute.rental.modules.product.mapper.ProductTranslationMapper;
import com.compute.rental.modules.product.mapper.RentalCycleRuleMapper;
import com.compute.rental.modules.product.mapper.RentalCycleRuleTranslationMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RentalEstimateServiceTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private AiModelMapper aiModelMapper;

    @Mock
    private RentalCycleRuleMapper rentalCycleRuleMapper;

    @Mock
    private ProductTranslationMapper productTranslationMapper;

    @Mock
    private AiModelTranslationMapper aiModelTranslationMapper;

    @Mock
    private RentalCycleRuleTranslationMapper rentalCycleRuleTranslationMapper;

    @InjectMocks
    private RentalEstimateService rentalEstimateService;

    @Test
    void estimateShouldCalculateExpectedProfitFromDatabaseValues() {
        when(productMapper.selectById(1L)).thenReturn(product(CommonStatus.ENABLED.value()));
        when(aiModelMapper.selectById(2L)).thenReturn(aiModel(CommonStatus.ENABLED.value()));
        when(rentalCycleRuleMapper.selectById(3L)).thenReturn(cycleRule(CommonStatus.ENABLED.value()));

        var response = rentalEstimateService.estimate(new RentalEstimateRequest(1L, 2L, 3L, null));

        assertThat(response.tokenOutputPerDay()).isEqualTo(1000L);
        assertThat(response.tokenUnitPrice()).isEqualByComparingTo("0.01000000");
        assertThat(response.yieldMultiplier()).isEqualByComparingTo("1.20000000");
        assertThat(response.expectedDailyProfit()).isEqualByComparingTo("12.00000000");
        assertThat(response.expectedTotalProfit()).isEqualByComparingTo("360.00000000");
    }

    @Test
    void estimateShouldRejectMissingProduct() {
        when(productMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> rentalEstimateService.estimate(new RentalEstimateRequest(1L, 2L, 3L, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    void estimateShouldRejectMissingAiModel() {
        when(productMapper.selectById(1L)).thenReturn(product(CommonStatus.ENABLED.value()));
        when(aiModelMapper.selectById(2L)).thenReturn(null);

        assertThatThrownBy(() -> rentalEstimateService.estimate(new RentalEstimateRequest(1L, 2L, 3L, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AI_MODEL_NOT_FOUND);
    }

    @Test
    void estimateShouldRejectMissingCycleRule() {
        when(productMapper.selectById(1L)).thenReturn(product(CommonStatus.ENABLED.value()));
        when(aiModelMapper.selectById(2L)).thenReturn(aiModel(CommonStatus.ENABLED.value()));
        when(rentalCycleRuleMapper.selectById(3L)).thenReturn(null);

        assertThatThrownBy(() -> rentalEstimateService.estimate(new RentalEstimateRequest(1L, 2L, 3L, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RENTAL_CYCLE_RULE_NOT_FOUND);
    }

    @Test
    void estimateShouldRejectDisabledProduct() {
        when(productMapper.selectById(1L)).thenReturn(product(CommonStatus.DISABLED.value()));

        assertThatThrownBy(() -> rentalEstimateService.estimate(new RentalEstimateRequest(1L, 2L, 3L, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    void estimateShouldRejectDisabledAiModel() {
        when(productMapper.selectById(1L)).thenReturn(product(CommonStatus.ENABLED.value()));
        when(aiModelMapper.selectById(2L)).thenReturn(aiModel(CommonStatus.DISABLED.value()));

        assertThatThrownBy(() -> rentalEstimateService.estimate(new RentalEstimateRequest(1L, 2L, 3L, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AI_MODEL_NOT_FOUND);
    }

    @Test
    void estimateShouldRejectDisabledCycleRule() {
        when(productMapper.selectById(1L)).thenReturn(product(CommonStatus.ENABLED.value()));
        when(aiModelMapper.selectById(2L)).thenReturn(aiModel(CommonStatus.ENABLED.value()));
        when(rentalCycleRuleMapper.selectById(3L)).thenReturn(cycleRule(CommonStatus.DISABLED.value()));

        assertThatThrownBy(() -> rentalEstimateService.estimate(new RentalEstimateRequest(1L, 2L, 3L, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RENTAL_CYCLE_RULE_NOT_FOUND);
    }

    private Product product(Integer status) {
        var product = new Product();
        product.setId(1L);
        product.setProductName("A100 Rental");
        product.setRentPrice(new BigDecimal("1000.00000000"));
        product.setTokenOutputPerDay(1000L);
        product.setStatus(status);
        return product;
    }

    private AiModel aiModel(Integer status) {
        var aiModel = new AiModel();
        aiModel.setId(2L);
        aiModel.setModelName("GPT Test");
        aiModel.setTokenUnitPrice(new BigDecimal("0.01000000"));
        aiModel.setDeployTechFee(new BigDecimal("100.00000000"));
        aiModel.setStatus(status);
        return aiModel;
    }

    private RentalCycleRule cycleRule(Integer status) {
        var rule = new RentalCycleRule();
        rule.setId(3L);
        rule.setCycleName("Monthly");
        rule.setCycleDays(30);
        rule.setYieldMultiplier(new BigDecimal("1.20000000"));
        rule.setStatus(status);
        return rule;
    }
}

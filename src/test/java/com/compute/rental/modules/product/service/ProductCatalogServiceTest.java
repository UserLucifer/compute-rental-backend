package com.compute.rental.modules.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.compute.rental.common.enums.CommonStatus;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.common.util.RedisKeys;
import com.compute.rental.modules.product.dto.ProductQueryRequest;
import com.compute.rental.modules.product.dto.ProductResponse;
import com.compute.rental.modules.product.dto.RegionResponse;
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
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductCatalogServiceTest {

    @Mock
    private RegionMapper regionMapper;

    @Mock
    private GpuModelMapper gpuModelMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private AiModelMapper aiModelMapper;

    @Mock
    private RentalCycleRuleMapper rentalCycleRuleMapper;

    @Mock
    private ProductCatalogCacheService productCatalogCacheService;

    @InjectMocks
    private ProductCatalogService productCatalogService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        var configuration = new Configuration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), Region.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), GpuModel.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), Product.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), AiModel.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), RentalCycleRule.class);
    }

    @Test
    void listEnabledRegionsShouldReturnCachedValueWhenPresent() {
        var cached = List.of(new RegionResponse(9L, "SG", "Singapore"));
        when(productCatalogCacheService.get(eq(RedisKeys.catalogRegions()), any())).thenReturn(cached);

        var result = productCatalogService.listEnabledRegions();

        assertThat(result).isEqualTo(cached);
        verify(regionMapper, never()).selectList(any());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void listEnabledRegionsShouldCacheDbResult() {
        when(regionMapper.selectList(any(Wrapper.class))).thenReturn(List.of(region()));

        var result = productCatalogService.listEnabledRegions();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).regionCode()).isEqualTo("HK");
        verify(productCatalogCacheService).put(eq(RedisKeys.catalogRegions()), eq(result));
    }

    @Test
    void pageEnabledProductsShouldReturnCachedValueWhenPresent() {
        var request = new ProductQueryRequest(1, 10, 1L, 2L);
        var cached = new PageResult<ProductResponse>(List.of(), 0, 1, 10);
        when(productCatalogCacheService.get(eq(RedisKeys.catalogProductPage(1, 10, 1L, 2L)), any()))
                .thenReturn(cached);

        var result = productCatalogService.pageEnabledProducts(request);

        assertThat(result).isEqualTo(cached);
        verify(productMapper, never()).selectPage(any(), any());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void pageEnabledProductsShouldQueryEnabledProductsAndMapDisplayFields() {
        var product = product();
        var page = new Page<Product>(1, 10);
        page.setRecords(List.of(product));
        page.setTotal(1);
        when(productMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);
        when(regionMapper.selectBatchIds(any())).thenReturn(List.of(region()));
        when(gpuModelMapper.selectBatchIds(any())).thenReturn(List.of(gpuModel()));

        var result = productCatalogService.pageEnabledProducts(new ProductQueryRequest(1, 10, 1L, 2L));

        assertThat(result.records()).hasSize(1);
        assertThat(result.records().get(0).regionName()).isEqualTo("Hong Kong");
        assertThat(result.records().get(0).gpuModelName()).isEqualTo("NVIDIA A100");
        assertThat(result.records().get(0).availableStock()).isEqualTo(7);

        var wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(productMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getSqlSegment()).contains("status", "region_id", "gpu_model_id");
        verify(productCatalogCacheService).put(eq(RedisKeys.catalogProductPage(1, 10, 1L, 2L)), eq(result));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void listEnabledAiModelsShouldQueryEnabledModels() {
        when(aiModelMapper.selectList(any(Wrapper.class))).thenReturn(List.of(aiModel()));

        var result = productCatalogService.listEnabledAiModels();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).modelCode()).isEqualTo("GPT_TEST");
        var wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(aiModelMapper).selectList(wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getSqlSegment()).contains("status", "sort_no");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void listEnabledCycleRulesShouldQueryEnabledRules() {
        when(rentalCycleRuleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(cycleRule()));

        var result = productCatalogService.listEnabledCycleRules();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).cycleCode()).isEqualTo("MONTHLY");
        var wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(rentalCycleRuleMapper).selectList(wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getSqlSegment()).contains("status", "sort_no");
    }

    private Product product() {
        var product = new Product();
        product.setId(100L);
        product.setProductCode("P001");
        product.setProductName("A100 Rental");
        product.setMachineCode("M001");
        product.setMachineAlias("A100-1");
        product.setRegionId(1L);
        product.setGpuModelId(2L);
        product.setGpuMemoryGb(80);
        product.setGpuPowerTops(new BigDecimal("312.00000000"));
        product.setRentPrice(new BigDecimal("1000.00000000"));
        product.setTokenOutputPerMinute(100L);
        product.setTokenOutputPerDay(144000L);
        product.setTotalStock(10);
        product.setAvailableStock(7);
        product.setRentedStock(3);
        product.setStatus(CommonStatus.ENABLED.value());
        product.setSortNo(1);
        return product;
    }

    private Region region() {
        var region = new Region();
        region.setId(1L);
        region.setRegionCode("HK");
        region.setRegionName("Hong Kong");
        region.setStatus(CommonStatus.ENABLED.value());
        return region;
    }

    private GpuModel gpuModel() {
        var gpuModel = new GpuModel();
        gpuModel.setId(2L);
        gpuModel.setModelCode("A100");
        gpuModel.setModelName("NVIDIA A100");
        gpuModel.setStatus(CommonStatus.ENABLED.value());
        return gpuModel;
    }

    private AiModel aiModel() {
        var model = new AiModel();
        model.setId(3L);
        model.setModelCode("GPT_TEST");
        model.setModelName("GPT Test");
        model.setTokenUnitPrice(new BigDecimal("0.01000000"));
        model.setDeployTechFee(new BigDecimal("100.00000000"));
        model.setStatus(CommonStatus.ENABLED.value());
        model.setSortNo(1);
        return model;
    }

    private RentalCycleRule cycleRule() {
        var rule = new RentalCycleRule();
        rule.setId(4L);
        rule.setCycleCode("MONTHLY");
        rule.setCycleName("Monthly");
        rule.setCycleDays(30);
        rule.setYieldMultiplier(new BigDecimal("1.20000000"));
        rule.setEarlyPenaltyRate(new BigDecimal("0.10000000"));
        rule.setStatus(CommonStatus.ENABLED.value());
        rule.setSortNo(1);
        return rule;
    }
}

package com.compute.rental.modules.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.compute.rental.common.enums.CommonStatus;
import com.compute.rental.modules.product.dto.AdminProductRequest;
import com.compute.rental.modules.product.dto.AdminRegionRequest;
import com.compute.rental.modules.product.dto.ProductTranslationRequest;
import com.compute.rental.modules.product.entity.GpuModel;
import com.compute.rental.modules.product.entity.Product;
import com.compute.rental.modules.product.entity.ProductTranslation;
import com.compute.rental.modules.product.entity.Region;
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
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class AdminCatalogManagementServiceTest {

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
    private RegionTranslationMapper regionTranslationMapper;
    @Mock
    private GpuModelTranslationMapper gpuModelTranslationMapper;
    @Mock
    private ProductTranslationMapper productTranslationMapper;
    @Mock
    private AiModelTranslationMapper aiModelTranslationMapper;
    @Mock
    private RentalCycleRuleTranslationMapper rentalCycleRuleTranslationMapper;
    @Mock
    private AdminLogService adminLogService;
    @Mock
    private ProductCatalogCacheService productCatalogCacheService;

    private AdminCatalogManagementService service;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        var configuration = new Configuration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), Product.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), ProductTranslation.class);
    }

    @BeforeEach
    void setUp() {
        service = new AdminCatalogManagementService(regionMapper, gpuModelMapper, productMapper, aiModelMapper,
                rentalCycleRuleMapper, regionTranslationMapper, gpuModelTranslationMapper, productTranslationMapper,
                aiModelTranslationMapper, rentalCycleRuleTranslationMapper, adminLogService,
                productCatalogCacheService);
    }

    @Test
    void pageProductsIncludesRegionAndGpuModelNames() {
        var product = new Product();
        product.setId(100L);
        product.setProductCode("P100");
        product.setRegionId(10L);
        product.setGpuModelId(20L);

        var page = new Page<Product>(1, 10);
        page.setRecords(List.of(product));
        page.setTotal(1);

        var region = new Region();
        region.setId(10L);
        region.setRegionName("北京B区");

        var gpuModel = new GpuModel();
        gpuModel.setId(20L);
        gpuModel.setModelName("RTX 4090");

        when(productMapper.selectPage(any(), any())).thenReturn(page);
        when(regionMapper.selectBatchIds(any())).thenReturn(List.of(region));
        when(gpuModelMapper.selectBatchIds(any())).thenReturn(List.of(gpuModel));

        var result = service.pageProducts(1, 10, null, null, null, null);
        var response = result.records().get(0);

        assertEquals(10L, response.regionId());
        assertEquals("北京B区", response.regionName());
        assertEquals(20L, response.gpuModelId());
        assertEquals("RTX 4090", response.gpuModelName());
    }

    @Test
    void disablingProductWritesAdminLog() {
        var product = new Product();
        product.setId(100L);
        product.setProductCode("P100");
        product.setStatus(CommonStatus.ENABLED.value());
        when(productMapper.selectOne(any())).thenReturn(product);
        when(productMapper.update(isNull(), any())).thenReturn(1);

        var result = service.setProductStatus("P100", CommonStatus.DISABLED.value(), 9L, "127.0.0.1");

        assertEquals("P100", result.productCode());
        verify(adminLogService).log(eq(9L), eq("DISABLE_PRODUCT"), eq("product"), eq(100L),
                isNull(), isNull(), eq("status=0"), eq("127.0.0.1"));
        verify(productCatalogCacheService).evictCatalog();
    }

    @Test
    void createRegionShouldEvictCatalogOnlyAfterTransactionCommit() {
        var request = new AdminRegionRequest("HK", "Hong Kong", 1, null);

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.createRegion(request, 9L, "127.0.0.1");

            verify(productCatalogCacheService, never()).evictCatalog();
            var synchronizations = TransactionSynchronizationManager.getSynchronizations();
            assertEquals(1, synchronizations.size());
            synchronizations.get(0).afterCommit();
            verify(productCatalogCacheService).evictCatalog();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void updateProductShouldUsePathProductCode() {
        var existing = new Product();
        existing.setId(100L);
        existing.setProductCode("P100");
        when(productMapper.selectOne(any())).thenReturn(existing);

        var request = new AdminProductRequest(
                "P999",
                "Updated Product",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        service.updateProduct("P100", request, 9L, "127.0.0.1");

        var captor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).updateById(captor.capture());
        assertEquals(100L, captor.getValue().getId());
        assertEquals("P100", captor.getValue().getProductCode());
        assertEquals("Updated Product", captor.getValue().getProductName());
    }

    @Test
    void updateProductTranslationCreatesEnglishTranslation() {
        var product = new Product();
        product.setId(100L);
        product.setProductCode("P100");
        product.setProductName("中文商品");
        when(productMapper.selectOne(any())).thenReturn(product);

        var result = service.updateProductTranslation("P100",
                new ProductTranslationRequest("en-US", "English Product"), 9L, "127.0.0.1");

        assertEquals(100L, result.productId());
        assertEquals("P100", result.productCode());
        assertEquals("en-US", result.locale());
        assertEquals("English Product", result.productName());

        var captor = ArgumentCaptor.forClass(ProductTranslation.class);
        verify(productTranslationMapper).insert(captor.capture());
        assertEquals(100L, captor.getValue().getProductId());
        assertEquals("en-US", captor.getValue().getLocale());
        assertEquals("English Product", captor.getValue().getProductName());
        verify(adminLogService).log(eq(9L), eq("UPDATE_PRODUCT_TRANSLATION"), eq("product"), eq(100L),
                isNull(), isNull(), eq("en-US"), eq("127.0.0.1"));
        verify(productCatalogCacheService).evictCatalog();
    }
}

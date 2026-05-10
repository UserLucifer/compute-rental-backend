package com.compute.rental.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.junit.jupiter.api.Test;

class MybatisPlusConfigTest {

    @Test
    void mybatisPlusInterceptorShouldEnableOptimisticLockBeforePagination() {
        var interceptor = new MybatisPlusConfig().mybatisPlusInterceptor();

        assertThat(interceptor.getInterceptors())
                .extracting(Object::getClass)
                .containsExactly(OptimisticLockerInnerInterceptor.class, PaginationInnerInterceptor.class);
    }
}

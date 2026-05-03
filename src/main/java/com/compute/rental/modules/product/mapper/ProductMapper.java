package com.compute.rental.modules.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.compute.rental.modules.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}

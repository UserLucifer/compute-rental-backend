package com.compute.rental.modules.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.compute.rental.modules.product.entity.GpuModel;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface GpuModelMapper extends BaseMapper<GpuModel> {

    @Select("""
            SELECT DISTINCT gm.*
            FROM product p
            INNER JOIN gpu_model gm ON gm.id = p.gpu_model_id
            WHERE p.region_id = #{regionId}
              AND p.status = 1
              AND gm.status = 1
            ORDER BY gm.sort_no ASC, gm.id DESC
            """)
    List<GpuModel> selectEnabledByRegionId(@Param("regionId") Long regionId);
}

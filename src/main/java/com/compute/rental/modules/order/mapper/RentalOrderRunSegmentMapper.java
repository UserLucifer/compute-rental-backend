package com.compute.rental.modules.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.compute.rental.modules.order.entity.RentalOrderRunSegment;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RentalOrderRunSegmentMapper extends BaseMapper<RentalOrderRunSegment> {

    @Select("""
            SELECT DISTINCT rental_order_id
            FROM rental_order_run_segment
            WHERE rental_order_id > #{lastOrderId}
              AND segment_start_at < #{windowEnd}
              AND (segment_end_at IS NULL OR segment_end_at > #{windowStart})
            ORDER BY rental_order_id
            LIMIT #{limit}
            """)
    List<Long> selectOverlappingOrderIds(@Param("windowStart") LocalDateTime windowStart,
                                         @Param("windowEnd") LocalDateTime windowEnd,
                                         @Param("lastOrderId") Long lastOrderId,
                                         @Param("limit") int limit);
}

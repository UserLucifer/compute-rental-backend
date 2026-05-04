package com.compute.rental.modules.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.compute.rental.common.query.StatusCountRow;
import com.compute.rental.modules.order.entity.RentalOrder;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RentalOrderMapper extends BaseMapper<RentalOrder> {

    @Select("""
            SELECT COALESCE(SUM(paid_amount), 0)
            FROM rental_order
            WHERE paid_at IS NOT NULL
            """)
    BigDecimal sumPaidAmount();

    @Select("""
            SELECT order_status AS status, COUNT(*) AS total
            FROM rental_order
            GROUP BY order_status
            """)
    List<StatusCountRow> countByOrderStatusGroup();

    @Select("""
            SELECT profit_status AS status, COUNT(*) AS total
            FROM rental_order
            GROUP BY profit_status
            """)
    List<StatusCountRow> countByProfitStatusGroup();
}

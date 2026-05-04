package com.compute.rental.modules.wallet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.compute.rental.modules.wallet.entity.RechargeOrder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RechargeOrderMapper extends BaseMapper<RechargeOrder> {

    @Select("""
            SELECT COALESCE(SUM(actual_amount), 0)
            FROM recharge_order
            WHERE status = #{status}
            """)
    BigDecimal sumActualAmountByStatus(@Param("status") String status);

    @Select("""
            SELECT COALESCE(SUM(actual_amount), 0)
            FROM recharge_order
            WHERE status = #{status}
              AND credited_at >= #{startAt}
              AND credited_at <= #{endAt}
            """)
    BigDecimal sumActualAmountByStatusAndCreditedAtRange(
            @Param("status") String status,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );
}

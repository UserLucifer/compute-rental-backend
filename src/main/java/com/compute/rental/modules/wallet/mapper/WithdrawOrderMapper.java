package com.compute.rental.modules.wallet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.compute.rental.modules.wallet.entity.WithdrawOrder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WithdrawOrderMapper extends BaseMapper<WithdrawOrder> {

    @Select("""
            SELECT COALESCE(SUM(actual_amount), 0)
            FROM withdraw_order
            WHERE status = #{status}
            """)
    BigDecimal sumActualAmountByStatus(@Param("status") String status);

    @Select("""
            SELECT COALESCE(SUM(actual_amount), 0)
            FROM withdraw_order
            WHERE status = #{status}
              AND paid_at >= #{startAt}
              AND paid_at <= #{endAt}
            """)
    BigDecimal sumActualAmountByStatusAndPaidAtRange(
            @Param("status") String status,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );
}

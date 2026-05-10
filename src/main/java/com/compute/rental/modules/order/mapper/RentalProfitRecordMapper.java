package com.compute.rental.modules.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.compute.rental.common.query.ProfitSummaryAggregateRow;
import com.compute.rental.common.query.ProfitTrendRow;
import com.compute.rental.modules.order.entity.RentalProfitRecord;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RentalProfitRecordMapper extends BaseMapper<RentalProfitRecord> {

    @Select("""
            SELECT COALESCE(SUM(final_profit_amount), 0)
            FROM rental_profit_record
            WHERE status = #{status}
            """)
    BigDecimal sumFinalProfitAmountByStatus(@Param("status") String status);

    @Select("""
            SELECT COALESCE(SUM(final_profit_amount), 0)
            FROM rental_profit_record
            WHERE status = #{status}
              AND profit_date = #{profitDate}
            """)
    BigDecimal sumFinalProfitAmountByStatusAndProfitDate(
            @Param("status") String status,
            @Param("profitDate") LocalDate profitDate
    );

    @Select("""
            SELECT
                COALESCE(SUM(CASE WHEN profit_date = #{today} THEN final_profit_amount ELSE 0 END), 0) AS today_profit,
                COALESCE(SUM(CASE WHEN profit_date = #{yesterday} THEN final_profit_amount ELSE 0 END), 0) AS yesterday_profit,
                COALESCE(SUM(CASE WHEN profit_date >= #{monthStart} THEN final_profit_amount ELSE 0 END), 0) AS current_month_profit,
                COUNT(*) AS settled_profit_count
            FROM rental_profit_record
            WHERE user_id = #{userId}
              AND status = #{status}
            """)
    ProfitSummaryAggregateRow userSummaryAggregate(
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("today") LocalDate today,
            @Param("yesterday") LocalDate yesterday,
            @Param("monthStart") LocalDate monthStart
    );

    @Select("""
            SELECT profit_date AS profit_date,
                   COALESCE(SUM(final_profit_amount), 0) AS final_profit_amount,
                   COUNT(*) AS record_count
            FROM rental_profit_record
            WHERE user_id = #{userId}
              AND status = #{status}
              AND profit_date >= #{startDate}
              AND profit_date <= #{endDate}
            GROUP BY profit_date
            ORDER BY profit_date ASC
            """)
    List<ProfitTrendRow> userTrendByDate(
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}

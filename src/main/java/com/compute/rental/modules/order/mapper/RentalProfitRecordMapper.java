package com.compute.rental.modules.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.compute.rental.modules.order.entity.RentalProfitRecord;
import java.math.BigDecimal;
import java.time.LocalDate;
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
}

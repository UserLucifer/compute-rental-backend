package com.compute.rental.modules.commission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.compute.rental.modules.commission.entity.CommissionRecord;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CommissionRecordMapper extends BaseMapper<CommissionRecord> {

    @Select("""
            SELECT COALESCE(SUM(commission_amount), 0)
            FROM commission_record
            WHERE status = #{status}
            """)
    BigDecimal sumCommissionAmountByStatus(@Param("status") String status);

    @Select("""
            SELECT COALESCE(SUM(commission_amount), 0)
            FROM commission_record
            WHERE status = #{status}
              AND settled_at >= #{startAt}
              AND settled_at <= #{endAt}
            """)
    BigDecimal sumCommissionAmountByStatusAndSettledAtRange(
            @Param("status") String status,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );

    @Select("""
            SELECT COALESCE(SUM(commission_amount), 0)
            FROM commission_record
            WHERE benefit_user_id = #{benefitUserId}
              AND status = #{status}
              AND level_no <= #{maxLevelNo}
            """)
    BigDecimal sumUserCommissionAmount(
            @Param("benefitUserId") Long benefitUserId,
            @Param("status") String status,
            @Param("maxLevelNo") Integer maxLevelNo
    );

    @Select("""
            SELECT COALESCE(SUM(commission_amount), 0)
            FROM commission_record
            WHERE benefit_user_id = #{benefitUserId}
              AND status = #{status}
              AND level_no <= #{maxLevelNo}
              AND settled_at >= #{startAt}
              AND settled_at < #{endAt}
            """)
    BigDecimal sumUserCommissionAmountBySettledAtRange(
            @Param("benefitUserId") Long benefitUserId,
            @Param("status") String status,
            @Param("maxLevelNo") Integer maxLevelNo,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );

    @Select("""
            SELECT COALESCE(SUM(commission_amount), 0)
            FROM commission_record
            WHERE benefit_user_id = #{benefitUserId}
              AND status = #{status}
              AND level_no <= #{maxLevelNo}
              AND settled_at >= #{startAt}
            """)
    BigDecimal sumUserCommissionAmountSince(
            @Param("benefitUserId") Long benefitUserId,
            @Param("status") String status,
            @Param("maxLevelNo") Integer maxLevelNo,
            @Param("startAt") LocalDateTime startAt
    );

    @Select("""
            SELECT COALESCE(SUM(commission_amount), 0)
            FROM commission_record
            WHERE benefit_user_id = #{benefitUserId}
              AND status = #{status}
              AND level_no = #{levelNo}
            """)
    BigDecimal sumUserCommissionAmountByLevel(
            @Param("benefitUserId") Long benefitUserId,
            @Param("status") String status,
            @Param("levelNo") Integer levelNo
    );
}

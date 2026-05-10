package com.compute.rental.modules.commission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.compute.rental.modules.commission.dto.CommissionSourceAmountRow;
import com.compute.rental.modules.commission.entity.CommissionRecord;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
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

    @Select("""
            <script>
            SELECT COALESCE(SUM(commission_amount), 0)
            FROM commission_record
            WHERE level_no &lt;= #{maxLevelNo}
              AND status IN
              <foreach collection="statuses" item="status" open="(" separator="," close=")">
                #{status}
              </foreach>
            <if test="startAt != null">
              AND created_at &gt;= #{startAt}
            </if>
            <if test="endAt != null">
              AND created_at &lt; #{endAt}
            </if>
            </script>
            """)
    BigDecimal sumCommissionAmountByStatusesAndCreatedAtRange(
            @Param("statuses") Collection<String> statuses,
            @Param("maxLevelNo") Integer maxLevelNo,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );

    @Select("""
            <script>
            SELECT
              source_user_id AS source_user_id,
              COALESCE(SUM(commission_amount), 0) AS amount
            FROM commission_record
            WHERE benefit_user_id = #{benefitUserId}
              AND source_user_id IN
              <foreach collection="sourceUserIds" item="sourceUserId" open="(" separator="," close=")">
                #{sourceUserId}
              </foreach>
              AND status = #{status}
              AND level_no &lt;= #{maxLevelNo}
            <if test="startAt != null">
              AND settled_at &gt;= #{startAt}
            </if>
            <if test="endAt != null">
              AND settled_at &lt; #{endAt}
            </if>
            GROUP BY source_user_id
            </script>
            """)
    List<CommissionSourceAmountRow> sumSettledContributionBySource(
            @Param("benefitUserId") Long benefitUserId,
            @Param("sourceUserIds") Collection<Long> sourceUserIds,
            @Param("status") String status,
            @Param("maxLevelNo") Integer maxLevelNo,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );
}

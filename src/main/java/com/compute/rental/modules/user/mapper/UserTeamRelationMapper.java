package com.compute.rental.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.compute.rental.modules.system.dto.AdminTeamAggregateRow;
import com.compute.rental.modules.user.entity.UserTeamRelation;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserTeamRelationMapper extends BaseMapper<UserTeamRelation> {

    @Select("""
            SELECT COUNT(DISTINCT relation.ancestor_user_id)
            FROM user_team_relation relation
            JOIN app_user descendant ON descendant.id = relation.descendant_user_id
            WHERE relation.level_depth <= #{maxLevel}
              AND descendant.status = #{enabledStatus}
            """)
    long countActiveTeamAncestors(
            @Param("maxLevel") int maxLevel,
            @Param("enabledStatus") int enabledStatus
    );

    @Select("""
            <script>
            SELECT COUNT(DISTINCT relation.ancestor_user_id)
            FROM user_team_relation relation
            JOIN app_user team_user ON team_user.id = relation.ancestor_user_id
            WHERE relation.level_depth &lt;= #{maxLevel}
            <if test="status != null">
              AND team_user.status = #{status}
            </if>
            <if test="keyword != null and keyword != ''">
              AND (
                team_user.user_id = #{keyword}
                OR team_user.email = #{keyword}
                OR team_user.user_name LIKE CONCAT(#{keyword}, '%')
                OR team_user.email LIKE CONCAT(#{keyword}, '%')
                <if test="internalUserId != null">
                  OR team_user.id = #{internalUserId}
                </if>
              )
            </if>
            </script>
            """)
    long countAdminTeamAggregates(
            @Param("maxLevel") int maxLevel,
            @Param("keyword") String keyword,
            @Param("internalUserId") Long internalUserId,
            @Param("status") Integer status
    );

    @Select("""
            <script>
            SELECT
              team_user.id AS user_id,
              team_user.user_name AS user_name,
              team_user.email AS email,
              team_user.avatar_key AS avatar_key,
              team_user.status AS user_status,
              team_count.direct_count AS direct_count,
              team_count.indirect_count AS indirect_count,
              team_count.total_team_count AS total_team_count,
              COALESCE(commission.yesterday_commission, 0) AS yesterday_commission,
              COALESCE(commission.total_commission, 0) AS total_commission,
              COALESCE(team_order.active_order_count, 0) AS active_order_count,
              COALESCE(team_order.running_order_count, 0) AS running_order_count,
              commission.last_commission_at AS last_commission_at
            FROM (
              SELECT
                relation.ancestor_user_id AS user_id,
                COUNT(CASE WHEN relation.level_depth = 1 THEN 1 END) AS direct_count,
                COUNT(CASE WHEN relation.level_depth = 2 THEN 1 END) AS indirect_count,
                COUNT(*) AS total_team_count
              FROM user_team_relation relation
              WHERE relation.level_depth &lt;= #{maxLevel}
              GROUP BY relation.ancestor_user_id
            ) team_count
            JOIN app_user team_user ON team_user.id = team_count.user_id
            LEFT JOIN (
              SELECT
                record.benefit_user_id AS user_id,
                SUM(CASE
                    WHEN record.settled_at &gt;= #{yesterdayStart}
                     AND record.settled_at &lt; #{todayStart}
                    THEN record.commission_amount
                    ELSE 0
                END) AS yesterday_commission,
                SUM(record.commission_amount) AS total_commission,
                MAX(record.settled_at) AS last_commission_at
              FROM commission_record record
              WHERE record.status = #{settledStatus}
                AND record.level_no &lt;= #{maxLevel}
              GROUP BY record.benefit_user_id
            ) commission ON commission.user_id = team_user.id
            LEFT JOIN (
              SELECT
                relation.ancestor_user_id AS user_id,
                COUNT(`order`.id) AS active_order_count,
                COUNT(CASE WHEN `order`.order_status = #{runningStatus} THEN 1 END) AS running_order_count
              FROM user_team_relation relation
              JOIN rental_order `order` ON `order`.user_id = relation.descendant_user_id
              WHERE relation.level_depth &lt;= #{maxLevel}
                AND `order`.order_status IN (#{runningStatus}, #{pausedStatus})
              GROUP BY relation.ancestor_user_id
            ) team_order ON team_order.user_id = team_user.id
            WHERE 1 = 1
            <if test="status != null">
              AND team_user.status = #{status}
            </if>
            <if test="keyword != null and keyword != ''">
              AND (
                team_user.user_id = #{keyword}
                OR team_user.email = #{keyword}
                OR team_user.user_name LIKE CONCAT(#{keyword}, '%')
                OR team_user.email LIKE CONCAT(#{keyword}, '%')
                <if test="internalUserId != null">
                  OR team_user.id = #{internalUserId}
                </if>
              )
            </if>
            ORDER BY
            <choose>
              <when test='sortBy == "totalCommission"'>
                total_commission DESC, user_id DESC
              </when>
              <when test='sortBy == "yesterdayCommission"'>
                yesterday_commission DESC, user_id DESC
              </when>
              <when test='sortBy == "directCount"'>
                direct_count DESC, user_id DESC
              </when>
              <otherwise>
                last_commission_at DESC, user_id DESC
              </otherwise>
            </choose>
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<AdminTeamAggregateRow> selectAdminTeamAggregatePage(
            @Param("maxLevel") int maxLevel,
            @Param("keyword") String keyword,
            @Param("internalUserId") Long internalUserId,
            @Param("status") Integer status,
            @Param("yesterdayStart") LocalDateTime yesterdayStart,
            @Param("todayStart") LocalDateTime todayStart,
            @Param("settledStatus") String settledStatus,
            @Param("runningStatus") String runningStatus,
            @Param("pausedStatus") String pausedStatus,
            @Param("sortBy") String sortBy,
            @Param("limit") long limit,
            @Param("offset") long offset
    );

    @Select("""
            SELECT
              team_user.id AS user_id,
              team_user.user_name AS user_name,
              team_user.email AS email,
              team_user.avatar_key AS avatar_key,
              team_user.status AS user_status,
              COALESCE(team_count.direct_count, 0) AS direct_count,
              COALESCE(team_count.indirect_count, 0) AS indirect_count,
              COALESCE(team_count.total_team_count, 0) AS total_team_count,
              COALESCE(commission.yesterday_commission, 0) AS yesterday_commission,
              COALESCE(commission.total_commission, 0) AS total_commission,
              COALESCE(team_order.active_order_count, 0) AS active_order_count,
              COALESCE(team_order.running_order_count, 0) AS running_order_count,
              commission.last_commission_at AS last_commission_at
            FROM app_user team_user
            LEFT JOIN (
              SELECT
                relation.ancestor_user_id AS user_id,
                COUNT(CASE WHEN relation.level_depth = 1 THEN 1 END) AS direct_count,
                COUNT(CASE WHEN relation.level_depth = 2 THEN 1 END) AS indirect_count,
                COUNT(*) AS total_team_count
              FROM user_team_relation relation
              WHERE relation.ancestor_user_id = #{userId}
                AND relation.level_depth <= #{maxLevel}
              GROUP BY relation.ancestor_user_id
            ) team_count ON team_count.user_id = team_user.id
            LEFT JOIN (
              SELECT
                record.benefit_user_id AS user_id,
                SUM(CASE
                    WHEN record.settled_at >= #{yesterdayStart}
                     AND record.settled_at < #{todayStart}
                    THEN record.commission_amount
                    ELSE 0
                END) AS yesterday_commission,
                SUM(record.commission_amount) AS total_commission,
                MAX(record.settled_at) AS last_commission_at
              FROM commission_record record
              WHERE record.benefit_user_id = #{userId}
                AND record.status = #{settledStatus}
                AND record.level_no <= #{maxLevel}
              GROUP BY record.benefit_user_id
            ) commission ON commission.user_id = team_user.id
            LEFT JOIN (
              SELECT
                relation.ancestor_user_id AS user_id,
                COUNT(`order`.id) AS active_order_count,
                COUNT(CASE WHEN `order`.order_status = #{runningStatus} THEN 1 END) AS running_order_count
              FROM user_team_relation relation
              JOIN rental_order `order` ON `order`.user_id = relation.descendant_user_id
              WHERE relation.ancestor_user_id = #{userId}
                AND relation.level_depth <= #{maxLevel}
                AND `order`.order_status IN (#{runningStatus}, #{pausedStatus})
              GROUP BY relation.ancestor_user_id
            ) team_order ON team_order.user_id = team_user.id
            WHERE team_user.id = #{userId}
            LIMIT 1
            """)
    AdminTeamAggregateRow selectAdminTeamAggregateByUserId(
            @Param("userId") Long userId,
            @Param("maxLevel") int maxLevel,
            @Param("yesterdayStart") LocalDateTime yesterdayStart,
            @Param("todayStart") LocalDateTime todayStart,
            @Param("settledStatus") String settledStatus,
            @Param("runningStatus") String runningStatus,
            @Param("pausedStatus") String pausedStatus
    );
}

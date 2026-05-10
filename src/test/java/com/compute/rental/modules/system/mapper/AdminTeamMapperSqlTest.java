package com.compute.rental.modules.system.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.compute.rental.modules.commission.mapper.CommissionRecordMapper;
import com.compute.rental.modules.user.mapper.UserTeamRelationMapper;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class AdminTeamMapperSqlTest {

    @Test
    void adminTeamAggregatePageSqlShouldBeParseable() {
        var configuration = new Configuration();
        configuration.addMapper(UserTeamRelationMapper.class);
        var parameters = new HashMap<String, Object>();
        parameters.put("maxLevel", 2);
        parameters.put("keyword", "alice");
        parameters.put("status", 1);
        parameters.put("yesterdayStart", LocalDateTime.now().minusDays(1));
        parameters.put("todayStart", LocalDateTime.now());
        parameters.put("settledStatus", "SETTLED");
        parameters.put("runningStatus", "RUNNING");
        parameters.put("pausedStatus", "PAUSED");
        parameters.put("sortBy", "totalCommission");
        parameters.put("limit", 20L);
        parameters.put("offset", 0L);

        var boundSql = configuration
                .getMappedStatement(UserTeamRelationMapper.class.getName() + ".selectAdminTeamAggregatePage")
                .getBoundSql(parameters);

        assertThat(boundSql.getSql())
                .contains("FROM user_team_relation")
                .contains("team_user.user_name LIKE")
                .contains("team_user.email LIKE")
                .contains("ORDER BY")
                .contains("LIMIT ? OFFSET ?")
                .doesNotContain("team_user.user_id =")
                .doesNotContain("OR team_user.id =")
                .doesNotContain("&lt;");
    }

    @Test
    void commissionAggregateSqlShouldBeParseable() {
        var configuration = new Configuration();
        configuration.addMapper(CommissionRecordMapper.class);
        var parameters = new HashMap<String, Object>();
        parameters.put("statuses", List.of("PENDING", "SETTLED"));
        parameters.put("maxLevelNo", 2);
        parameters.put("startAt", LocalDateTime.now().minusDays(1));
        parameters.put("endAt", LocalDateTime.now());

        var boundSql = configuration
                .getMappedStatement(CommissionRecordMapper.class.getName()
                        + ".sumCommissionAmountByStatusesAndCreatedAtRange")
                .getBoundSql(parameters);

        assertThat(boundSql.getSql())
                .contains("status IN")
                .contains("created_at >=")
                .contains("created_at <")
                .doesNotContain("&lt;");
    }
}

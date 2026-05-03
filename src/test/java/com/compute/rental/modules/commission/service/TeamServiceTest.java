package com.compute.rental.modules.commission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.compute.rental.modules.user.entity.AppUser;
import com.compute.rental.modules.user.entity.UserTeamRelation;
import com.compute.rental.modules.user.mapper.AppUserMapper;
import com.compute.rental.modules.user.mapper.UserTeamRelationMapper;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private UserTeamRelationMapper teamRelationMapper;

    @Mock
    private AppUserMapper appUserMapper;

    @InjectMocks
    private TeamService teamService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        var configuration = new Configuration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), UserTeamRelation.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), AppUser.class);
    }

    @Test
    void teamSummaryShouldCountDeeperMembersWithoutCommissionDepthLimit() {
        when(teamRelationMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                relation(1),
                relation(2),
                relation(3),
                relation(4),
                relation(8)
        ));

        var summary = teamService.summary(10L);

        assertThat(summary.totalTeamCount()).isEqualTo(5);
        assertThat(summary.directTeamCount()).isEqualTo(1);
        assertThat(summary.level2TeamCount()).isEqualTo(1);
        assertThat(summary.level3TeamCount()).isEqualTo(1);
        assertThat(summary.deeperTeamCount()).isEqualTo(2);
    }

    private UserTeamRelation relation(Integer levelDepth) {
        var relation = new UserTeamRelation();
        relation.setAncestorUserId(10L);
        relation.setDescendantUserId(100L + levelDepth);
        relation.setLevelDepth(levelDepth);
        return relation;
    }
}

package com.compute.rental.modules.commission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.compute.rental.modules.commission.dto.TeamMemberQueryRequest;
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
    void teamSummaryShouldOnlyCountTwoLevelMembers() {
        when(teamRelationMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                relation(1),
                relation(2)
        ));

        var summary = teamService.summary(10L);

        assertThat(summary.totalTeamCount()).isEqualTo(2);
        assertThat(summary.directTeamCount()).isEqualTo(1);
        assertThat(summary.level2TeamCount()).isEqualTo(1);
    }

    @Test
    void pageMembersShouldReturnAvatarKey() {
        var relation = relation(1);
        relation.setDescendantUserId(100L);
        var page = new Page<UserTeamRelation>(1, 10);
        page.setRecords(List.of(relation));
        page.setTotal(1);
        when(teamRelationMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);
        when(appUserMapper.selectBatchIds(List.of(100L))).thenReturn(List.of(user(100L, "avatar-001")));
        when(teamRelationMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        var result = teamService.pageMembers(10L, new TeamMemberQueryRequest(1, 10, null, null));

        assertThat(result.records()).hasSize(1);
        assertThat(result.records().get(0).avatarKey()).isEqualTo("avatar-001");
    }

    private UserTeamRelation relation(Integer levelDepth) {
        var relation = new UserTeamRelation();
        relation.setAncestorUserId(10L);
        relation.setDescendantUserId(100L + levelDepth);
        relation.setLevelDepth(levelDepth);
        return relation;
    }

    private AppUser user(Long id, String avatarKey) {
        var user = new AppUser();
        user.setId(id);
        user.setUserId("U" + id);
        user.setUserName("user-" + id);
        user.setAvatarKey(avatarKey);
        user.setStatus(1);
        return user;
    }
}

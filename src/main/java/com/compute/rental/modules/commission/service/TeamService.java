package com.compute.rental.modules.commission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.modules.commission.dto.TeamMemberQueryRequest;
import com.compute.rental.modules.commission.dto.TeamMemberResponse;
import com.compute.rental.modules.commission.dto.TeamSummaryResponse;
import com.compute.rental.modules.user.entity.AppUser;
import com.compute.rental.modules.user.entity.UserTeamRelation;
import com.compute.rental.modules.user.mapper.AppUserMapper;
import com.compute.rental.modules.user.mapper.UserTeamRelationMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TeamService {

    private final UserTeamRelationMapper teamRelationMapper;
    private final AppUserMapper appUserMapper;

    public TeamService(UserTeamRelationMapper teamRelationMapper, AppUserMapper appUserMapper) {
        this.teamRelationMapper = teamRelationMapper;
        this.appUserMapper = appUserMapper;
    }

    public TeamSummaryResponse summary(Long userId) {
        var relations = teamRelationMapper.selectList(new LambdaQueryWrapper<UserTeamRelation>()
                .eq(UserTeamRelation::getAncestorUserId, userId));
        return new TeamSummaryResponse(
                relations.size(),
                countDepth(relations, 1),
                countDepth(relations, 2),
                countDepth(relations, 3),
                relations.stream().filter(relation -> relation.getLevelDepth() != null && relation.getLevelDepth() > 3).count()
        );
    }

    public PageResult<TeamMemberResponse> pageMembers(Long userId, TeamMemberQueryRequest request) {
        var page = new Page<UserTeamRelation>(request.current(), request.size());
        var wrapper = new LambdaQueryWrapper<UserTeamRelation>()
                .eq(UserTeamRelation::getAncestorUserId, userId)
                .eq(request.levelDepth() != null, UserTeamRelation::getLevelDepth, request.levelDepth())
                .orderByAsc(UserTeamRelation::getLevelDepth)
                .orderByDesc(UserTeamRelation::getId);
        var result = teamRelationMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getRecords().stream().map(this::toMemberResponse).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    private long countDepth(List<UserTeamRelation> relations, int depth) {
        return relations.stream()
                .filter(relation -> Integer.valueOf(depth).equals(relation.getLevelDepth()))
                .count();
    }

    private TeamMemberResponse toMemberResponse(UserTeamRelation relation) {
        var user = appUserMapper.selectById(relation.getDescendantUserId());
        if (user == null) {
            return new TeamMemberResponse(null, null, null, null, relation.getLevelDepth(), relation.getCreatedAt());
        }
        return toMemberResponse(user, relation);
    }

    private TeamMemberResponse toMemberResponse(AppUser user, UserTeamRelation relation) {
        return new TeamMemberResponse(
                user.getUserId(),
                user.getUserName(),
                user.getEmail(),
                user.getStatus(),
                relation.getLevelDepth(),
                relation.getCreatedAt()
        );
    }
}

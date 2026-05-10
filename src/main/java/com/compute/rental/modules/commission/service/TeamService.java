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
import com.compute.rental.modules.user.support.AppUserSearchSupport;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class TeamService {

    private static final int MAX_TEAM_DEPTH = 2;

    private final UserTeamRelationMapper teamRelationMapper;
    private final AppUserMapper appUserMapper;

    public TeamService(UserTeamRelationMapper teamRelationMapper, AppUserMapper appUserMapper) {
        this.teamRelationMapper = teamRelationMapper;
        this.appUserMapper = appUserMapper;
    }

    public TeamSummaryResponse summary(Long userId) {
        var relations = teamRelationMapper.selectList(new LambdaQueryWrapper<UserTeamRelation>()
                .eq(UserTeamRelation::getAncestorUserId, userId)
                .le(UserTeamRelation::getLevelDepth, MAX_TEAM_DEPTH));
        return new TeamSummaryResponse(
                relations.size(),
                countDepth(relations, 1),
                countDepth(relations, 2)
        );
    }

    public PageResult<TeamMemberResponse> pageMembers(Long userId, TeamMemberQueryRequest request) {
        if (request.levelDepth() != null && request.levelDepth() > MAX_TEAM_DEPTH) {
            return new PageResult<>(List.of(), 0, request.current(), request.size());
        }
        var matchedUserIds = findMatchedUserIds(request.normalizedKeyword());
        if (matchedUserIds != null && matchedUserIds.isEmpty()) {
            return new PageResult<>(List.of(), 0, request.current(), request.size());
        }

        var page = new Page<UserTeamRelation>(request.current(), request.size());
        var wrapper = new LambdaQueryWrapper<UserTeamRelation>()
                .eq(UserTeamRelation::getAncestorUserId, userId)
                .le(UserTeamRelation::getLevelDepth, MAX_TEAM_DEPTH)
                .eq(request.levelDepth() != null, UserTeamRelation::getLevelDepth, request.levelDepth())
                .in(matchedUserIds != null, UserTeamRelation::getDescendantUserId, matchedUserIds)
                .orderByAsc(UserTeamRelation::getLevelDepth)
                .orderByDesc(UserTeamRelation::getId);
        var result = teamRelationMapper.selectPage(page, wrapper);
        return new PageResult<>(toMemberResponses(result.getRecords()),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    private List<Long> findMatchedUserIds(String keyword) {
        var normalizedKeyword = AppUserSearchSupport.normalize(keyword);
        if (!AppUserSearchSupport.hasText(normalizedKeyword)) {
            return null;
        }
        return appUserMapper.selectList(AppUserSearchSupport.idQuery(normalizedKeyword))
                .stream()
                .map(AppUser::getId)
                .toList();
    }

    private long countDepth(List<UserTeamRelation> relations, int depth) {
        return relations.stream()
                .filter(relation -> Integer.valueOf(depth).equals(relation.getLevelDepth()))
                .count();
    }

    private List<TeamMemberResponse> toMemberResponses(List<UserTeamRelation> relations) {
        if (relations.isEmpty()) {
            return List.of();
        }

        var descendantIds = relations.stream()
                .map(UserTeamRelation::getDescendantUserId)
                .distinct()
                .toList();
        var usersById = appUserMapper.selectBatchIds(descendantIds).stream()
                .collect(Collectors.toMap(AppUser::getId, Function.identity()));
        var subTeamCounts = teamRelationMapper.selectList(new LambdaQueryWrapper<UserTeamRelation>()
                        .in(UserTeamRelation::getAncestorUserId, descendantIds)
                        .le(UserTeamRelation::getLevelDepth, MAX_TEAM_DEPTH))
                .stream()
                .collect(Collectors.groupingBy(UserTeamRelation::getAncestorUserId, Collectors.counting()));
        var parentInternalIdsByDescendant = teamRelationMapper.selectList(new LambdaQueryWrapper<UserTeamRelation>()
                        .in(UserTeamRelation::getDescendantUserId, descendantIds)
                        .eq(UserTeamRelation::getLevelDepth, 1))
                .stream()
                .collect(Collectors.toMap(
                        UserTeamRelation::getDescendantUserId,
                        UserTeamRelation::getAncestorUserId,
                        (first, ignored) -> first));
        var parentIds = parentInternalIdsByDescendant.values().stream().distinct().toList();
        var parentPublicIdsByInternalId = parentIds.isEmpty()
                ? java.util.Map.<Long, String>of()
                : appUserMapper.selectBatchIds(parentIds).stream()
                        .collect(Collectors.toMap(AppUser::getId, AppUser::getUserId));

        return relations.stream()
                .map(relation -> {
                    var descendantId = relation.getDescendantUserId();
                    var parentInternalId = parentInternalIdsByDescendant.get(descendantId);
                    var parentId = parentInternalId == null ? null : parentPublicIdsByInternalId.get(parentInternalId);
                    return toMemberResponse(
                            usersById.get(descendantId),
                            relation,
                            subTeamCounts.getOrDefault(descendantId, 0L),
                            parentId);
                })
                .toList();
    }

    private TeamMemberResponse toMemberResponse(AppUser user, UserTeamRelation relation, Long subTeamCount, String parentId) {
        if (user == null) {
            return new TeamMemberResponse(null, null, null, null, relation.getLevelDepth(), relation.getCreatedAt(),
                    subTeamCount, parentId);
        }
        return new TeamMemberResponse(
                user.getUserId(),
                user.getUserName(),
                user.getAvatarKey(),
                user.getStatus(),
                relation.getLevelDepth(),
                relation.getCreatedAt(),
                subTeamCount,
                parentId
        );
    }
}

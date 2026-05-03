package com.compute.rental.modules.user.service;

import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.common.util.DateTimeUtils;
import com.compute.rental.modules.user.dto.UpdateAvatarRequest;
import com.compute.rental.modules.user.dto.UserMeResponse;
import com.compute.rental.modules.user.entity.AppUser;
import com.compute.rental.modules.user.mapper.AppUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FrontUserService {

    private final AppUserMapper appUserMapper;

    public FrontUserService(AppUserMapper appUserMapper) {
        this.appUserMapper = appUserMapper;
    }

    public UserMeResponse getMe(Long id) {
        var user = requireUser(id);
        return new UserMeResponse(
                user.getId(),
                user.getUserId(),
                user.getEmail(),
                user.getUserName(),
                user.getAvatarKey(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }

    @Transactional
    public UserMeResponse updateAvatar(Long id, UpdateAvatarRequest request) {
        var user = requireUser(id);
        user.setAvatarKey(request.avatarKey().trim());
        user.setUpdatedAt(DateTimeUtils.now());
        appUserMapper.updateById(user);
        return getMe(id);
    }

    private AppUser requireUser(Long id) {
        var user = appUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }
}

package com.compute.rental.modules.user.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.compute.rental.modules.user.entity.AppUser;
import org.springframework.util.StringUtils;

public final class AppUserSearchSupport {

    private AppUserSearchSupport() {
    }

    public static String normalize(String keyword) {
        return keyword == null ? null : keyword.trim();
    }

    public static boolean hasText(String keyword) {
        return StringUtils.hasText(keyword);
    }

    public static LambdaQueryWrapper<AppUser> idQuery(String keyword) {
        return new LambdaQueryWrapper<AppUser>()
                .select(AppUser::getId)
                .and(wrapper -> applyKeyword(wrapper, keyword));
    }

    public static LambdaQueryWrapper<AppUser> applyKeyword(
            LambdaQueryWrapper<AppUser> wrapper,
            String keyword
    ) {
        var normalized = normalize(keyword);
        return wrapper
                .likeRight(AppUser::getUserName, normalized)
                .or()
                .likeRight(AppUser::getEmail, normalized);
    }
}

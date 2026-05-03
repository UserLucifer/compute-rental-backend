package com.compute.rental.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.compute.rental.modules.user.entity.AppUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AppUserMapper extends BaseMapper<AppUser> {
}

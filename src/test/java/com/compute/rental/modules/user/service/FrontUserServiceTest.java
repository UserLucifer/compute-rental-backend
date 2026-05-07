package com.compute.rental.modules.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.compute.rental.modules.user.entity.AppUser;
import com.compute.rental.modules.user.entity.UserReferralRelation;
import com.compute.rental.modules.user.mapper.AppUserMapper;
import com.compute.rental.modules.user.mapper.UserReferralRelationMapper;
import java.time.LocalDateTime;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FrontUserServiceTest {

    @Mock
    private AppUserMapper appUserMapper;

    @Mock
    private UserReferralRelationMapper userReferralRelationMapper;

    @InjectMocks
    private FrontUserService frontUserService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        var configuration = new Configuration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), AppUser.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), UserReferralRelation.class);
    }

    @Test
    void getMeShouldReturnReferralInviteCode() {
        when(appUserMapper.selectById(10L)).thenReturn(user());
        when(userReferralRelationMapper.selectOne(any(Wrapper.class))).thenReturn(referral());

        var response = frontUserService.getMe(10L);

        assertThat(response.userId()).isEqualTo("U001");
        assertThat(response.inviteCode()).isEqualTo("ABC123");
    }

    private AppUser user() {
        var user = new AppUser();
        user.setId(10L);
        user.setUserId("U001");
        user.setEmail("test@example.com");
        user.setUserName("tester");
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    private UserReferralRelation referral() {
        var referral = new UserReferralRelation();
        referral.setUserId(10L);
        referral.setInviteCode("ABC123");
        return referral;
    }
}

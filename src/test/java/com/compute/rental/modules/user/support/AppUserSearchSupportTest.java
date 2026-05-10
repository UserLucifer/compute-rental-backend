package com.compute.rental.modules.user.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.compute.rental.modules.user.entity.AppUser;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AppUserSearchSupportTest {

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        var configuration = new Configuration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), AppUser.class);
    }

    @Test
    void keywordSearchOnlyMatchesUserNameAndEmail() {
        var sqlSegment = AppUserSearchSupport.idQuery("alice").getSqlSegment();

        assertThat(sqlSegment)
                .contains("user_name")
                .contains("email")
                .doesNotContain("user_id");
    }
}

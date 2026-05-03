package com.compute.rental.modules.user.entity;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@Getter
@Setter
@TableName("user_team_relation")
public class UserTeamRelation {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("ancestor_user_id")
    private Long ancestorUserId;

    @TableField(exist = false)
    private String ancestorUserName;

    @TableField("descendant_user_id")
    private Long descendantUserId;

    @TableField(exist = false)
    private String descendantUserName;

    @TableField("level_depth")
    private Integer levelDepth;

    @TableField("created_at")
    private LocalDateTime createdAt;
}

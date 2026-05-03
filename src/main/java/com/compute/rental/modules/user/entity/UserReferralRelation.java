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
@TableName("user_referral_relation")
public class UserReferralRelation {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("invite_code")
    private String inviteCode;

    @TableField("parent_user_id")
    private Long parentUserId;

    @TableField("parent_invite_code")
    private String parentInviteCode;

    @TableField("level1_user_id")
    private Long level1UserId;

    @TableField("level2_user_id")
    private Long level2UserId;

    @TableField("level3_user_id")
    private Long level3UserId;

    @TableField("created_at")
    private LocalDateTime createdAt;

}

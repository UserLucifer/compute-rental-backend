package com.compute.rental.modules.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("app_user")
public class AppUser {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private String userId;

    @TableField("email")
    private String email;

    @TableField("password_hash")
    private String passwordHash;

    @TableField("user_name")
    private String userName;

    @TableField("avatar_key")
    private String avatarKey;

    @TableField("status")
    private Integer status;

    @TableField("email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @TableField("last_login_at")
    private LocalDateTime lastLoginAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

}

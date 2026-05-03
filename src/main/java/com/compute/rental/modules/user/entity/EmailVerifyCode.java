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
@TableName("email_verify_code")
public class EmailVerifyCode {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("email")
    private String email;

    @TableField("scene")
    private String scene;

    @TableField("code_hash")
    private String codeHash;

    @TableField("send_ip")
    private String sendIp;

    @TableField("expire_at")
    private LocalDateTime expireAt;

    @TableField("used_at")
    private LocalDateTime usedAt;

    @TableField("status")
    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;

}

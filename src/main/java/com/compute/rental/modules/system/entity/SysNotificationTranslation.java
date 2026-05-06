package com.compute.rental.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sys_notification_translation")
public class SysNotificationTranslation {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("notification_id")
    private Long notificationId;

    @TableField("locale")
    private String locale;

    @TableField("title")
    private String title;

    @TableField("content")
    private String content;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

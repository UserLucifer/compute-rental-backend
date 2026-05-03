package com.compute.rental.modules.system.entity;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@Getter
@Setter
@TableName("sys_admin_log")
public class SysAdminLog {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("admin_id")
    private Long adminId;

    @TableField(exist = false)
    private String operatorName;

    @TableField("action")
    private String action;

    @TableField(exist = false)
    private String actionName;

    @TableField("target_table")
    private String targetTable;

    @TableField("target_id")
    private Long targetId;

    @TableField("before_value")
    private String beforeValue;

    @TableField("after_value")
    private String afterValue;

    @TableField("remark")
    private String remark;

    @TableField("ip")
    private String ip;

    @TableField("created_at")
    private LocalDateTime createdAt;

}

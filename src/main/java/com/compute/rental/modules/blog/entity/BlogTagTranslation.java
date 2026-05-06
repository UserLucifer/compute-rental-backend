package com.compute.rental.modules.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("blog_tag_translation")
public class BlogTagTranslation {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("tag_id")
    private Long tagId;

    @TableField("locale")
    private String locale;

    @TableField("tag_name")
    private String tagName;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

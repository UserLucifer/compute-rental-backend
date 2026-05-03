package com.compute.rental.modules.blog.entity;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@Getter
@Setter
@TableName("blog_post_tag")
public class BlogPostTag {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("post_id")
    private Long postId;

    @TableField("tag_id")
    private Long tagId;

    @TableField("created_at")
    private LocalDateTime createdAt;
}

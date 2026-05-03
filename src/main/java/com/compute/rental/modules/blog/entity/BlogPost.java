package com.compute.rental.modules.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("blog_post")
public class BlogPost {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("category_id")
    private Long categoryId;

    @TableField("title")
    private String title;

    @TableField("summary")
    private String summary;

    @TableField("cover_image_url")
    private String coverImageUrl;

    @TableField("content_markdown")
    private String contentMarkdown;

    @TableField("publish_status")
    private Integer publishStatus;

    @TableField("published_at")
    private LocalDateTime publishedAt;

    @TableField("is_top")
    private Integer isTop;

    @TableField("sort_no")
    private Integer sortNo;

    @TableField("view_count")
    private Long viewCount;

    @TableField("created_by")
    private Long createdBy;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

}

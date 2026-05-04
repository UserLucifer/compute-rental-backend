package com.compute.rental.modules.docs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("doc_article")
public class DocArticle {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("category_id")
    private Long categoryId;

    @TableField("language")
    private String language;

    @TableField("section")
    private String section;

    @TableField("title")
    private String title;

    @TableField("slug")
    private String slug;

    @TableField("summary")
    private String summary;

    @TableField("content_markdown")
    private String contentMarkdown;

    @TableField("publish_status")
    private Integer publishStatus;

    @TableField("is_section_home")
    private Integer isSectionHome;

    @TableField("published_at")
    private LocalDateTime publishedAt;

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

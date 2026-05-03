-- Minimal public documentation center schema.
-- Supports top navigation, nested sidebar categories, article CRUD, publish status, and keyword search.
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `doc_category` (
  `id`            BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id`     BIGINT DEFAULT NULL COMMENT '父级分类ID，NULL表示一级分类',
  `category_code` VARCHAR(64) NOT NULL COMMENT '分类编码，用于前端路由标识',
  `category_name` VARCHAR(64) NOT NULL COMMENT '分类名称',
  `icon`          VARCHAR(64) DEFAULT NULL COMMENT '分类图标标识',
  `sort_no`       INT NOT NULL DEFAULT 0 COMMENT '排序号',
  `status`        TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-停用',
  `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_code` (`category_code`),
  KEY `idx_parent_status_sort` (`parent_id`, `status`, `sort_no`),
  KEY `idx_status_sort` (`status`, `sort_no`),
  CONSTRAINT `fk_doc_category_parent` FOREIGN KEY (`parent_id`) REFERENCES `doc_category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档分类表';

CREATE TABLE IF NOT EXISTS `doc_article` (
  `id`               BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `category_id`      BIGINT NOT NULL COMMENT '所属分类ID',
  `title`            VARCHAR(255) NOT NULL COMMENT '文档标题',
  `slug`             VARCHAR(128) NOT NULL COMMENT '文档路由标识',
  `summary`          VARCHAR(500) DEFAULT NULL COMMENT '文档摘要',
  `content_markdown` LONGTEXT NOT NULL COMMENT '文档正文Markdown内容',
  `publish_status`   TINYINT NOT NULL DEFAULT 0 COMMENT '发布状态：0-草稿，1-已发布，2-已下线',
  `published_at`     DATETIME DEFAULT NULL COMMENT '发布时间',
  `sort_no`          INT NOT NULL DEFAULT 0 COMMENT '排序号',
  `view_count`       BIGINT NOT NULL DEFAULT 0 COMMENT '浏览量',
  `created_by`       BIGINT DEFAULT NULL COMMENT '创建人管理员ID',
  `created_at`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_slug` (`slug`),
  KEY `idx_category_status_sort` (`category_id`, `publish_status`, `sort_no`),
  KEY `idx_publish_status` (`publish_status`),
  KEY `idx_published_at` (`published_at`),
  CONSTRAINT `fk_doc_article_category` FOREIGN KEY (`category_id`) REFERENCES `doc_category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档文章表';

SET FOREIGN_KEY_CHECKS = 1;

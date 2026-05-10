-- Run segment migration for minute-based profit settlement.
-- The table records only real RUNNING periods. Daily profit and final settlement
-- aggregate complete minutes from these segments, so PAUSED time consumes rental
-- duration but does not produce profit.

CREATE TABLE IF NOT EXISTS `rental_order_run_segment` (
  `id`                BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rental_order_id`   BIGINT NOT NULL COMMENT '租赁订单ID',
  `user_id`           BIGINT NOT NULL COMMENT '用户ID',
  `segment_start_at`  DATETIME NOT NULL COMMENT '本段运行开始时间',
  `segment_end_at`    DATETIME DEFAULT NULL COMMENT '本段运行结束时间，NULL表示当前正在运行',
  `close_reason`      VARCHAR(32) DEFAULT NULL COMMENT '关闭原因：AUTO_PAUSE/ADMIN_DISABLE/EXPIRE/EARLY_SETTLE',
  `created_at`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_open` (`rental_order_id`, `segment_end_at`),
  KEY `idx_profit_window` (`segment_start_at`, `segment_end_at`),
  KEY `idx_user_start` (`user_id`, `segment_start_at`),
  CONSTRAINT `fk_run_segment_order` FOREIGN KEY (`rental_order_id`) REFERENCES `rental_order` (`id`),
  CONSTRAINT `fk_run_segment_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租赁订单运行片段表';

INSERT INTO `rental_order_run_segment` (
  `rental_order_id`,
  `user_id`,
  `segment_start_at`,
  `segment_end_at`,
  `close_reason`,
  `created_at`,
  `updated_at`
)
SELECT
  ro.`id`,
  ro.`user_id`,
  ro.`profit_start_at`,
  CASE
    WHEN ro.`order_status` = 'RUNNING' THEN NULL
    WHEN ro.`profit_end_at` IS NULL THEN COALESCE(ro.`paused_at`, ro.`updated_at`, ro.`profit_start_at`)
    ELSE LEAST(COALESCE(ro.`paused_at`, ro.`updated_at`, ro.`profit_start_at`), ro.`profit_end_at`)
  END,
  CASE
    WHEN ro.`order_status` = 'RUNNING' THEN NULL
    ELSE 'AUTO_PAUSE'
  END,
  ro.`profit_start_at`,
  CASE
    WHEN ro.`order_status` = 'RUNNING' THEN ro.`profit_start_at`
    WHEN ro.`profit_end_at` IS NULL THEN COALESCE(ro.`paused_at`, ro.`updated_at`, ro.`profit_start_at`)
    ELSE LEAST(COALESCE(ro.`paused_at`, ro.`updated_at`, ro.`profit_start_at`), ro.`profit_end_at`)
  END
FROM `rental_order` ro
WHERE ro.`profit_start_at` IS NOT NULL
  AND ro.`order_status` IN ('RUNNING', 'PAUSED')
  AND NOT EXISTS (
    SELECT 1
    FROM `rental_order_run_segment` existing_segment
    WHERE existing_segment.`rental_order_id` = ro.`id`
  );

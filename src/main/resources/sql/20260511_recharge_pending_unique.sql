-- Enforce at most one submitted recharge order per user.
-- Before running this migration, resolve duplicate SUBMITTED orders returned by the diagnostic query.

SELECT `user_id`, COUNT(*) AS submitted_count
FROM `recharge_order`
WHERE `status` = 'SUBMITTED'
GROUP BY `user_id`
HAVING COUNT(*) > 1;

SET @add_submitted_user_id_column = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'recharge_order'
        AND COLUMN_NAME = 'submitted_user_id'
    ),
    'SELECT 1',
    'ALTER TABLE `recharge_order` ADD COLUMN `submitted_user_id` BIGINT GENERATED ALWAYS AS (CASE WHEN `status` = ''SUBMITTED'' THEN `user_id` ELSE NULL END) STORED COMMENT ''待审核充值唯一约束辅助列'' AFTER `status`'
  )
);
PREPARE stmt FROM @add_submitted_user_id_column;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_submitted_user_id_unique = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'recharge_order'
        AND INDEX_NAME = 'uk_recharge_submitted_user'
    ),
    'SELECT 1',
    'ALTER TABLE `recharge_order` ADD UNIQUE KEY `uk_recharge_submitted_user` (`submitted_user_id`) COMMENT ''同一用户只允许存在一笔待审核充值'''
  )
);
PREPARE stmt FROM @add_submitted_user_id_unique;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

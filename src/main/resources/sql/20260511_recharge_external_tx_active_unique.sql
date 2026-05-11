-- Allow canceled/rejected recharge orders to reuse the same external transaction number,
-- while keeping SUBMITTED/APPROVED orders unique by external_tx_no.
-- Safe to run repeatedly on MySQL 8.

SELECT `external_tx_no`, COUNT(*) AS active_count
FROM `recharge_order`
WHERE `status` IN ('SUBMITTED', 'APPROVED')
  AND `external_tx_no` IS NOT NULL
  AND `external_tx_no` <> ''
GROUP BY `external_tx_no`
HAVING COUNT(*) > 1;

SET @drop_external_tx_no_unique = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'recharge_order'
        AND INDEX_NAME = 'uk_external_tx_no'
    ),
    'ALTER TABLE `recharge_order` DROP INDEX `uk_external_tx_no`',
    'SELECT 1'
  )
);
PREPARE stmt FROM @drop_external_tx_no_unique;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_active_external_tx_no_column = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'recharge_order'
        AND COLUMN_NAME = 'active_external_tx_no'
    ),
    'SELECT 1',
    'ALTER TABLE `recharge_order` ADD COLUMN `active_external_tx_no` VARCHAR(128) GENERATED ALWAYS AS (CASE WHEN `status` IN (''SUBMITTED'', ''APPROVED'') THEN `external_tx_no` ELSE NULL END) STORED COMMENT ''待审核/已到账交易哈希唯一约束辅助列'' AFTER `external_tx_no`'
  )
);
PREPARE stmt FROM @add_active_external_tx_no_column;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_active_external_tx_no_unique = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'recharge_order'
        AND INDEX_NAME = 'uk_recharge_active_external_tx_no'
    ),
    'SELECT 1',
    'ALTER TABLE `recharge_order` ADD UNIQUE KEY `uk_recharge_active_external_tx_no` (`active_external_tx_no`) COMMENT ''待审核/已到账充值单交易哈希不可重复'''
  )
);
PREPARE stmt FROM @add_active_external_tx_no_unique;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

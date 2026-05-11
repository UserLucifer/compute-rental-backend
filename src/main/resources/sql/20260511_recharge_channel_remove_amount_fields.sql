-- Move recharge amount and fee rules from recharge_channel to sys_config.
-- Safe to run repeatedly on MySQL 8.

INSERT INTO `sys_config` (`config_key`, `config_value`, `config_desc`) VALUES
('recharge.min_amount', '500', '全局最低充值金额 USDT')
ON DUPLICATE KEY UPDATE
  `config_desc` = VALUES(`config_desc`);

SET @drop_recharge_min_amount = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'recharge_channel'
        AND COLUMN_NAME = 'min_amount'
    ),
    'ALTER TABLE `recharge_channel` DROP COLUMN `min_amount`',
    'SELECT 1'
  )
);
PREPARE stmt FROM @drop_recharge_min_amount;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_recharge_max_amount = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'recharge_channel'
        AND COLUMN_NAME = 'max_amount'
    ),
    'ALTER TABLE `recharge_channel` DROP COLUMN `max_amount`',
    'SELECT 1'
  )
);
PREPARE stmt FROM @drop_recharge_max_amount;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_recharge_fee_rate = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'recharge_channel'
        AND COLUMN_NAME = 'fee_rate'
    ),
    'ALTER TABLE `recharge_channel` DROP COLUMN `fee_rate`',
    'SELECT 1'
  )
);
PREPARE stmt FROM @drop_recharge_fee_rate;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

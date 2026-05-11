-- User withdraw address book.
-- Safe to run repeatedly on MySQL 8.

CREATE TABLE IF NOT EXISTS `user_withdraw_address` (
  `id`              BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`         BIGINT NOT NULL COMMENT '用户ID',
  `network`         VARCHAR(64) NOT NULL COMMENT '链网络，如 TRC20/ERC20/BEP20',
  `account_name`    VARCHAR(64) DEFAULT NULL COMMENT '收款账户名称，可为空',
  `account_no`      VARCHAR(255) NOT NULL COMMENT '提现收款地址',
  `label`           VARCHAR(64) DEFAULT NULL COMMENT '用户自定义标签',
  `is_default`      TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认地址：1-是，0-否',
  `default_user_id` BIGINT GENERATED ALWAYS AS (CASE WHEN `is_default` = 1 THEN `user_id` ELSE NULL END) STORED COMMENT '默认地址唯一约束辅助列',
  `status`          TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-停用',
  `created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_withdraw_address` (`user_id`, `network`, `account_no`),
  UNIQUE KEY `uk_user_default_withdraw_address` (`default_user_id`),
  KEY `idx_user_status_default` (`user_id`, `status`, `is_default`),
  CONSTRAINT `fk_user_withdraw_address_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户提现地址簿';

SET @add_withdraw_address_id_column = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'withdraw_order'
        AND COLUMN_NAME = 'withdraw_address_id'
    ),
    'SELECT 1',
    'ALTER TABLE `withdraw_order` ADD COLUMN `withdraw_address_id` BIGINT DEFAULT NULL COMMENT ''用户提现地址簿ID快照'' AFTER `wallet_id`'
  )
);
PREPARE stmt FROM @add_withdraw_address_id_column;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

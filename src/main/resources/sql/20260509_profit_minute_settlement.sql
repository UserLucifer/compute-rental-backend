-- Profit minute settlement migration.
-- Adds audit fields for minute-based profit calculation and an index for frequent expire scans.

SET @schema_name = DATABASE();

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'rental_profit_record'
     AND COLUMN_NAME = 'effective_minutes') = 0,
  'ALTER TABLE `rental_profit_record`
     ADD COLUMN `effective_minutes` INT NOT NULL DEFAULT 1440 COMMENT ''本收益日有效运行完整分钟数，不足1分钟不计'' AFTER `profit_date`',
  'SELECT ''rental_profit_record.effective_minutes already exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'rental_profit_record'
     AND COLUMN_NAME = 'period_start_at') = 0,
  'ALTER TABLE `rental_profit_record`
     ADD COLUMN `period_start_at` DATETIME DEFAULT NULL COMMENT ''本次收益计算有效开始时间'' AFTER `effective_minutes`',
  'SELECT ''rental_profit_record.period_start_at already exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'rental_profit_record'
     AND COLUMN_NAME = 'period_end_at') = 0,
  'ALTER TABLE `rental_profit_record`
     ADD COLUMN `period_end_at` DATETIME DEFAULT NULL COMMENT ''本次收益计算有效结束时间'' AFTER `period_start_at`',
  'SELECT ''rental_profit_record.period_end_at already exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'rental_profit_record'
     AND INDEX_NAME = 'idx_profit_period') = 0,
  'ALTER TABLE `rental_profit_record`
     ADD KEY `idx_profit_period` (`period_start_at`, `period_end_at`)',
  'SELECT ''rental_profit_record.idx_profit_period already exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'rental_order'
     AND INDEX_NAME = 'idx_order_status_profit_end_at') = 0,
  'ALTER TABLE `rental_order`
     ADD KEY `idx_order_status_profit_end_at` (`order_status`, `profit_end_at`) COMMENT ''用于到期结算定时任务扫描''',
  'SELECT ''rental_order.idx_order_status_profit_end_at already exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE;

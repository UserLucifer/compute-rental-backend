-- User dashboard overview/search performance indexes.
-- Safe to run repeatedly; each statement checks information_schema before adding an index.

SET @schema_name = DATABASE();

SET @idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = @schema_name
      AND table_name = 'rental_profit_record'
      AND index_name = 'idx_user_status_profit_date'
);
SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE rental_profit_record ADD INDEX idx_user_status_profit_date (user_id, status, profit_date)',
    'SELECT ''idx_user_status_profit_date already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = @schema_name
      AND table_name = 'rental_order'
      AND index_name = 'idx_user_product_snapshot_id'
);
SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE rental_order ADD INDEX idx_user_product_snapshot_id (user_id, product_name_snapshot, id)',
    'SELECT ''idx_user_product_snapshot_id already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = @schema_name
      AND table_name = 'rental_order'
      AND index_name = 'idx_user_ai_model_snapshot_id'
);
SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE rental_order ADD INDEX idx_user_ai_model_snapshot_id (user_id, ai_model_name_snapshot, id)',
    'SELECT ''idx_user_ai_model_snapshot_id already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = @schema_name
      AND table_name = 'wallet_transaction'
      AND index_name = 'idx_user_tx_no_id'
);
SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE wallet_transaction ADD INDEX idx_user_tx_no_id (user_id, tx_no, id)',
    'SELECT ''idx_user_tx_no_id already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = @schema_name
      AND table_name = 'wallet_transaction'
      AND index_name = 'idx_user_biz_order_no_id'
);
SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE wallet_transaction ADD INDEX idx_user_biz_order_no_id (user_id, biz_order_no, id)',
    'SELECT ''idx_user_biz_order_no_id already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = @schema_name
      AND table_name = 'sys_notification'
      AND index_name = 'idx_user_title_id'
);
SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE sys_notification ADD INDEX idx_user_title_id (user_id, title, id)',
    'SELECT ''idx_user_title_id already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = @schema_name
      AND table_name = 'doc_article'
      AND index_name = 'idx_language_status_title_id'
);
SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE doc_article ADD INDEX idx_language_status_title_id (language, publish_status, title, id)',
    'SELECT ''idx_language_status_title_id already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

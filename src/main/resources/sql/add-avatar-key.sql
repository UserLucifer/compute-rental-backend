ALTER TABLE `app_user`
  ADD COLUMN `avatar_key` VARCHAR(64) DEFAULT NULL COMMENT 'avatar key' AFTER `user_name`;

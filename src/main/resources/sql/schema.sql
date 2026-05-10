-- Schema generated from 算力租赁平台_正式开发版_v1.1.md
-- MySQL 8 compatible table definitions.
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `app_user` (
  `id`                BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`           VARCHAR(64) NOT NULL COMMENT '用户编号，业务唯一标识',
  `email`             VARCHAR(128) NOT NULL COMMENT '邮箱，唯一',
  `password_hash`     VARCHAR(255) DEFAULT NULL COMMENT '登录密码哈希',
  `user_name`         VARCHAR(64) DEFAULT NULL COMMENT '用户名称',
  `status`            TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
  `email_verified_at` DATETIME DEFAULT NULL COMMENT '邮箱验证时间',
  `last_login_at`     DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `created_at`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `avatar_key`        VARCHAR(64) DEFAULT NULL COMMENT 'avatar key',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_user_name` (`user_name`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='前台用户表';

CREATE TABLE `email_verify_code` (
  `id`         BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `email`      VARCHAR(128) NOT NULL COMMENT '邮箱',
  `scene`      VARCHAR(32) NOT NULL DEFAULT 'SIGNUP' COMMENT '场景：SIGNUP-注册，RESET_PASSWORD-重置密码',
  `code_hash`  VARCHAR(255) NOT NULL COMMENT '验证码哈希',
  `send_ip`    VARCHAR(64) DEFAULT NULL COMMENT '发送IP',
  `expire_at`  DATETIME NOT NULL COMMENT '过期时间',
  `used_at`    DATETIME DEFAULT NULL COMMENT '使用时间',
  `status`     TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-未使用，1-已使用，2-已过期',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_email_scene` (`email`, `scene`),
  KEY `idx_expire_at` (`expire_at`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邮箱验证码表';

CREATE TABLE `user_referral_relation` (
  `id`                 BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`            BIGINT NOT NULL COMMENT '当前用户ID',
  `invite_code`        VARCHAR(32) NOT NULL COMMENT '当前用户自己的邀请码',
  `parent_user_id`     BIGINT DEFAULT NULL COMMENT '直属上级用户ID',
  `parent_invite_code` VARCHAR(32) DEFAULT NULL COMMENT '注册时使用的邀请码',
  `level1_user_id`     BIGINT DEFAULT NULL COMMENT '一级返佣上级（直属上级），收益时拿20%',
  `level2_user_id`     BIGINT DEFAULT NULL COMMENT '二级返佣上级，收益时拿10%',
  `created_at`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  UNIQUE KEY `uk_invite_code` (`invite_code`),
  KEY `idx_parent_user_id` (`parent_user_id`),
  KEY `idx_level1_user_id` (`level1_user_id`),
  KEY `idx_level2_user_id` (`level2_user_id`),
  CONSTRAINT `fk_referral_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户推荐关系表';

CREATE TABLE `user_team_relation` (
  `id`                  BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ancestor_user_id`    BIGINT NOT NULL COMMENT '上级用户ID',
  `descendant_user_id`  BIGINT NOT NULL COMMENT '下级用户ID',
  `level_depth`         INT NOT NULL COMMENT '相对层级：1-直属，2-二级',
  `created_at`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ancestor_descendant` (`ancestor_user_id`, `descendant_user_id`),
  KEY `idx_ancestor_depth` (`ancestor_user_id`, `level_depth`),
  KEY `idx_descendant` (`descendant_user_id`),
  CONSTRAINT `fk_team_ancestor` FOREIGN KEY (`ancestor_user_id`) REFERENCES `app_user` (`id`),
  CONSTRAINT `fk_team_descendant` FOREIGN KEY (`descendant_user_id`) REFERENCES `app_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户团队归属关系表';

CREATE TABLE `user_wallet` (
  `id`                BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `wallet_no`         VARCHAR(64) NOT NULL COMMENT '钱包编号',
  `user_id`           BIGINT NOT NULL COMMENT '用户ID',
  `currency`          VARCHAR(10) NOT NULL DEFAULT 'USDT' COMMENT '币种，固定USDT',
  `available_balance` DECIMAL(20,8) NOT NULL DEFAULT 0.00000000 COMMENT '可用余额',
  `frozen_balance`    DECIMAL(20,8) NOT NULL DEFAULT 0.00000000 COMMENT '冻结余额',
  `total_recharge`    DECIMAL(20,8) NOT NULL DEFAULT 0.00000000 COMMENT '累计充值成功金额',
  `total_withdraw`    DECIMAL(20,8) NOT NULL DEFAULT 0.00000000 COMMENT '累计提现成功金额',
  `total_profit`      DECIMAL(20,8) NOT NULL DEFAULT 0.00000000 COMMENT '累计租赁收益金额',
  `total_commission`  DECIMAL(20,8) NOT NULL DEFAULT 0.00000000 COMMENT '累计推广佣金金额',
  `status`            TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-正常，0-停用',
  `version_no`        INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号，所有余额变更必须带此字段',
  `created_at`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wallet_no` (`wallet_no`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_wallet_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户USDT钱包表';

CREATE TABLE `wallet_transaction` (
  `id`                       BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tx_no`                    VARCHAR(64) NOT NULL COMMENT '钱包流水号',
  `idempotency_key`          VARCHAR(128) DEFAULT NULL COMMENT '幂等键，防止重复入账或重复扣款，格式：{biz_type}:{biz_order_no}:{action}',
  `user_id`                  BIGINT NOT NULL COMMENT '用户ID',
  `wallet_id`                BIGINT NOT NULL COMMENT '钱包ID',
  `currency`                 VARCHAR(10) NOT NULL DEFAULT 'USDT' COMMENT '币种，固定USDT',
  `tx_type`                  VARCHAR(32) NOT NULL COMMENT '财务动作：IN-入账，OUT-支出，FREEZE-冻结，UNFREEZE-解冻',
  `amount`                   DECIMAL(20,8) NOT NULL COMMENT '本次变动金额，统一存正数',
  `before_available_balance` DECIMAL(20,8) NOT NULL COMMENT '变动前可用余额',
  `after_available_balance`  DECIMAL(20,8) NOT NULL COMMENT '变动后可用余额',
  `before_frozen_balance`    DECIMAL(20,8) NOT NULL DEFAULT 0.00000000 COMMENT '变动前冻结余额',
  `after_frozen_balance`     DECIMAL(20,8) NOT NULL DEFAULT 0.00000000 COMMENT '变动后冻结余额',
  `biz_type`                 VARCHAR(32) NOT NULL COMMENT '业务场景：RECHARGE/WITHDRAW/RENT_PAY/API_DEPLOY_FEE/RENT_PROFIT/COMMISSION_PROFIT/SETTLEMENT/EARLY_PENALTY/REFUND/ADJUST',
  `biz_order_no`             VARCHAR(64) DEFAULT NULL COMMENT '关联业务单号',
  `remark`                   VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_at`               DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tx_no` (`tx_no`),
  UNIQUE KEY `uk_idempotency_key` (`idempotency_key`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_wallet_id` (`wallet_id`),
  KEY `idx_biz` (`biz_type`, `biz_order_no`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_tx_type` (`tx_type`),
  CONSTRAINT `fk_wallet_tx_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`),
  CONSTRAINT `fk_wallet_tx_wallet` FOREIGN KEY (`wallet_id`) REFERENCES `user_wallet` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='钱包流水表';

CREATE TABLE `recharge_channel` (
  `id`           BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `channel_code` VARCHAR(64) NOT NULL COMMENT '渠道编码，如 USDT_TRC20',
  `channel_name` VARCHAR(64) NOT NULL COMMENT '渠道名称',
  `network`      VARCHAR(64) DEFAULT NULL COMMENT '链网络，如 TRC20/ERC20/BEP20',
  `display_url`  VARCHAR(255) DEFAULT NULL COMMENT '充值链接或二维码URL',
  `account_name` VARCHAR(128) DEFAULT NULL COMMENT '收款名称',
  `account_no`   VARCHAR(255) DEFAULT NULL COMMENT '收款地址',
  `min_amount`   DECIMAL(20,8) DEFAULT NULL COMMENT '渠道最小充值金额（与全局配置取较大值）',
  `max_amount`   DECIMAL(20,8) DEFAULT NULL COMMENT '最大充值金额',
  `fee_rate`     DECIMAL(12,8) NOT NULL DEFAULT 0.00000000 COMMENT '充值手续费率',
  `sort_no`      INT NOT NULL DEFAULT 0 COMMENT '排序号',
  `status`       TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-停用',
  `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_channel_code` (`channel_code`),
  KEY `idx_status_sort` (`status`, `sort_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='充值渠道表';

CREATE TABLE `recharge_channel_translation` (
  `id`           BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `channel_id`   BIGINT NOT NULL COMMENT '充值渠道ID',
  `locale`       VARCHAR(16) NOT NULL COMMENT '语言：zh-CN/en-US',
  `channel_name` VARCHAR(64) DEFAULT NULL COMMENT '渠道展示名称',
  `account_name` VARCHAR(128) DEFAULT NULL COMMENT '收款名称',
  `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_recharge_channel_translation_locale` (`channel_id`, `locale`),
  KEY `idx_recharge_channel_translation_locale` (`locale`),
  CONSTRAINT `fk_recharge_channel_translation_channel` FOREIGN KEY (`channel_id`) REFERENCES `recharge_channel` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='充值渠道多语言表';

CREATE TABLE `recharge_order` (
  `id`                       BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `recharge_no`              VARCHAR(64) NOT NULL COMMENT '充值单号',
  `user_id`                  BIGINT NOT NULL COMMENT '用户ID',
  `wallet_id`                BIGINT NOT NULL COMMENT '钱包ID',
  `channel_id`               BIGINT NOT NULL COMMENT '充值渠道ID',
  `currency`                 VARCHAR(10) NOT NULL DEFAULT 'USDT' COMMENT '币种，固定USDT',
  `channel_name_snapshot`    VARCHAR(64) NOT NULL COMMENT '渠道名称快照',
  `network_snapshot`         VARCHAR(64) DEFAULT NULL COMMENT '链网络快照',
  `display_url_snapshot`     VARCHAR(255) DEFAULT NULL COMMENT '充值链接快照',
  `account_no_snapshot`      VARCHAR(255) DEFAULT NULL COMMENT '收款地址快照',
  `apply_amount`             DECIMAL(20,8) NOT NULL COMMENT '用户申报充值金额',
  `actual_amount`            DECIMAL(20,8) DEFAULT NULL COMMENT '审核确认到账金额',
  `external_tx_no`           VARCHAR(128) DEFAULT NULL COMMENT '外部流水号/交易哈希，唯一约束防重复入账',
  `payment_proof_url`        VARCHAR(255) DEFAULT NULL COMMENT '支付凭证截图URL',
  `user_remark`              VARCHAR(255) DEFAULT NULL COMMENT '用户备注',
  `status`                   VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED' COMMENT '充值单状态：SUBMITTED-已提交，APPROVED-已到账，REJECTED-已驳回，CANCELED-已取消',
  `reviewed_by`              BIGINT DEFAULT NULL COMMENT '审核管理员ID',
  `reviewed_at`              DATETIME DEFAULT NULL COMMENT '审核时间',
  `review_remark`            VARCHAR(255) DEFAULT NULL COMMENT '审核备注',
  `credited_at`              DATETIME DEFAULT NULL COMMENT '到账时间',
  `wallet_tx_no`             VARCHAR(64) DEFAULT NULL COMMENT '入账钱包流水号',
  `created_at`               DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`               DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_recharge_no` (`recharge_no`),
  UNIQUE KEY `uk_external_tx_no` (`external_tx_no`) COMMENT 'NULL值不受唯一约束影响，只防非空哈希重复',
  KEY `idx_user_id` (`user_id`),
  KEY `idx_wallet_id` (`wallet_id`),
  KEY `idx_channel_id` (`channel_id`),
  KEY `idx_status` (`status`),
  KEY `idx_status_credited_at` (`status`, `credited_at`),
  KEY `idx_status_credited_amount` (`status`, `credited_at`, `actual_amount`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_recharge_order_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`),
  CONSTRAINT `fk_recharge_order_wallet` FOREIGN KEY (`wallet_id`) REFERENCES `user_wallet` (`id`),
  CONSTRAINT `fk_recharge_order_channel` FOREIGN KEY (`channel_id`) REFERENCES `recharge_channel` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='充值申请单';

CREATE TABLE `withdraw_order` (
  `id`              BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `withdraw_no`     VARCHAR(64) NOT NULL COMMENT '提现单号',
  `user_id`         BIGINT NOT NULL COMMENT '用户ID',
  `wallet_id`       BIGINT NOT NULL COMMENT '钱包ID',
  `currency`        VARCHAR(10) NOT NULL DEFAULT 'USDT' COMMENT '币种，固定USDT',
  `withdraw_method` VARCHAR(32) NOT NULL DEFAULT 'USDT' COMMENT '提现方式，第一版固定USDT',
  `network`         VARCHAR(64) DEFAULT NULL COMMENT '链网络，如 TRC20/ERC20/BEP20',
  `account_name`    VARCHAR(64) DEFAULT NULL COMMENT '收款账户名称，可为空',
  `account_no`      VARCHAR(255) NOT NULL COMMENT 'USDT收款地址（已经过前端和后端格式校验）',
  `apply_amount`    DECIMAL(20,8) NOT NULL COMMENT '申请提现金额（最低10 USDT）',
  `fee_amount`      DECIMAL(20,8) NOT NULL DEFAULT 0.00000000 COMMENT '手续费：>=100免手续费，<100收5%，比例从sys_config读取',
  `actual_amount`   DECIMAL(20,8) NOT NULL COMMENT '实际打款金额 = apply_amount - fee_amount',
  `status`          VARCHAR(32) NOT NULL DEFAULT 'PENDING_REVIEW' COMMENT '提现状态：PENDING_REVIEW-待审核，APPROVED-审核通过，PAID-已打款，REJECTED-已驳回，CANCELED-已取消',
  `freeze_tx_no`    VARCHAR(64) DEFAULT NULL COMMENT '冻结钱包流水号',
  `unfreeze_tx_no`  VARCHAR(64) DEFAULT NULL COMMENT '解冻钱包流水号，驳回时使用',
  `paid_tx_no`      VARCHAR(64) DEFAULT NULL COMMENT '打款扣减冻结余额流水号',
  `reviewed_by`     BIGINT DEFAULT NULL COMMENT '审核管理员ID',
  `reviewed_at`     DATETIME DEFAULT NULL COMMENT '审核时间',
  `review_remark`   VARCHAR(255) DEFAULT NULL COMMENT '审核备注',
  `paid_at`         DATETIME DEFAULT NULL COMMENT '打款时间',
  `pay_proof_no`    VARCHAR(128) DEFAULT NULL COMMENT '打款流水号/交易哈希',
  `created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_withdraw_no` (`withdraw_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_wallet_id` (`wallet_id`),
  KEY `idx_status` (`status`),
  KEY `idx_status_paid_at` (`status`, `paid_at`),
  KEY `idx_status_paid_amount` (`status`, `paid_at`, `actual_amount`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_withdraw_order_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`),
  CONSTRAINT `fk_withdraw_order_wallet` FOREIGN KEY (`wallet_id`) REFERENCES `user_wallet` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提现申请单';

CREATE TABLE `region` (
  `id`          BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `region_code` VARCHAR(32) NOT NULL COMMENT '地区编码，如 west-B',
  `region_name` VARCHAR(64) NOT NULL COMMENT '地区名称，如 西北B区',
  `sort_no`     INT NOT NULL DEFAULT 0 COMMENT '排序号，越小越靠前',
  `status`      TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-停用',
  `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_region_code` (`region_code`),
  KEY `idx_status_sort` (`status`, `sort_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='地区表';

CREATE TABLE `gpu_model` (
  `id`         BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `model_code` VARCHAR(64) NOT NULL COMMENT '型号编码，如 RTX_5090',
  `model_name` VARCHAR(64) NOT NULL COMMENT '型号名称，如 RTX 5090',
  `sort_no`    INT NOT NULL DEFAULT 0 COMMENT '排序号，越小越靠前',
  `status`     TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-停用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_model_code` (`model_code`),
  UNIQUE KEY `uk_model_name` (`model_name`),
  KEY `idx_status_sort` (`status`, `sort_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='GPU型号表';

CREATE TABLE `product` (
  `id`                      BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `product_code`            VARCHAR(64) NOT NULL COMMENT '产品编码',
  `product_name`            VARCHAR(128) NOT NULL COMMENT '产品名称，如 RTX 5090 / 32 GB',
  `machine_code`            VARCHAR(64) DEFAULT NULL COMMENT '机器展示编码，仅展示',
  `machine_alias`           VARCHAR(64) DEFAULT NULL COMMENT '机器别名，如 566机',
  `region_id`               BIGINT NOT NULL COMMENT '地区ID',
  `gpu_model_id`            BIGINT NOT NULL COMMENT 'GPU型号ID',
  `gpu_memory_gb`           INT NOT NULL COMMENT 'GPU显存GB',
  `gpu_power_tops`          DECIMAL(20,4) DEFAULT NULL COMMENT 'GPU算力TOPS',
  `rent_price`              DECIMAL(20,8) NOT NULL COMMENT '机器购买/租赁价格，USDT',
  `token_output_per_minute` BIGINT NOT NULL COMMENT '每分钟Token产出',
  `token_output_per_day`    BIGINT NOT NULL COMMENT '每日Token产出',
  `rentable_until`          DATE DEFAULT NULL COMMENT '可租用至日期',
  `total_stock`             INT NOT NULL DEFAULT 0 COMMENT '总库存，仅后台手动展示',
  `available_stock`         INT NOT NULL DEFAULT 0 COMMENT '可用库存，仅后台手动展示',
  `rented_stock`            INT NOT NULL DEFAULT 0 COMMENT '已租库存，仅后台手动展示',
  `cpu_model`               VARCHAR(128) DEFAULT NULL COMMENT 'CPU型号',
  `cpu_cores`               INT DEFAULT NULL COMMENT 'CPU核数',
  `memory_gb`               INT DEFAULT NULL COMMENT '内存GB',
  `system_disk_gb`          INT DEFAULT NULL COMMENT '系统盘GB',
  `data_disk_gb`            INT DEFAULT NULL COMMENT '数据盘GB',
  `max_expand_disk_gb`      INT DEFAULT NULL COMMENT '最大可扩容数据盘GB',
  `driver_version`          VARCHAR(32) DEFAULT NULL COMMENT 'GPU驱动版本',
  `cuda_version`            VARCHAR(32) DEFAULT NULL COMMENT 'CUDA版本',
  `has_cache_optimization`  TINYINT NOT NULL DEFAULT 0 COMMENT '是否显示缓存优化图标：1-是，0-否',
  `status`                  TINYINT NOT NULL DEFAULT 1 COMMENT '产品状态：1-上架，0-下架',
  `sort_no`                 INT NOT NULL DEFAULT 0 COMMENT '排序号，越小越靠前',
  `version_no`              INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at`              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_code` (`product_code`),
  KEY `idx_machine_code` (`machine_code`),
  KEY `idx_region_id` (`region_id`),
  KEY `idx_gpu_model_id` (`gpu_model_id`),
  KEY `idx_product_region_status_gpu` (`region_id`, `status`, `gpu_model_id`),
  KEY `idx_status_sort` (`status`, `sort_no`),
  KEY `idx_product_status_available_sort` (`status`, ((CASE WHEN `available_stock` > 0 THEN 0 ELSE 1 END)), `sort_no`, `id`),
  KEY `idx_product_region_gpu_available_sort` (`status`, `region_id`, `gpu_model_id`, ((CASE WHEN `available_stock` > 0 THEN 0 ELSE 1 END)), `sort_no`, `id`),
  KEY `idx_rentable_until` (`rentable_until`),
  CONSTRAINT `fk_product_region` FOREIGN KEY (`region_id`) REFERENCES `region` (`id`),
  CONSTRAINT `fk_product_gpu_model` FOREIGN KEY (`gpu_model_id`) REFERENCES `gpu_model` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='GPU机器产品表';

CREATE TABLE `ai_model` (
  `id`                                   BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `model_code`                           VARCHAR(64) NOT NULL COMMENT '模型编码，如 GPT_5_NANO',
  `model_name`                           VARCHAR(128) NOT NULL COMMENT '模型名称，如 GPT-5 Nano',
  `vendor_name`                          VARCHAR(64) DEFAULT NULL COMMENT '所属机构/厂商，如 OpenAI',
  `logo_url`                             VARCHAR(255) DEFAULT NULL COMMENT 'logo地址',
  `monthly_token_consumption_trillion`   DECIMAL(20,4) DEFAULT NULL COMMENT 'Token月消耗量，单位：万亿，仅展示',
  `token_unit_price`                     DECIMAL(20,8) NOT NULL DEFAULT 0.00000000 COMMENT '单个Token价格，USDT，用于收益公式',
  `deploy_tech_fee`                      DECIMAL(20,8) NOT NULL DEFAULT 0.00000000 COMMENT 'API部署技术费用，USDT，用户激活时支付（支付即激活，无需额外点击）',
  `status`                               TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-停用',
  `sort_no`                              INT NOT NULL DEFAULT 0 COMMENT '排序号',
  `created_at`                           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`                           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_model_code` (`model_code`),
  KEY `idx_status_sort` (`status`, `sort_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI API模型表';

CREATE TABLE `rental_cycle_rule` (
  `id`                  BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `cycle_code`          VARCHAR(32) NOT NULL COMMENT '周期编码，如 D7/D15/D30/D90/D180/D360',
  `cycle_name`          VARCHAR(64) NOT NULL COMMENT '周期名称，如 7天/15天/1个月',
  `cycle_days`          INT NOT NULL COMMENT '周期天数',
  `yield_multiplier`    DECIMAL(10,4) NOT NULL COMMENT '收益倍率，如 1.0000/1.1000/1.5000',
  `early_penalty_rate`  DECIMAL(10,4) NOT NULL DEFAULT 0.0100 COMMENT '提前解约违约金比例，默认1%',
  `status`              TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-停用',
  `sort_no`             INT NOT NULL DEFAULT 0 COMMENT '排序号',
  `created_at`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cycle_code` (`cycle_code`),
  UNIQUE KEY `uk_cycle_days` (`cycle_days`),
  KEY `idx_status_sort` (`status`, `sort_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租赁周期收益规则表';

CREATE TABLE `rental_order` (
  `id`                                   BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no`                             VARCHAR(64) NOT NULL COMMENT '订单号',
  `user_id`                              BIGINT NOT NULL COMMENT '用户ID',
  `product_id`                           BIGINT NOT NULL COMMENT '产品ID',
  `ai_model_id`                          BIGINT NOT NULL COMMENT 'AI模型ID',
  `cycle_rule_id`                        BIGINT NOT NULL COMMENT '租赁周期规则ID',

  -- 产品快照
  `product_code_snapshot`                VARCHAR(64) NOT NULL COMMENT '产品编码快照',
  `product_name_snapshot`                VARCHAR(128) NOT NULL COMMENT '产品名称快照',
  `machine_code_snapshot`                VARCHAR(64) DEFAULT NULL COMMENT '机器展示编码快照',
  `machine_alias_snapshot`               VARCHAR(64) DEFAULT NULL COMMENT '机器别名快照',
  `region_name_snapshot`                 VARCHAR(64) NOT NULL COMMENT '地区名称快照',
  `gpu_model_snapshot`                   VARCHAR(64) NOT NULL COMMENT 'GPU型号快照',
  `gpu_memory_snapshot_gb`               INT NOT NULL COMMENT '显存快照GB',
  `gpu_power_tops_snapshot`              DECIMAL(20,4) DEFAULT NULL COMMENT 'GPU算力快照TOPS',
  `gpu_rent_price_snapshot`              DECIMAL(20,8) NOT NULL COMMENT '机器价格快照USDT',
  `token_output_per_day_snapshot`        BIGINT NOT NULL COMMENT '每日Token产出快照',

  -- AI模型快照
  `ai_model_name_snapshot`               VARCHAR(128) NOT NULL COMMENT 'AI模型名称快照',
  `ai_vendor_name_snapshot`              VARCHAR(64) DEFAULT NULL COMMENT 'AI模型厂商快照',
  `monthly_token_consumption_snapshot`   DECIMAL(20,4) DEFAULT NULL COMMENT 'Token月消耗量快照，仅展示',
  `token_unit_price_snapshot`            DECIMAL(20,8) NOT NULL DEFAULT 0.00000000 COMMENT 'Token单价快照USDT',
  `deploy_fee_snapshot`                  DECIMAL(20,8) NOT NULL DEFAULT 0.00000000 COMMENT 'API部署费用快照USDT',

  -- 周期快照
  `cycle_days_snapshot`                  INT NOT NULL COMMENT '租赁周期天数快照',
  `yield_multiplier_snapshot`            DECIMAL(10,4) NOT NULL COMMENT '收益倍率快照',
  `early_penalty_rate_snapshot`          DECIMAL(10,4) NOT NULL DEFAULT 0.0100 COMMENT '提前解约违约金比例快照',

  -- 金额
  `currency`                             VARCHAR(10) NOT NULL DEFAULT 'USDT' COMMENT '币种，固定USDT',
  `order_amount`                         DECIMAL(20,8) NOT NULL DEFAULT 0.00000000 COMMENT '机器订单金额/本金USDT',
  `paid_amount`                          DECIMAL(20,8) NOT NULL DEFAULT 0.00000000 COMMENT '机器实付金额USDT',
  `expected_daily_profit`                DECIMAL(20,8) NOT NULL DEFAULT 0.00000000 COMMENT '预计每日收益USDT',
  `expected_total_profit`                DECIMAL(20,8) NOT NULL DEFAULT 0.00000000 COMMENT '预计总收益USDT',

  -- 状态
  `order_status`      VARCHAR(32) NOT NULL DEFAULT 'PENDING_PAY' COMMENT '订单状态：PENDING_PAY/PAID/PENDING_ACTIVATION/ACTIVATING/PAUSED/RUNNING/EXPIRED/SETTLING/SETTLED/EARLY_CLOSED/CANCELED',
  `profit_status`     VARCHAR(32) NOT NULL DEFAULT 'NOT_STARTED' COMMENT '收益状态：NOT_STARTED/RUNNING/PAUSED/FINISHED',
  `settlement_status` VARCHAR(32) NOT NULL DEFAULT 'UNSETTLED' COMMENT '结算状态：UNSETTLED/SETTLING/SETTLED',

  -- 时间轴（所有时间均为UTC+8）
  `machine_pay_tx_no`   VARCHAR(64) DEFAULT NULL COMMENT '机器费用支付钱包流水号',
  `paid_at`             DATETIME DEFAULT NULL COMMENT '机器费用支付时间',
  `api_generated_at`    DATETIME DEFAULT NULL COMMENT 'API生成时间（超时1小时未激活则自动取消）',
  `deploy_fee_paid_at`  DATETIME DEFAULT NULL COMMENT 'API部署费用支付时间（支付即激活）',
  `activated_at`        DATETIME DEFAULT NULL COMMENT '激活时间（=deploy_fee_paid_at）',
  `auto_pause_at`       DATETIME DEFAULT NULL COMMENT '自动暂停时间（activated_at + 24小时）',
  `paused_at`           DATETIME DEFAULT NULL COMMENT '暂停时间',
  `started_at`          DATETIME DEFAULT NULL COMMENT '用户点击启动时间',
  `profit_start_at`     DATETIME DEFAULT NULL COMMENT '收益开始时间（=started_at）',
  `profit_end_at`       DATETIME DEFAULT NULL COMMENT '收益结束时间（profit_start_at + cycle_days）',
  `expired_at`          DATETIME DEFAULT NULL COMMENT '到期时间',
  `canceled_at`         DATETIME DEFAULT NULL COMMENT '取消时间',
  `finished_at`         DATETIME DEFAULT NULL COMMENT '完成时间',

  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_ai_model_id` (`ai_model_id`),
  KEY `idx_cycle_rule_id` (`cycle_rule_id`),
  KEY `idx_order_status` (`order_status`),
  KEY `idx_profit_status` (`profit_status`),
  KEY `idx_paid_at` (`paid_at`),
  KEY `idx_paid_at_amount` (`paid_at`, `paid_amount`),
  KEY `idx_profit_time` (`profit_start_at`, `profit_end_at`),
  KEY `idx_order_status_profit_end_at` (`order_status`, `profit_end_at`) COMMENT '用于到期结算定时任务扫描',
  KEY `idx_api_generated_at` (`api_generated_at`) COMMENT '用于超时取消定时任务扫描',
  KEY `idx_auto_pause_at` (`auto_pause_at`) COMMENT '用于自动暂停定时任务扫描',
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_rental_order_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`),
  CONSTRAINT `fk_rental_order_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`),
  CONSTRAINT `fk_rental_order_ai_model` FOREIGN KEY (`ai_model_id`) REFERENCES `ai_model` (`id`),
  CONSTRAINT `fk_rental_order_cycle_rule` FOREIGN KEY (`cycle_rule_id`) REFERENCES `rental_cycle_rule` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租赁订单主表';

CREATE TABLE `rental_order_run_segment` (
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

CREATE TABLE `api_credential` (
  `id`                    BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `credential_no`         VARCHAR(64) NOT NULL COMMENT '凭证编号',
  `user_id`               BIGINT NOT NULL COMMENT '用户ID',
  `rental_order_id`       BIGINT NOT NULL COMMENT '租赁订单ID',
  `api_name`              VARCHAR(128) DEFAULT NULL COMMENT 'API名称，用于前台展示',
  `api_base_url`          VARCHAR(255) DEFAULT NULL COMMENT 'API基础地址',
  `token_ciphertext`      TEXT NOT NULL COMMENT '加密存储的完整Token（AES-256-GCM，密钥从配置中心读取）',
  `token_masked`          VARCHAR(128) DEFAULT NULL COMMENT '脱敏展示Token，如 sk-****abcd',
  `model_name_snapshot`   VARCHAR(128) DEFAULT NULL COMMENT '模型名称快照',
  `deploy_fee_snapshot`   DECIMAL(20,8) NOT NULL DEFAULT 0.00000000 COMMENT '部署费用快照USDT',
  `token_status`          VARCHAR(32) NOT NULL DEFAULT 'GENERATED' COMMENT '状态：GENERATED-已生成，ACTIVATING-激活中，PAUSED-已暂停，ACTIVE-运行中，EXPIRED-已过期，REVOKED-已吊销',
  `generated_at`          DATETIME DEFAULT NULL COMMENT 'API生成时间',
  `activation_paid_at`    DATETIME DEFAULT NULL COMMENT '激活费用支付时间（支付即激活）',
  `activated_at`          DATETIME DEFAULT NULL COMMENT '激活时间',
  `auto_pause_at`         DATETIME DEFAULT NULL COMMENT '自动暂停时间（激活后24小时）',
  `paused_at`             DATETIME DEFAULT NULL COMMENT '暂停时间',
  `started_at`            DATETIME DEFAULT NULL COMMENT '启动时间',
  `expired_at`            DATETIME DEFAULT NULL COMMENT '过期时间',
  `revoked_at`            DATETIME DEFAULT NULL COMMENT '吊销时间',
  `mock_request_count`    BIGINT NOT NULL DEFAULT 0 COMMENT '模拟请求次数，仅前台展示',
  `mock_token_display`    BIGINT NOT NULL DEFAULT 0 COMMENT '模拟Token展示数量，仅前台展示',
  `mock_last_refresh_at`  DATETIME DEFAULT NULL COMMENT '模拟数据最后刷新时间',
  `remark`                VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_at`            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_credential_no` (`credential_no`),
  UNIQUE KEY `uk_rental_order_id` (`rental_order_id`) COMMENT '一个订单只能有一个API凭证',
  KEY `idx_user_id` (`user_id`),
  KEY `idx_token_status` (`token_status`),
  CONSTRAINT `fk_api_credential_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`),
  CONSTRAINT `fk_api_credential_order` FOREIGN KEY (`rental_order_id`) REFERENCES `rental_order` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API凭证表';

CREATE TABLE `api_deploy_order` (
  `id`                  BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `deploy_no`           VARCHAR(64) NOT NULL COMMENT '部署支付单号',
  `user_id`             BIGINT NOT NULL COMMENT '用户ID',
  `rental_order_id`     BIGINT NOT NULL COMMENT '租赁订单ID',
  `api_credential_id`   BIGINT NOT NULL COMMENT 'API凭证ID',
  `ai_model_id`         BIGINT NOT NULL COMMENT 'AI模型ID',
  `model_name_snapshot` VARCHAR(128) NOT NULL COMMENT '模型名称快照',
  `currency`            VARCHAR(10) NOT NULL DEFAULT 'USDT' COMMENT '币种，固定USDT',
  `deploy_fee_amount`   DECIMAL(20,8) NOT NULL COMMENT '部署费用金额USDT',
  `status`              VARCHAR(32) NOT NULL DEFAULT 'PENDING_PAY' COMMENT '状态：PENDING_PAY-待支付，PAID-已支付，CANCELED-已取消，REFUNDED-已退款',
  `wallet_tx_no`        VARCHAR(64) DEFAULT NULL COMMENT '钱包流水号',
  `paid_at`             DATETIME DEFAULT NULL COMMENT '支付时间（支付即激活）',
  `canceled_at`         DATETIME DEFAULT NULL COMMENT '取消时间',
  `created_at`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_deploy_no` (`deploy_no`),
  UNIQUE KEY `uk_order_api` (`rental_order_id`, `api_credential_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_order_id` (`rental_order_id`),
  KEY `idx_api_credential_id` (`api_credential_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_api_deploy_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`),
  CONSTRAINT `fk_api_deploy_order` FOREIGN KEY (`rental_order_id`) REFERENCES `rental_order` (`id`),
  CONSTRAINT `fk_api_deploy_credential` FOREIGN KEY (`api_credential_id`) REFERENCES `api_credential` (`id`),
  CONSTRAINT `fk_api_deploy_ai_model` FOREIGN KEY (`ai_model_id`) REFERENCES `ai_model` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API部署费用支付单';

CREATE TABLE `rental_profit_record` (
  `id`                       BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `profit_no`                VARCHAR(64) NOT NULL COMMENT '收益单号',
  `user_id`                  BIGINT NOT NULL COMMENT '用户ID',
  `rental_order_id`          BIGINT NOT NULL COMMENT '租赁订单ID',
  `profit_date`              DATE NOT NULL COMMENT '收益日期（UTC+8，按0点切割）',
  `effective_minutes`        INT NOT NULL DEFAULT 1440 COMMENT '本收益日有效运行完整分钟数，不足1分钟不计',
  `period_start_at`          DATETIME DEFAULT NULL COMMENT '本次收益计算有效开始时间',
  `period_end_at`            DATETIME DEFAULT NULL COMMENT '本次收益计算有效结束时间',
  `gpu_daily_token_snapshot` BIGINT NOT NULL COMMENT 'GPU每日产出Token快照',
  `token_price_snapshot`     DECIMAL(20,8) NOT NULL COMMENT 'Token单价快照USDT',
  `yield_multiplier_snapshot` DECIMAL(10,4) NOT NULL COMMENT '周期收益倍率快照',
  `base_profit_amount`       DECIMAL(20,8) NOT NULL COMMENT '基础收益 = token_snapshot × price_snapshot',
  `final_profit_amount`      DECIMAL(20,8) NOT NULL COMMENT '最终收益 = base × multiplier',
  `status`                   VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-待结算，SETTLED-已入账，CANCELED-已取消',
  `wallet_tx_no`             VARCHAR(64) DEFAULT NULL COMMENT '收益入账钱包流水号',
  `commission_generated`     TINYINT NOT NULL DEFAULT 0 COMMENT '是否已生成佣金：1-是，0-否',
  `settled_at`               DATETIME DEFAULT NULL COMMENT '收益入账时间',
  `remark`                   VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_at`               DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`               DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_profit_no` (`profit_no`),
  UNIQUE KEY `uk_order_profit_date` (`rental_order_id`, `profit_date`) COMMENT '防重复入账，定时任务用INSERT IGNORE或捕获唯一键冲突',
  KEY `idx_user_date` (`user_id`, `profit_date`),
  KEY `idx_status` (`status`),
  KEY `idx_status_profit_date` (`status`, `profit_date`),
  KEY `idx_status_profit_date_amount` (`status`, `profit_date`, `final_profit_amount`),
  KEY `idx_commission_generated` (`commission_generated`),
  KEY `idx_profit_period` (`period_start_at`, `period_end_at`),
  CONSTRAINT `fk_profit_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`),
  CONSTRAINT `fk_profit_order` FOREIGN KEY (`rental_order_id`) REFERENCES `rental_order` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日收益明细表';

CREATE TABLE `rental_settlement_order` (
  `id`                   BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `settlement_no`        VARCHAR(64) NOT NULL COMMENT '结算单号',
  `user_id`              BIGINT NOT NULL COMMENT '用户ID',
  `rental_order_id`      BIGINT NOT NULL COMMENT '租赁订单ID',
  `settlement_type`      VARCHAR(32) NOT NULL COMMENT '结算类型：EXPIRE-到期结算，EARLY_TERMINATE-提前解约，MANUAL-人工结算',
  `currency`             VARCHAR(10) NOT NULL DEFAULT 'USDT' COMMENT '币种，固定USDT',
  `principal_amount`     DECIMAL(20,8) NOT NULL DEFAULT 0.00000000 COMMENT '机器本金/订单金额USDT',
  `profit_amount`        DECIMAL(20,8) NOT NULL DEFAULT 0.00000000 COMMENT '累计收益汇总USDT，仅展示审计，已入账收益不重复入账',
  `penalty_amount`       DECIMAL(20,8) NOT NULL DEFAULT 0.00000000 COMMENT '提前解约违约金USDT（= order_amount × early_penalty_rate_snapshot）',
  `actual_settle_amount` DECIMAL(20,8) NOT NULL DEFAULT 0.00000000 COMMENT '实际结算金额（= 本金 - 违约金）',
  `status`               VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-待结算，SETTLED-已结算，REJECTED-驳回，CANCELED-取消',
  `reviewed_by`          BIGINT DEFAULT NULL COMMENT '审核管理员ID',
  `reviewed_at`          DATETIME DEFAULT NULL COMMENT '审核时间',
  `settled_at`           DATETIME DEFAULT NULL COMMENT '结算时间',
  `wallet_tx_no`         VARCHAR(64) DEFAULT NULL COMMENT '结算入账或扣款钱包流水号',
  `remark`               VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_at`           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_settlement_no` (`settlement_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_order_id` (`rental_order_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_settlement_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`),
  CONSTRAINT `fk_settlement_order` FOREIGN KEY (`rental_order_id`) REFERENCES `rental_order` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租赁订单结算表';

CREATE TABLE `commission_rule` (
  `id`              BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `level_no`        INT NOT NULL COMMENT '层级：1-一级，2-二级',
  `commission_rate` DECIMAL(10,4) NOT NULL COMMENT '佣金比例，如 0.2000（20%）',
  `status`          TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-停用',
  `created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_level_no` (`level_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='佣金规则表';

CREATE TABLE `commission_record` (
  `id`                       BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `commission_no`            VARCHAR(64) NOT NULL COMMENT '佣金单号',
  `benefit_user_id`          BIGINT NOT NULL COMMENT '获得佣金的用户ID',
  `source_user_id`           BIGINT NOT NULL COMMENT '产生收益的下级用户ID',
  `source_order_id`          BIGINT NOT NULL COMMENT '来源租赁订单ID',
  `source_profit_id`         BIGINT NOT NULL COMMENT '来源收益记录ID',
  `level_no`                 INT NOT NULL COMMENT '佣金层级：1/2',
  `currency`                 VARCHAR(10) NOT NULL DEFAULT 'USDT' COMMENT '币种，固定USDT',
  `source_profit_amount`     DECIMAL(20,8) NOT NULL COMMENT '下级收益金额USDT',
  `commission_rate_snapshot` DECIMAL(10,4) NOT NULL COMMENT '佣金比例快照',
  `commission_amount`        DECIMAL(20,8) NOT NULL COMMENT '佣金金额USDT',
  `status`                   VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-待入账，SETTLED-已入账，CANCELED-已取消',
  `wallet_tx_no`             VARCHAR(64) DEFAULT NULL COMMENT '佣金入账钱包流水号',
  `settled_at`               DATETIME DEFAULT NULL COMMENT '入账时间',
  `created_at`               DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`               DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_commission_no` (`commission_no`),
  UNIQUE KEY `uk_source_profit_level` (`source_profit_id`, `level_no`) COMMENT '防重复佣金',
  KEY `idx_benefit_user` (`benefit_user_id`),
  KEY `idx_source_user` (`source_user_id`),
  KEY `idx_source_order` (`source_order_id`),
  KEY `idx_status` (`status`),
  KEY `idx_status_settled_at` (`status`, `settled_at`),
  KEY `idx_status_settled_amount` (`status`, `settled_at`, `commission_amount`),
  KEY `idx_benefit_status_settled_at` (`benefit_user_id`, `status`, `settled_at`, `commission_amount`),
  KEY `idx_benefit_status_level` (`benefit_user_id`, `status`, `level_no`, `commission_amount`),
  CONSTRAINT `fk_commission_benefit_user` FOREIGN KEY (`benefit_user_id`) REFERENCES `app_user` (`id`),
  CONSTRAINT `fk_commission_source_user` FOREIGN KEY (`source_user_id`) REFERENCES `app_user` (`id`),
  CONSTRAINT `fk_commission_profit` FOREIGN KEY (`source_profit_id`) REFERENCES `rental_profit_record` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='佣金记录表';

CREATE TABLE `sys_config` (
  `id`           BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_key`   VARCHAR(64) NOT NULL COMMENT '配置键',
  `config_value` VARCHAR(255) NOT NULL COMMENT '配置值',
  `config_desc`  VARCHAR(255) DEFAULT NULL COMMENT '说明',
  `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统可配置参数表';

CREATE TABLE `sys_admin` (
  `id`            BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_name`     VARCHAR(64) NOT NULL COMMENT '管理员登录名，唯一',
  `password_hash` VARCHAR(255) NOT NULL COMMENT '后台登录密码哈希',
  `role`          VARCHAR(20) NOT NULL DEFAULT 'ADMIN' COMMENT '角色：SUPER_ADMIN-超级管理员，ADMIN-普通管理员',
  `status`        TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-正常，0-禁用F',
  `last_login_at` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `created_by`    BIGINT DEFAULT NULL COMMENT '创建人管理员ID，首个超管可为空',
  `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_name` (`user_name`),
  KEY `idx_role` (`role`),
  KEY `idx_status` (`status`),
  KEY `idx_created_by` (`created_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统管理员表';

CREATE TABLE `sys_admin_log` (
  `id`           BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `admin_id`     BIGINT NOT NULL COMMENT '操作管理员ID',
  `action`       VARCHAR(64) NOT NULL COMMENT '操作类型，如 APPROVE_RECHARGE/REJECT_WITHDRAW/BAN_USER/ADJUST_WALLET/EDIT_PRODUCT',
  `target_table` VARCHAR(64) DEFAULT NULL COMMENT '操作目标表',
  `target_id`    BIGINT DEFAULT NULL COMMENT '操作目标ID',
  `before_value` TEXT DEFAULT NULL COMMENT '操作前快照（JSON格式）',
  `after_value`  TEXT DEFAULT NULL COMMENT '操作后快照（JSON格式）',
  `remark`       VARCHAR(255) DEFAULT NULL COMMENT '操作备注',
  `ip`           VARCHAR(64) DEFAULT NULL COMMENT '操作IP',
  `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_admin_id` (`admin_id`),
  KEY `idx_action` (`action`),
  KEY `idx_target` (`target_table`, `target_id`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_admin_log_admin` FOREIGN KEY (`admin_id`) REFERENCES `sys_admin` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='后台管理员操作日志表';

CREATE TABLE `sys_notification` (
  `id`          BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`     BIGINT NOT NULL COMMENT '接收通知的用户ID',
  `title`       VARCHAR(128) NOT NULL COMMENT '通知标题',
  `content`     TEXT NOT NULL COMMENT '通知内容',
  `type`        VARCHAR(32) NOT NULL DEFAULT 'FINANCIAL' COMMENT '通知分类：FINANCIAL-账务，SYSTEM-系统，BLOG-博客',
  `biz_type`    VARCHAR(32) DEFAULT NULL COMMENT '业务类型：见枚举 6.21',
  `biz_id`      BIGINT DEFAULT NULL COMMENT '关联业务ID',
  `read_status` TINYINT NOT NULL DEFAULT 0 COMMENT '读取状态：0-未读，1-已读',
  `read_at`     DATETIME DEFAULT NULL COMMENT '读取时间',
  `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_biz` (`user_id`, `biz_type`, `biz_id`) COMMENT '防重复通知',
  KEY `idx_user_read` (`user_id`, `read_status`),
  KEY `idx_user_created` (`user_id`, `created_at`),
  KEY `idx_biz` (`biz_type`, `biz_id`),
  CONSTRAINT `fk_notification_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户站内信通知表';

CREATE TABLE `sys_notification_translation` (
  `id`              BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `notification_id` BIGINT NOT NULL COMMENT '站内信通知ID',
  `locale`          VARCHAR(16) NOT NULL COMMENT '语言：zh-CN/en-US',
  `title`           VARCHAR(128) DEFAULT NULL COMMENT '通知标题',
  `content`         TEXT DEFAULT NULL COMMENT '通知内容',
  `created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notification_translation_locale` (`notification_id`, `locale`),
  KEY `idx_notification_translation_locale` (`locale`),
  CONSTRAINT `fk_notification_translation_notification` FOREIGN KEY (`notification_id`) REFERENCES `sys_notification` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户站内信通知多语言表';

CREATE TABLE `scheduler_log` (
  `id`            BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_name`     VARCHAR(64) NOT NULL COMMENT '任务名称，如 daily_profit/auto_pause/activation_timeout',
  `started_at`    DATETIME NOT NULL COMMENT '开始时间',
  `finished_at`   DATETIME DEFAULT NULL COMMENT '结束时间',
  `total_count`   INT NOT NULL DEFAULT 0 COMMENT '扫描总数',
  `success_count` INT NOT NULL DEFAULT 0 COMMENT '成功数',
  `fail_count`    INT NOT NULL DEFAULT 0 COMMENT '失败数',
  `status`        VARCHAR(16) NOT NULL DEFAULT 'RUNNING' COMMENT '执行状态：RUNNING/SUCCESS/PARTIAL_FAIL/FAIL',
  `error_message` TEXT DEFAULT NULL COMMENT '异常信息',
  `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_name` (`task_name`),
  KEY `idx_started_at` (`started_at`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='定时任务执行日志表';

CREATE TABLE `user_push_device` (
  `id`             BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`        BIGINT NOT NULL COMMENT '所属用户ID',
  `device_type`    VARCHAR(20) NOT NULL COMMENT '设备类型：IOS/ANDROID',
  `device_token`   VARCHAR(255) NOT NULL COMMENT '推送设备Token',
  `status`         TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-有效，0-失效',
  `last_active_at` DATETIME DEFAULT NULL COMMENT '最后活跃时间',
  `created_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_token` (`device_token`),
  KEY `idx_user_status` (`user_id`, `status`),
  CONSTRAINT `fk_push_device_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户App Push设备表';

CREATE TABLE `blog_category` (
  `id`            BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `category_name` VARCHAR(64) NOT NULL COMMENT '分类名称',
  `sort_no`       INT NOT NULL DEFAULT 0 COMMENT '排序号',
  `status`        TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-停用',
  `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status_sort` (`status`, `sort_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='博客分类表';

CREATE TABLE `blog_tag` (
  `id`         BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tag_name`   VARCHAR(64) NOT NULL COMMENT '标签名称',
  `sort_no`    INT NOT NULL DEFAULT 0 COMMENT '排序号',
  `status`     TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-停用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_name` (`tag_name`),
  KEY `idx_status_sort` (`status`, `sort_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='博客标签表';

CREATE TABLE `blog_post` (
  `id`               BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `category_id`      BIGINT DEFAULT NULL COMMENT '分类ID，可为空',
  `title`            VARCHAR(255) NOT NULL COMMENT '文章标题',
  `summary`          VARCHAR(500) DEFAULT NULL COMMENT '文章摘要，用于列表页',
  `cover_image_url`  VARCHAR(255) DEFAULT NULL COMMENT '封面图URL',
  `content_markdown` LONGTEXT NOT NULL COMMENT '文章正文Markdown内容',
  `publish_status`   TINYINT NOT NULL DEFAULT 0 COMMENT '发布状态：0-草稿，1-已发布，2-已下线',
  `published_at`     DATETIME DEFAULT NULL COMMENT '发布时间',
  `is_top`           TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶：1-是，0-否',
  `sort_no`          INT NOT NULL DEFAULT 0 COMMENT '排序号',
  `view_count`       BIGINT NOT NULL DEFAULT 0 COMMENT '浏览量',
  `created_by`       BIGINT DEFAULT NULL COMMENT '创建人管理员ID',
  `created_at`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_publish_status` (`publish_status`),
  KEY `idx_published_at` (`published_at`),
  KEY `idx_top_sort` (`is_top`, `sort_no`),
  CONSTRAINT `fk_blog_post_category` FOREIGN KEY (`category_id`) REFERENCES `blog_category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='博客文章表';

CREATE TABLE `blog_post_tag` (
  `id`         BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `post_id`    BIGINT NOT NULL COMMENT '文章ID',
  `tag_id`     BIGINT NOT NULL COMMENT '标签ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_tag` (`post_id`, `tag_id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_tag_id` (`tag_id`),
  CONSTRAINT `fk_blog_post_tag_post` FOREIGN KEY (`post_id`) REFERENCES `blog_post` (`id`),
  CONSTRAINT `fk_blog_post_tag_tag` FOREIGN KEY (`tag_id`) REFERENCES `blog_tag` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章标签关联表';

CREATE TABLE `blog_category_translation` (
  `id`            BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `category_id`   BIGINT NOT NULL COMMENT '博客分类ID',
  `locale`        VARCHAR(16) NOT NULL COMMENT '语言：zh-CN/en-US',
  `category_name` VARCHAR(64) NOT NULL COMMENT '分类展示名称',
  `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_blog_category_translation_locale` (`category_id`, `locale`),
  KEY `idx_blog_category_translation_locale` (`locale`),
  CONSTRAINT `fk_blog_category_translation_category` FOREIGN KEY (`category_id`) REFERENCES `blog_category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='博客分类多语言表';

CREATE TABLE `blog_tag_translation` (
  `id`         BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tag_id`     BIGINT NOT NULL COMMENT '博客标签ID',
  `locale`     VARCHAR(16) NOT NULL COMMENT '语言：zh-CN/en-US',
  `tag_name`   VARCHAR(64) NOT NULL COMMENT '标签展示名称',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_blog_tag_translation_locale` (`tag_id`, `locale`),
  KEY `idx_blog_tag_translation_locale` (`locale`),
  CONSTRAINT `fk_blog_tag_translation_tag` FOREIGN KEY (`tag_id`) REFERENCES `blog_tag` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='博客标签多语言表';

CREATE TABLE `blog_post_translation` (
  `id`               BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `post_id`          BIGINT NOT NULL COMMENT '博客文章ID',
  `locale`           VARCHAR(16) NOT NULL COMMENT '语言：zh-CN/en-US',
  `title`            VARCHAR(255) NOT NULL COMMENT '文章标题',
  `summary`          VARCHAR(500) DEFAULT NULL COMMENT '文章摘要',
  `content_markdown` LONGTEXT DEFAULT NULL COMMENT '文章正文Markdown内容',
  `created_at`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_blog_post_translation_locale` (`post_id`, `locale`),
  KEY `idx_blog_post_translation_locale` (`locale`),
  CONSTRAINT `fk_blog_post_translation_post` FOREIGN KEY (`post_id`) REFERENCES `blog_post` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='博客文章多语言表';

CREATE TABLE `doc_category` (
  `id`            BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id`     BIGINT DEFAULT NULL COMMENT '父级分类ID，NULL表示一级分类',
  `language`      VARCHAR(20) NOT NULL DEFAULT 'zh-CN' COMMENT '文档语言：zh-CN/en-US',
  `section`       VARCHAR(32) NOT NULL COMMENT '文档分区：guide/integration/faq/support',
  `category_code` VARCHAR(64) NOT NULL COMMENT '分类编码，用于前端路由标识',
  `category_name` VARCHAR(64) NOT NULL COMMENT '分类名称',
  `icon`          VARCHAR(64) DEFAULT NULL COMMENT '分类图标标识',
  `sort_no`       INT NOT NULL DEFAULT 0 COMMENT '排序号',
  `status`        TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-停用',
  `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_language_section_category_code` (`language`, `section`, `category_code`),
  KEY `idx_language_section_parent_status_sort` (`language`, `section`, `parent_id`, `status`, `sort_no`),
  KEY `idx_language_section_status_sort` (`language`, `section`, `status`, `sort_no`),
  CONSTRAINT `fk_doc_category_parent` FOREIGN KEY (`parent_id`) REFERENCES `doc_category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档分类表';

CREATE TABLE `doc_article` (
  `id`               BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `category_id`      BIGINT NOT NULL COMMENT '所属分类ID',
  `language`         VARCHAR(20) NOT NULL DEFAULT 'zh-CN' COMMENT '文档语言：zh-CN/en-US',
  `section`          VARCHAR(32) NOT NULL COMMENT '文档分区：guide/integration/faq/support',
  `title`            VARCHAR(255) NOT NULL COMMENT '文档标题',
  `slug`             VARCHAR(128) NOT NULL COMMENT '文档路由标识',
  `summary`          VARCHAR(500) DEFAULT NULL COMMENT '文档摘要',
  `content_markdown` LONGTEXT NOT NULL COMMENT '文档正文Markdown内容',
  `publish_status`   TINYINT NOT NULL DEFAULT 0 COMMENT '发布状态：0-草稿，1-已发布，2-已下线',
  `is_section_home`  TINYINT NOT NULL DEFAULT 0 COMMENT '是否分区首页：1-是，0-否',
  `published_at`     DATETIME DEFAULT NULL COMMENT '发布时间',
  `sort_no`          INT NOT NULL DEFAULT 0 COMMENT '排序号',
  `view_count`       BIGINT NOT NULL DEFAULT 0 COMMENT '浏览量',
  `created_by`       BIGINT DEFAULT NULL COMMENT '创建人管理员ID',
  `created_at`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `published_home_language_section` VARCHAR(64) GENERATED ALWAYS AS (CASE WHEN `publish_status` = 1 AND `is_section_home` = 1 THEN CONCAT(`language`, ':', `section`) ELSE NULL END) STORED,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_language_slug` (`language`, `slug`),
  UNIQUE KEY `uk_published_home_language_section` (`published_home_language_section`),
  KEY `idx_language_section_category_status_sort` (`language`, `section`, `category_id`, `publish_status`, `sort_no`),
  KEY `idx_language_section_home_status` (`language`, `section`, `is_section_home`, `publish_status`),
  KEY `idx_publish_status` (`publish_status`),
  KEY `idx_published_at` (`published_at`),
  CONSTRAINT `fk_doc_article_category` FOREIGN KEY (`category_id`) REFERENCES `doc_category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档文章表';

CREATE TABLE `region_translation` (
  `id`          BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `region_id`   BIGINT NOT NULL COMMENT '地区ID',
  `locale`      VARCHAR(16) NOT NULL COMMENT '语言：zh-CN/en-US',
  `region_name` VARCHAR(64) NOT NULL COMMENT '地区展示名称',
  `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_region_translation_locale` (`region_id`, `locale`),
  KEY `idx_region_translation_locale` (`locale`),
  CONSTRAINT `fk_region_translation_region` FOREIGN KEY (`region_id`) REFERENCES `region` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='地区多语言表';

CREATE TABLE `gpu_model_translation` (
  `id`           BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `gpu_model_id` BIGINT NOT NULL COMMENT 'GPU型号ID',
  `locale`       VARCHAR(16) NOT NULL COMMENT '语言：zh-CN/en-US',
  `model_name`   VARCHAR(64) NOT NULL COMMENT 'GPU型号展示名称',
  `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_gpu_model_translation_locale` (`gpu_model_id`, `locale`),
  KEY `idx_gpu_model_translation_locale` (`locale`),
  CONSTRAINT `fk_gpu_model_translation_model` FOREIGN KEY (`gpu_model_id`) REFERENCES `gpu_model` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='GPU型号多语言表';

CREATE TABLE `product_translation` (
  `id`           BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `product_id`   BIGINT NOT NULL COMMENT '商品ID',
  `locale`       VARCHAR(16) NOT NULL COMMENT '语言：zh-CN/en-US',
  `product_name` VARCHAR(128) NOT NULL COMMENT '商品展示名称',
  `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_translation_locale` (`product_id`, `locale`),
  KEY `idx_product_translation_locale` (`locale`),
  CONSTRAINT `fk_product_translation_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品多语言表';

CREATE TABLE `ai_model_translation` (
  `id`          BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ai_model_id` BIGINT NOT NULL COMMENT 'AI模型ID',
  `locale`      VARCHAR(16) NOT NULL COMMENT '语言：zh-CN/en-US',
  `model_name`  VARCHAR(128) NOT NULL COMMENT 'AI模型展示名称',
  `vendor_name` VARCHAR(64) DEFAULT NULL COMMENT '厂商展示名称',
  `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_model_translation_locale` (`ai_model_id`, `locale`),
  KEY `idx_ai_model_translation_locale` (`locale`),
  CONSTRAINT `fk_ai_model_translation_model` FOREIGN KEY (`ai_model_id`) REFERENCES `ai_model` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI模型多语言表';

CREATE TABLE `rental_cycle_rule_translation` (
  `id`            BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `cycle_rule_id` BIGINT NOT NULL COMMENT '租赁周期规则ID',
  `locale`        VARCHAR(16) NOT NULL COMMENT '语言：zh-CN/en-US',
  `cycle_name`    VARCHAR(64) NOT NULL COMMENT '周期展示名称',
  `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cycle_rule_translation_locale` (`cycle_rule_id`, `locale`),
  KEY `idx_cycle_rule_translation_locale` (`locale`),
  CONSTRAINT `fk_cycle_rule_translation_rule` FOREIGN KEY (`cycle_rule_id`) REFERENCES `rental_cycle_rule` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租赁周期规则多语言表';

SET FOREIGN_KEY_CHECKS = 1;

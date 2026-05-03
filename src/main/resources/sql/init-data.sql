-- Initial data generated from 算力租赁平台_正式开发版_v1.1.md
SET NAMES utf8mb4;

-- Dev only default admin: user_name=admin, password=admin123. Change password_hash before production use.
INSERT INTO `sys_admin` (`user_name`, `password_hash`, `role`, `status`) VALUES
('admin', '$2a$10$sETkfpRdhwa2bsRlCl7tEe.HniV6X5y4Rl8dnG8Dae0Ht3u6guK7C', 'SUPER_ADMIN', 1)
ON DUPLICATE KEY UPDATE `user_name` = `user_name`;
INSERT INTO `rental_cycle_rule` (`cycle_code`, `cycle_name`, `cycle_days`, `yield_multiplier`, `early_penalty_rate`, `sort_no`, `status`) VALUES
('D7',   '7天',   7,   1.0000, 0.0100, 1, 1),
('D15',  '15天',  15,  1.1000, 0.0100, 2, 1),
('D30',  '1个月', 30,  1.2000, 0.0100, 3, 1),
('D90',  '3个月', 90,  1.3000, 0.0100, 4, 1),
('D180', '6个月', 180, 1.4000, 0.0100, 5, 1),
('D360', '12个月',360, 1.5000, 0.0100, 6, 1);

INSERT INTO `commission_rule` (`level_no`, `commission_rate`, `status`) VALUES
(1, 0.2000, 1),
(2, 0.1000, 1),
(3, 0.0500, 1);

INSERT INTO `recharge_channel` (
  `channel_code`, `channel_name`, `network`, `display_url`, `account_name`, `account_no`,
  `min_amount`, `max_amount`, `fee_rate`, `sort_no`, `status`
) VALUES
('USDT_TRC20', 'USDT-TRC20', 'TRC20', NULL, 'Configure receiving account', 'Configure TRC20 receiving address', 100.00000000, 100000.00000000, 0.00000000, 1, 0),
('USDT_ERC20', 'USDT-ERC20', 'ERC20', NULL, 'Configure receiving account', 'Configure ERC20 receiving address', 100.00000000, 100000.00000000, 0.00000000, 2, 0),
('USDT_BEP20', 'USDT-BEP20', 'BEP20', NULL, 'Configure receiving account', 'Configure BEP20 receiving address', 100.00000000, 100000.00000000, 0.00000000, 3, 0)
ON DUPLICATE KEY UPDATE
  `channel_name` = VALUES(`channel_name`),
  `network` = VALUES(`network`),
  `display_url` = VALUES(`display_url`),
  `account_name` = VALUES(`account_name`),
  `account_no` = VALUES(`account_no`),
  `min_amount` = VALUES(`min_amount`),
  `max_amount` = VALUES(`max_amount`),
  `fee_rate` = VALUES(`fee_rate`),
  `sort_no` = VALUES(`sort_no`);

INSERT INTO `ai_model` (
  `model_code`, `model_name`, `vendor_name`, `logo_url`,
  `monthly_token_consumption_trillion`, `token_unit_price`, `deploy_tech_fee`,
  `status`, `sort_no`
) VALUES
('GPT_5_4', 'GPT-5.4', 'OpenAI', NULL, NULL, 0.00000000, 0.00000000, 1, 1),
('GPT_5_4_MINI', 'GPT-5.4 mini', 'OpenAI', NULL, NULL, 0.00000000, 0.00000000, 1, 2),
('GPT_5_3_CODEX', 'GPT-5.3 Codex', 'OpenAI', NULL, NULL, 0.00000000, 0.00000000, 1, 3),
('CLAUDE_OPUS_4_5', 'Claude Opus 4.5', 'Anthropic', NULL, NULL, 0.00000000, 0.00000000, 1, 4),
('CLAUDE_SONNET_4_5', 'Claude Sonnet 4.5', 'Anthropic', NULL, NULL, 0.00000000, 0.00000000, 1, 5),
('CLAUDE_HAIKU_4_5', 'Claude Haiku 4.5', 'Anthropic', NULL, NULL, 0.00000000, 0.00000000, 1, 6),
('GEMINI_3_PRO', 'Gemini 3 Pro', 'Google', NULL, NULL, 0.00000000, 0.00000000, 1, 7),
('GEMINI_2_5_PRO', 'Gemini 2.5 Pro', 'Google', NULL, NULL, 0.00000000, 0.00000000, 1, 8),
('GEMINI_2_5_FLASH', 'Gemini 2.5 Flash', 'Google', NULL, NULL, 0.00000000, 0.00000000, 1, 9),
('DEEPSEEK_V3', 'DeepSeek-V3', 'DeepSeek', NULL, NULL, 0.00000000, 0.00000000, 1, 10),
('DEEPSEEK_R1', 'DeepSeek-R1', 'DeepSeek', NULL, NULL, 0.00000000, 0.00000000, 1, 11),
('QWEN3_MAX', 'Qwen3-Max', 'Alibaba Cloud', NULL, NULL, 0.00000000, 0.00000000, 1, 12),
('QWEN3_MAX_THINKING', 'Qwen3-Max-Thinking', 'Alibaba Cloud', NULL, NULL, 0.00000000, 0.00000000, 1, 13),
('QWEN3_235B_A22B', 'Qwen3-235B-A22B', 'Alibaba Cloud', NULL, NULL, 0.00000000, 0.00000000, 1, 14),
('KIMI_K2', 'Kimi K2', 'Moonshot AI', NULL, NULL, 0.00000000, 0.00000000, 1, 15),
('KIMI_K2_THINKING', 'Kimi K2 Thinking', 'Moonshot AI', NULL, NULL, 0.00000000, 0.00000000, 1, 16),
('GLM_4_5', 'GLM-4.5', 'Zhipu AI', NULL, NULL, 0.00000000, 0.00000000, 1, 17),
('GLM_4_5_AIR', 'GLM-4.5-Air', 'Zhipu AI', NULL, NULL, 0.00000000, 0.00000000, 1, 18),
('ERNIE_4_5', 'ERNIE 4.5', 'Baidu', NULL, NULL, 0.00000000, 0.00000000, 1, 19),
('GROK_4_20', 'Grok 4.20', 'xAI', NULL, NULL, 0.00000000, 0.00000000, 1, 20),
('GROK_4_FAST', 'Grok 4 Fast', 'xAI', NULL, NULL, 0.00000000, 0.00000000, 1, 21),
('LLAMA_4_MAVERICK', 'Llama 4 Maverick', 'Meta', NULL, NULL, 0.00000000, 0.00000000, 1, 22),
('LLAMA_4_SCOUT', 'Llama 4 Scout', 'Meta', NULL, NULL, 0.00000000, 0.00000000, 1, 23),
('MISTRAL_LARGE_3', 'Mistral Large 3', 'Mistral AI', NULL, NULL, 0.00000000, 0.00000000, 1, 24),
('MISTRAL_MEDIUM_3', 'Mistral Medium 3', 'Mistral AI', NULL, NULL, 0.00000000, 0.00000000, 1, 25)
ON DUPLICATE KEY UPDATE
  `model_name` = VALUES(`model_name`),
  `vendor_name` = VALUES(`vendor_name`),
  `logo_url` = VALUES(`logo_url`),
  `monthly_token_consumption_trillion` = VALUES(`monthly_token_consumption_trillion`),
  `token_unit_price` = VALUES(`token_unit_price`),
  `deploy_tech_fee` = VALUES(`deploy_tech_fee`),
  `status` = VALUES(`status`),
  `sort_no` = VALUES(`sort_no`);

INSERT INTO `blog_category` (`category_name`, `sort_no`, `status`)
SELECT seed.`category_name`, seed.`sort_no`, seed.`status`
FROM (
  SELECT 'GPU选型' AS `category_name`, 1 AS `sort_no`, 1 AS `status`
  UNION ALL SELECT 'Token收益', 2, 1
  UNION ALL SELECT 'AI部署', 3, 1
  UNION ALL SELECT '算力市场', 4, 1
  UNION ALL SELECT '运维实践', 5, 1
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM `blog_category` c WHERE c.`category_name` = seed.`category_name`
);

INSERT INTO `blog_tag` (`tag_name`, `sort_no`, `status`) VALUES
('RTX 5090', 1, 1),
('RTX 4090', 2, 1),
('A800', 3, 1),
('A100', 4, 1),
('H20', 5, 1),
('RTX PRO 6000', 6, 1),
('AI训练', 7, 1),
('AI推理', 8, 1),
('Token收益', 9, 1),
('模型部署', 10, 1),
('算力租赁', 11, 1),
('成本优化', 12, 1)
ON DUPLICATE KEY UPDATE
  `sort_no` = VALUES(`sort_no`),
  `status` = VALUES(`status`);

INSERT INTO `blog_post` (`category_id`, `title`, `summary`, `cover_image_url`, `content_markdown`, `publish_status`, `published_at`, `is_top`, `sort_no`, `view_count`, `created_by`)
SELECT (SELECT `id` FROM `blog_category` WHERE `category_name` = 'GPU选型' ORDER BY `id` LIMIT 1),
       'RTX 5090 适合哪些 AI 场景：从显存、吞吐到租赁周期的完整判断',
       '面向个人开发者、小团队和中小型推理服务，梳理 RTX 5090 在本地微调、图像生成、视频生成和轻量推理中的适用边界，并给出租赁时长和成本评估方法。',
       NULL,
       '# RTX 5090 适合哪些 AI 场景\n\nRTX 5090 的核心价值不只是单卡峰值性能，而是较新的 CUDA 生态、32GB 级显存和较好的能耗比组合。对租赁用户来说，它更适合需求明确、并发规模可控、希望快速验证模型或内容生产工作流的场景。\n\n## 适合的任务\n\n- LoRA 微调、轻量 SFT 和小批量评测。\n- Stable Diffusion、视频生成、3D 资产生成等内容生产任务。\n- 中小参数量模型的量化推理和 Agent 原型验证。\n- 需要较高单卡性能但暂时不需要 80GB 显存的研发任务。\n\n## 不适合的任务\n\n如果任务长期依赖超大上下文、全参数训练或大 batch 推理，32GB 显存可能成为主要约束。此时应优先评估 A800、H20 或更高显存的企业级卡，而不是单纯追求单卡新型号。\n\n## 租赁建议\n\n短期实验建议先租 7 天到 15 天，用真实数据跑通环境、吞吐、失败重试和成本模型。确认业务稳定后再切换 30 天以上周期，避免在需求未固化时锁定过长资源。\n\n## 成本评估公式\n\n建议按任务拆成三层：GPU 租赁成本、工程调试时间、Token 或内容产出价值。只有当产出价值能覆盖前两项，并且任务可以持续排队运行时，长周期租赁才更划算。',
       1, '2026-04-20 09:00:00', 1, 1, 0,
       (SELECT `id` FROM `sys_admin` WHERE `user_name` = 'admin' LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM `blog_post` WHERE `title` = 'RTX 5090 适合哪些 AI 场景：从显存、吞吐到租赁周期的完整判断');

INSERT INTO `blog_post` (`category_id`, `title`, `summary`, `cover_image_url`, `content_markdown`, `publish_status`, `published_at`, `is_top`, `sort_no`, `view_count`, `created_by`)
SELECT (SELECT `id` FROM `blog_category` WHERE `category_name` = 'GPU选型' ORDER BY `id` LIMIT 1),
       'RTX 4090 与 RTX 4090D：AI 推理和内容生成的性价比怎么选',
       '从显存容量、生态兼容、任务排队和单位产出成本出发，说明 RTX 4090 系列为什么仍然是 AI 内容生产和轻量推理中的热门选择。',
       NULL,
       '# RTX 4090 与 RTX 4090D 怎么选\n\nRTX 4090 系列的优势在于生态成熟、单卡性能强、租赁价格通常比企业级大显存卡更容易接受。它适合对显存要求没有超过 24GB，但对迭代速度和 CUDA 兼容性要求较高的团队。\n\n## 典型使用场景\n\n- 文生图、图生图、批量出图和视频生成前处理。\n- 7B 到 14B 级模型的量化推理。\n- 小规模 Embedding、RAG 评测和 Agent 工具链测试。\n- 需要快速交付 Demo 的产品验证。\n\n## 选择标准\n\n如果任务主要看单卡吞吐和稳定驱动，优先看机器的 CUDA 版本、驱动版本和缓存优化状态。显卡型号重要，但实际体验还会受到 CPU、内存、磁盘和镜像环境影响。\n\n## 什么时候升级\n\n当模型频繁 OOM、上下文长度被迫降低、或同一任务需要多卡并行时，就应该评估 A800、H20 或 vGPU 大显存实例。不要为了节省单价而让工程时间持续消耗。',
       1, '2026-04-19 09:00:00', 0, 2, 0,
       (SELECT `id` FROM `sys_admin` WHERE `user_name` = 'admin' LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM `blog_post` WHERE `title` = 'RTX 4090 与 RTX 4090D：AI 推理和内容生成的性价比怎么选');

INSERT INTO `blog_post` (`category_id`, `title`, `summary`, `cover_image_url`, `content_markdown`, `publish_status`, `published_at`, `is_top`, `sort_no`, `view_count`, `created_by`)
SELECT (SELECT `id` FROM `blog_category` WHERE `category_name` = 'GPU选型' ORDER BY `id` LIMIT 1),
       'A800、A100 与 H20：大显存训练和企业推理的选型思路',
       '企业级 GPU 的核心差异不止是算力，还包括显存、互联、稳定性和可持续租赁能力。本文帮助用户按训练、推理和数据规模进行选型。',
       NULL,
       '# A800、A100 与 H20 的选型思路\n\n企业级 GPU 更适合长任务、多人共享任务和对稳定性要求高的生产环境。相比消费级卡，A800、A100、H20 这类资源的价值主要体现在大显存、更好的数据中心适配和更稳定的长时间运行能力。\n\n## 大显存的价值\n\n大显存可以减少切分、卸载和量化带来的工程复杂度。对于长上下文推理、较大 batch、复杂训练管线和多任务并发，大显存通常比单纯的峰值算力更关键。\n\n## 训练任务优先看什么\n\n训练任务优先看显存容量、显存带宽、多卡通信和磁盘读写。单卡价格低并不代表总成本低，反复 OOM、频繁重启和工程等待都会扩大真实成本。\n\n## 推理任务优先看什么\n\n推理任务要看稳定吞吐、并发、延迟和冷启动。对需要持续服务的 API 场景，建议用小流量压测确认 P95 延迟，再决定是否扩容。\n\n## 租赁建议\n\n研发早期可以用 7 天周期验证模型和数据，进入生产前再切到 30 天或更长周期。企业任务应保留监控、日志和镜像版本，降低迁移成本。',
       1, '2026-04-18 09:00:00', 0, 3, 0,
       (SELECT `id` FROM `sys_admin` WHERE `user_name` = 'admin' LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM `blog_post` WHERE `title` = 'A800、A100 与 H20：大显存训练和企业推理的选型思路');

INSERT INTO `blog_post` (`category_id`, `title`, `summary`, `cover_image_url`, `content_markdown`, `publish_status`, `published_at`, `is_top`, `sort_no`, `view_count`, `created_by`)
SELECT (SELECT `id` FROM `blog_category` WHERE `category_name` = 'Token收益' ORDER BY `id` LIMIT 1),
       'Token 收益怎么估算：把 GPU 产出、单价和周期收益讲清楚',
       '围绕 Token 产出、Token 单价、周期倍率和租赁成本，拆解用户在下单前应该如何估算收益，并提醒常见误区。',
       NULL,
       '# Token 收益怎么估算\n\nToken 收益不是只看显卡型号，也不是只看每日产出。合理估算需要把 GPU 产出、Token 单价、租赁周期、平台规则和风险折扣放在同一个模型里。\n\n## 基础公式\n\n可先用简单公式建立直觉：每日基础收益 = 每日 Token 产出 × Token 单价。周期收益 = 每日基础收益 × 天数 × 周期倍率。最终还要扣除租赁成本、手续费和可能的提前解约成本。\n\n## 关键变量\n\n- 每日 Token 产出是否稳定。\n- Token 单价是否会随市场和平台策略变化。\n- 周期倍率是否足以覆盖锁定期风险。\n- 机器是否有足够库存和稳定运行记录。\n\n## 常见误区\n\n不要用最高产出日作为长期平均值，也不要忽略停机、排队、环境配置和策略变化带来的折扣。更稳妥的方式是先用保守产出做测算，再用实际运行数据修正。\n\n## 实操建议\n\n新用户建议先跑短周期，观察收益入账、机器稳定性和提现路径。只有当数据连续稳定后，再考虑提高金额或延长租赁周期。',
       1, '2026-04-17 09:00:00', 1, 4, 0,
       (SELECT `id` FROM `sys_admin` WHERE `user_name` = 'admin' LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM `blog_post` WHERE `title` = 'Token 收益怎么估算：把 GPU 产出、单价和周期收益讲清楚');

INSERT INTO `blog_post` (`category_id`, `title`, `summary`, `cover_image_url`, `content_markdown`, `publish_status`, `published_at`, `is_top`, `sort_no`, `view_count`, `created_by`)
SELECT (SELECT `id` FROM `blog_category` WHERE `category_name` = 'Token收益' ORDER BY `id` LIMIT 1),
       '租赁周期怎么选：7 天、30 天和 90 天的收益风险对比',
       '不同租赁周期对应不同试错成本、收益倍率和流动性风险。本文给出新手、稳定用户和团队用户的周期选择建议。',
       NULL,
       '# 租赁周期怎么选\n\n租赁周期本质上是在收益倍率和灵活性之间取舍。周期越长，通常越适合稳定任务；周期越短，越适合试错和验证。\n\n## 7 天周期\n\n适合新用户、首次验证机器、测试 API 部署或观察收益波动。优点是风险小，缺点是倍率优势有限，且需要更频繁地做续租决策。\n\n## 30 天周期\n\n适合已经确认模型、环境和收益路径的用户。它在灵活性和收益稳定之间比较平衡，也是多数团队从测试走向稳定运行的常用周期。\n\n## 90 天及以上周期\n\n适合有明确长期任务、现金流计划和运维能力的用户。选择长周期前，应确认提现、收益结算、提前解约规则和机器稳定性。\n\n## 决策建议\n\n不要只看倍率。更合理的做法是先用短周期拿到真实数据，再用真实日均收益评估长周期是否值得。',
       1, '2026-04-16 09:00:00', 0, 5, 0,
       (SELECT `id` FROM `sys_admin` WHERE `user_name` = 'admin' LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM `blog_post` WHERE `title` = '租赁周期怎么选：7 天、30 天和 90 天的收益风险对比');

INSERT INTO `blog_post` (`category_id`, `title`, `summary`, `cover_image_url`, `content_markdown`, `publish_status`, `published_at`, `is_top`, `sort_no`, `view_count`, `created_by`)
SELECT (SELECT `id` FROM `blog_category` WHERE `category_name` = 'AI部署' ORDER BY `id` LIMIT 1),
       'AI 模型 API 部署前要准备什么：从镜像、密钥到限流策略',
       '面向希望把模型能力变成 API 的用户，梳理部署前需要确认的镜像环境、模型文件、访问密钥、限流、日志和计费边界。',
       NULL,
       '# AI 模型 API 部署前要准备什么\n\n把模型跑起来只是第一步，把模型稳定地作为 API 提供服务需要额外准备环境、权限、安全和观测能力。部署前准备越充分，后续排障成本越低。\n\n## 环境准备\n\n确认 CUDA、驱动、Python、推理框架和模型权重版本。建议把环境固定成镜像或启动脚本，避免重启后依赖漂移。\n\n## 密钥管理\n\nAPI Token 应独立生成、加密存储，并设置明确的使用边界。不要把管理员账号、数据库密码或第三方密钥写入日志和前端配置。\n\n## 限流和稳定性\n\n生产接口需要限流、超时、重试和错误码。对大模型接口来说，单次请求可能占用较长 GPU 时间，没有限流很容易导致队列被少数请求打满。\n\n## 上线检查\n\n上线前至少跑一次并发压测，确认平均延迟、P95 延迟、显存峰值和失败率。只有监控数据可解释，API 才适合对外开放。',
       1, '2026-04-15 09:00:00', 0, 6, 0,
       (SELECT `id` FROM `sys_admin` WHERE `user_name` = 'admin' LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM `blog_post` WHERE `title` = 'AI 模型 API 部署前要准备什么：从镜像、密钥到限流策略');

INSERT INTO `blog_post` (`category_id`, `title`, `summary`, `cover_image_url`, `content_markdown`, `publish_status`, `published_at`, `is_top`, `sort_no`, `view_count`, `created_by`)
SELECT (SELECT `id` FROM `blog_category` WHERE `category_name` = 'AI部署' ORDER BY `id` LIMIT 1),
       'RAG、Agent 与多模态应用：不同 AI 产品该租哪类算力',
       '把常见 AI 产品拆成 RAG 检索问答、Agent 自动化、多模态生成三类，分别说明算力需求、显存压力和扩容方式。',
       NULL,
       '# 不同 AI 产品该租哪类算力\n\nAI 产品的算力需求差异很大。RAG、Agent 和多模态生成看起来都属于 AI 应用，但瓶颈可能分别在检索、推理延迟、上下文长度或图像视频生成速度。\n\n## RAG 应用\n\nRAG 更关注 Embedding、向量检索和回答延迟。早期可以选择中等显存机器验证链路，真正上线后再根据并发扩容推理实例。\n\n## Agent 应用\n\nAgent 通常请求次数多、工具调用链长，更需要稳定的 API 服务、请求追踪和错误恢复。GPU 选型要看并发和响应时间，而不是只看单次推理速度。\n\n## 多模态生成\n\n图像和视频生成更依赖显存和持续吞吐。任务可以排队时，消费级高性能卡往往性价比好；需要高并发时，再考虑多卡或企业级资源。\n\n## 总结\n\n先判断业务瓶颈，再选择 GPU。把产品类型、并发规模和任务时长写清楚，选型会比单纯比较显卡参数更可靠。',
       1, '2026-04-14 09:00:00', 0, 7, 0,
       (SELECT `id` FROM `sys_admin` WHERE `user_name` = 'admin' LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM `blog_post` WHERE `title` = 'RAG、Agent 与多模态应用：不同 AI 产品该租哪类算力');

INSERT INTO `blog_post` (`category_id`, `title`, `summary`, `cover_image_url`, `content_markdown`, `publish_status`, `published_at`, `is_top`, `sort_no`, `view_count`, `created_by`)
SELECT (SELECT `id` FROM `blog_category` WHERE `category_name` = '算力市场' ORDER BY `id` LIMIT 1),
       '为什么算力租赁适合 AI 初创团队：现金流、弹性和试错速度',
       '从现金流管理、技术验证、突发需求和团队协作角度，说明为什么租赁 GPU 往往比一次性采购更适合早期 AI 团队。',
       NULL,
       '# 为什么算力租赁适合 AI 初创团队\n\nAI 初创团队最稀缺的往往不是单次硬件投入能力，而是试错速度和现金流弹性。算力租赁可以把固定资产投入变成按需使用，让团队把更多资金放在产品和客户验证上。\n\n## 降低前期投入\n\n采购 GPU 需要一次性支出、机房、电力、网络和维护能力。租赁模式可以先用小规模资源验证方向，减少因路线调整造成的沉没成本。\n\n## 提高试错速度\n\n不同模型、框架和业务形态对算力需求不同。租赁可以快速切换卡型、区域和周期，帮助团队用真实任务验证假设。\n\n## 应对峰值需求\n\n产品发布、活动、客户测试和数据处理常常带来短期峰值。临时扩容比长期持有闲置机器更经济。\n\n## 管理建议\n\n团队应建立资源使用台账，记录每台机器的任务、成本、收益和负责人。算力是生产资料，只有被持续度量，才能真正优化。',
       1, '2026-04-13 09:00:00', 0, 8, 0,
       (SELECT `id` FROM `sys_admin` WHERE `user_name` = 'admin' LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM `blog_post` WHERE `title` = '为什么算力租赁适合 AI 初创团队：现金流、弹性和试错速度');

INSERT INTO `blog_post` (`category_id`, `title`, `summary`, `cover_image_url`, `content_markdown`, `publish_status`, `published_at`, `is_top`, `sort_no`, `view_count`, `created_by`)
SELECT (SELECT `id` FROM `blog_category` WHERE `category_name` = '运维实践' ORDER BY `id` LIMIT 1),
       'GPU 任务稳定运行指南：驱动、CUDA、显存和日志怎么排查',
       '总结 GPU 租赁中最常见的运行问题，包括 CUDA 版本不匹配、显存溢出、磁盘不足、进程残留和日志缺失，并给出排查顺序。',
       NULL,
       '# GPU 任务稳定运行指南\n\nGPU 任务失败并不一定是机器问题，更多时候来自环境版本、数据路径、显存峰值或脚本异常。建立固定排查顺序，可以大幅减少定位时间。\n\n## 先看环境\n\n确认驱动版本、CUDA 版本、框架版本和镜像依赖是否匹配。升级依赖前应记录可回滚版本，避免任务跑到一半才发现兼容性问题。\n\n## 再看资源\n\n使用系统工具观察显存、GPU 利用率、CPU、内存和磁盘。显存持续接近上限时，应降低 batch、开启量化或换更大显存实例。\n\n## 最后看日志\n\n训练和推理任务都要输出关键日志，包括模型版本、参数、数据集路径、启动时间、异常堆栈和产物目录。没有日志的任务很难稳定运营。\n\n## 长任务建议\n\n长任务应支持断点续跑，输出中间 checkpoint，并把关键结果同步到持久化存储。只依赖本地临时目录会放大故障损失。',
       1, '2026-04-12 09:00:00', 0, 9, 0,
       (SELECT `id` FROM `sys_admin` WHERE `user_name` = 'admin' LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM `blog_post` WHERE `title` = 'GPU 任务稳定运行指南：驱动、CUDA、显存和日志怎么排查');

INSERT INTO `blog_post` (`category_id`, `title`, `summary`, `cover_image_url`, `content_markdown`, `publish_status`, `published_at`, `is_top`, `sort_no`, `view_count`, `created_by`)
SELECT (SELECT `id` FROM `blog_category` WHERE `category_name` = '算力市场' ORDER BY `id` LIMIT 1),
       '从模型训练到推理服务：算力成本优化的 6 个关键动作',
       '围绕任务拆分、卡型匹配、批处理、缓存、监控和周期管理，给出降低 GPU 租赁成本的实操建议。',
       NULL,
       '# 算力成本优化的 6 个关键动作\n\n算力成本优化不是一味选择最低单价，而是让每一小时 GPU 时间都产生有效产出。以下 6 个动作适合训练、推理和内容生成团队长期执行。\n\n## 1. 任务拆分\n\n把探索、训练、评测和部署分开。探索阶段用短周期，稳定阶段再上长周期，避免把不确定性锁进长期成本。\n\n## 2. 卡型匹配\n\n显存够用时，优先选择性价比卡；显存不足时，直接换大显存实例。长期使用工程绕路规避 OOM，通常比升级卡型更贵。\n\n## 3. 批处理\n\n能排队的任务尽量批处理，让 GPU 保持高利用率。低利用率长时间运行，是最常见的隐性浪费。\n\n## 4. 缓存复用\n\n模型权重、数据预处理结果、Embedding 和中间产物都应缓存。重复下载和重复预处理会占用大量无效时间。\n\n## 5. 监控告警\n\n记录 GPU 利用率、显存、任务耗时、失败率和单位产出成本。没有监控，就无法判断成本是否真的下降。\n\n## 6. 周期管理\n\n用短周期验证，用长周期承接稳定任务。周期策略应跟业务确定性绑定，而不是只被倍率吸引。',
       1, '2026-04-11 09:00:00', 0, 10, 0,
       (SELECT `id` FROM `sys_admin` WHERE `user_name` = 'admin' LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM `blog_post` WHERE `title` = '从模型训练到推理服务：算力成本优化的 6 个关键动作');

INSERT INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.`id`, t.`id` FROM `blog_post` p JOIN `blog_tag` t ON t.`tag_name` IN ('RTX 5090', 'AI训练', 'AI推理', '算力租赁')
WHERE p.`title` = 'RTX 5090 适合哪些 AI 场景：从显存、吞吐到租赁周期的完整判断'
  AND NOT EXISTS (SELECT 1 FROM `blog_post_tag` pt WHERE pt.`post_id` = p.`id` AND pt.`tag_id` = t.`id`);
INSERT INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.`id`, t.`id` FROM `blog_post` p JOIN `blog_tag` t ON t.`tag_name` IN ('RTX 4090', 'AI推理', '成本优化', '算力租赁')
WHERE p.`title` = 'RTX 4090 与 RTX 4090D：AI 推理和内容生成的性价比怎么选'
  AND NOT EXISTS (SELECT 1 FROM `blog_post_tag` pt WHERE pt.`post_id` = p.`id` AND pt.`tag_id` = t.`id`);
INSERT INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.`id`, t.`id` FROM `blog_post` p JOIN `blog_tag` t ON t.`tag_name` IN ('A800', 'A100', 'H20', 'AI训练', 'AI推理')
WHERE p.`title` = 'A800、A100 与 H20：大显存训练和企业推理的选型思路'
  AND NOT EXISTS (SELECT 1 FROM `blog_post_tag` pt WHERE pt.`post_id` = p.`id` AND pt.`tag_id` = t.`id`);
INSERT INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.`id`, t.`id` FROM `blog_post` p JOIN `blog_tag` t ON t.`tag_name` IN ('Token收益', '成本优化', '算力租赁')
WHERE p.`title` = 'Token 收益怎么估算：把 GPU 产出、单价和周期收益讲清楚'
  AND NOT EXISTS (SELECT 1 FROM `blog_post_tag` pt WHERE pt.`post_id` = p.`id` AND pt.`tag_id` = t.`id`);
INSERT INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.`id`, t.`id` FROM `blog_post` p JOIN `blog_tag` t ON t.`tag_name` IN ('Token收益', '成本优化', '算力租赁')
WHERE p.`title` = '租赁周期怎么选：7 天、30 天和 90 天的收益风险对比'
  AND NOT EXISTS (SELECT 1 FROM `blog_post_tag` pt WHERE pt.`post_id` = p.`id` AND pt.`tag_id` = t.`id`);
INSERT INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.`id`, t.`id` FROM `blog_post` p JOIN `blog_tag` t ON t.`tag_name` IN ('模型部署', 'AI推理', '成本优化')
WHERE p.`title` = 'AI 模型 API 部署前要准备什么：从镜像、密钥到限流策略'
  AND NOT EXISTS (SELECT 1 FROM `blog_post_tag` pt WHERE pt.`post_id` = p.`id` AND pt.`tag_id` = t.`id`);
INSERT INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.`id`, t.`id` FROM `blog_post` p JOIN `blog_tag` t ON t.`tag_name` IN ('模型部署', 'AI推理', 'AI训练')
WHERE p.`title` = 'RAG、Agent 与多模态应用：不同 AI 产品该租哪类算力'
  AND NOT EXISTS (SELECT 1 FROM `blog_post_tag` pt WHERE pt.`post_id` = p.`id` AND pt.`tag_id` = t.`id`);
INSERT INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.`id`, t.`id` FROM `blog_post` p JOIN `blog_tag` t ON t.`tag_name` IN ('算力租赁', '成本优化', 'AI训练')
WHERE p.`title` = '为什么算力租赁适合 AI 初创团队：现金流、弹性和试错速度'
  AND NOT EXISTS (SELECT 1 FROM `blog_post_tag` pt WHERE pt.`post_id` = p.`id` AND pt.`tag_id` = t.`id`);
INSERT INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.`id`, t.`id` FROM `blog_post` p JOIN `blog_tag` t ON t.`tag_name` IN ('模型部署', 'AI训练', 'AI推理')
WHERE p.`title` = 'GPU 任务稳定运行指南：驱动、CUDA、显存和日志怎么排查'
  AND NOT EXISTS (SELECT 1 FROM `blog_post_tag` pt WHERE pt.`post_id` = p.`id` AND pt.`tag_id` = t.`id`);
INSERT INTO `blog_post_tag` (`post_id`, `tag_id`)
SELECT p.`id`, t.`id` FROM `blog_post` p JOIN `blog_tag` t ON t.`tag_name` IN ('成本优化', '模型部署', '算力租赁')
WHERE p.`title` = '从模型训练到推理服务：算力成本优化的 6 个关键动作'
  AND NOT EXISTS (SELECT 1 FROM `blog_post_tag` pt WHERE pt.`post_id` = p.`id` AND pt.`tag_id` = t.`id`);

INSERT INTO `sys_config` (`config_key`, `config_value`, `config_desc`) VALUES
('withdraw.min_amount',              '10',     '最低提现金额 USDT'),
('withdraw.fee_free_threshold',      '100',    '免手续费门槛 USDT，大于等于此金额免费'),
('withdraw.fee_rate',                '0.05',   '提现手续费比例，小于门槛时收取（5%）'),
('withdraw.max_daily_amount',        '100000', '每日累计提现上限 USDT'),
('recharge.min_amount',              '500',    '全局最低充值金额 USDT'),
('order.activation_timeout_minutes', '60',     '待激活订单超时自动取消时间（分钟），超时退还机器费'),
('order.pending_activation_timeout_minutes', '60', '待激活订单超时自动取消时间（分钟），兼容当前阶段配置键'),
('email_code.rate_limit_per_minute', '5',      '邮箱验证码每分钟最大发送次数');

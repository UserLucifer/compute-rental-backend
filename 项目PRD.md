# 算力租赁平台 PRD

版本：v1.0-current  
日期：2026-05-01  
依据：当前后端代码、数据库脚本、`算力租赁平台_正式开发版_v1.1.md`  
范围：用户端、后台端、钱包财务、算力租赁订单、API 激活、收益、佣金、结算、提现、通知、博客、调度任务

## 1. 项目定位

本项目是一个基于 USDT 钱包的算力租赁平台。用户注册后充值 USDT，选择 GPU 产品、AI 模型和租赁周期，下单支付机器费用。支付成功后系统生成 API 凭证，用户再支付 API 部署费进入激活流程。激活 24 小时后系统自动暂停，用户手动启动后才开始产生每日收益。订单到期或用户提前结算后退还本金或扣除违约金后退还本金。收益入账后按推荐关系生成三级佣金，用户最终可申请提现。

系统同时提供后台能力：用户管理、钱包与流水查询、充值提现审核、产品与规则管理、订单与收益查询、系统配置、通知、博客、看板和定时任务手动触发。

## 2. 角色与权限

| 角色 | 说明 | 关键权限 |
|---|---|---|
| 游客 | 未登录用户 | 查看公开博客、健康检查、注册/登录、查看公开接口文档 |
| 前台用户 | 已登录普通用户，JWT `USER` | 用户资料、钱包、充值、提现、产品浏览、租赁下单、支付、启动、提前结算、收益/佣金/团队查询 |
| 后台管理员 | 已登录后台用户，JWT `ADMIN` 或 `SUPER_ADMIN` | 后台看板、用户与业务查询、充值提现审核、产品配置、系统配置、通知、博客、调度任务 |
| 定时任务 | Spring Scheduler + Redis Lock | 激活超时取消、自动暂停、每日收益、到期结算、佣金生成 |

权限口径：
- `/api/auth/**`、`/api/admin/auth/login`、`/api/blog/**`、`/api/system/health`、OpenAPI/Knife4j、WebSocket 为公开入口。
- `/api/admin/**` 需要后台管理员角色。
- 其他接口需要前台用户角色。

## 3. 业务总闭环

```text
注册/登录
→ 创建用户钱包、邀请码、团队关系
→ 用户充值，后台审核入账
→ 用户浏览产品、AI 模型、周期规则并预估收益
→ 用户创建租赁订单
→ 用户支付机器费，钱包扣款，生成 API 凭证
→ 用户支付 API 部署费，钱包扣款，进入 API 激活中
→ 24 小时后定时任务自动暂停
→ 用户手动启动，订单进入运行中
→ 每日收益定时入账
→ 佣金定时发放给上三级推荐人
→ 到期自动结算或用户提前结算
→ 用户提现，后台审核和标记打款
```

闭环核心：
- 所有资金变化必须落到 `wallet_transaction`。
- 订单生命周期由 `rental_order.order_status`、`profit_status`、`settlement_status` 联合表达。
- API 生命周期由 `api_credential.token_status` 表达。
- 结算闭环通过 `rental_settlement_order` 和钱包入账完成。
- 收益闭环通过 `rental_profit_record`、钱包入账和佣金任务完成。

## 4. 用户账号业务

### 4.1 注册流程

入口：
- 发送注册验证码：`POST /api/auth/signup/email-code/send`
- 校验注册验证码：`POST /api/auth/signup/email-code/verify`
- 注册：`POST /api/auth/signup` 或 `POST /api/auth/register`

流程：
```text
用户提交邮箱
→ 系统校验邮箱未注册
→ Redis 频率限制：email_code_rate:{email}:SIGNUP，默认每分钟 5 次
→ 生成验证码，仅保存哈希到 email_verify_code
→ 用户填写邮箱、验证码、用户名、密码、可选邀请码
→ 校验最新未使用且未过期验证码
→ 将验证码状态置为 USED
→ 创建 app_user，状态 ENABLED
→ 创建 user_wallet，币种 USDT，余额为 0
→ 生成邀请码并写 user_referral_relation
→ 如使用邀请码，写直属上级和三级返佣上级
→ 写 user_team_relation 闭包关系，用于无限层团队统计
→ 更新 last_login_at
→ 返回前台 JWT
```

状态变化：

| 对象 | 字段 | 初始 | 触发 | 目标 |
|---|---|---|---|---|
| email_verify_code | status | UNUSED | 注册成功消费验证码 | USED |
| app_user | status | ENABLED | 后台禁用用户 | DISABLED |
| user_wallet | status | ENABLED | 当前代码无用户端变更入口 | ENABLED |

说明：
- `EmailVerifyStatus.EXPIRED` 枚举存在，但当前代码通过 `expire_at < now` 判定过期，没有定时把状态改成 `EXPIRED`。
- 注册与钱包、推荐关系、团队关系在同一事务内完成。

### 4.2 登录流程

入口：
- `POST /api/auth/login`
- `POST /api/auth/login/password`

流程：
```text
用户提交邮箱和密码
→ 查询 app_user
→ 校验用户存在且 status=ENABLED
→ BCrypt 校验 password_hash
→ 更新 last_login_at
→ 返回 JWT 和用户资料
```

闭环：
- 登录成功后，用户可访问钱包、订单、收益、佣金等前台受保护业务。
- 用户被禁用后不能再次登录。

### 4.3 重置密码流程

入口：
- 发送重置验证码：`POST /api/auth/reset-password/email-code/send`
- 校验重置验证码：`POST /api/auth/reset-password/email-code/verify`
- 重置密码：`POST /api/auth/reset-password` 或 `POST /api/auth/password/reset`

状态变化：

| 对象 | 字段 | 初始 | 触发 | 目标 |
|---|---|---|---|---|
| email_verify_code | status | UNUSED | 重置密码成功消费验证码 | USED |
| app_user | password_hash | 旧密码哈希 | 重置成功 | 新 BCrypt 哈希 |

### 4.4 个人资料和设备

入口：
- 当前用户：`GET /api/user/me`
- 更新头像：`PUT /api/user/avatar`
- 注册推送设备：`POST /api/push-devices/register`
- 注销推送设备：`POST /api/push-devices/unregister`

状态变化：

| 对象 | 字段 | 触发 | 目标 |
|---|---|---|---|
| app_user | avatar_key | 用户更新头像 | 新头像 key |
| user_push_device | status | 注册或重新绑定设备 Token | ENABLED |
| user_push_device | status | 注销设备 Token | DISABLED |

## 5. 钱包业务

钱包是所有资金业务的统一入口。每个用户注册时创建一个 USDT 钱包。

核心字段：
- `available_balance`：可用余额。
- `frozen_balance`：冻结余额，主要用于提现审核。
- `total_recharge`：累计充值成功金额。
- `total_withdraw`：累计提现成功金额。
- `total_profit`：累计租赁收益。
- `total_commission`：累计佣金收益。
- `version_no`：乐观锁版本号。

钱包动作：

| 动作 | tx_type | 余额变化 | 典型业务 |
|---|---|---|---|
| 入账 | IN | 可用余额增加 | 充值、收益、佣金、结算退款 |
| 支出 | OUT | 可用余额减少，或冻结余额减少 | 机器费、部署费、提现打款 |
| 冻结 | FREEZE | 可用余额减少，冻结余额增加 | 提现申请 |
| 解冻 | UNFREEZE | 冻结余额减少，可用余额增加 | 提现取消或驳回 |

闭环规则：
- 每一次余额变化都必须写 `wallet_transaction`。
- 钱包变更使用 `version_no` 乐观锁，失败时按余额不足、钱包禁用或并发冲突返回。
- 业务幂等键格式通常为 `{biz_type}:{biz_order_no}:{action}`，防止重复扣款或重复入账。

## 6. 充值业务

### 6.1 充值渠道

后台管理入口：
- 充值渠道列表、创建、更新、删除。

状态：

| 对象 | 字段 | 状态 |
|---|---|---|
| recharge_channel | status | ENABLED / DISABLED |

规则：
- 前台只展示 `status=ENABLED` 的充值渠道。
- 渠道已被充值订单引用后不能删除。
- 渠道可设置 `min_amount`、`max_amount`、手续费率、链网络、收款地址等。

### 6.2 用户充值流程

入口：
- 前台渠道：`GET /api/recharge/channels`
- 创建充值订单：`POST /api/recharge/orders`
- 我的充值订单：`GET /api/recharge/orders`
- 取消充值订单：`POST /api/recharge/orders/{rechargeNo}/cancel`
- 后台审核通过：`POST /api/admin/recharge/orders/{rechargeNo}/approve`
- 后台审核驳回：`POST /api/admin/recharge/orders/{rechargeNo}/reject`

流程：
```text
用户选择启用的充值渠道
→ 输入充值金额、外部交易号、支付凭证、备注
→ 系统校验金额 > 0
→ 校验金额 >= MAX(sys_config.recharge.min_amount, channel.min_amount)
→ 校验 external_tx_no 未重复
→ 创建 recharge_order，status=SUBMITTED
→ 用户可在 SUBMITTED 时取消
→ 后台审核
  → 审核通过：recharge_order.status=APPROVED，钱包 IN 入账，total_recharge 增加
  → 审核驳回：recharge_order.status=REJECTED，不改变钱包
```

状态变化：

| 当前状态 | 触发 | 下一状态 | 钱包影响 | 闭环 |
|---|---|---|---|---|
| SUBMITTED | 用户取消 | CANCELED | 无 | 充值单终止 |
| SUBMITTED | 后台审核通过 | APPROVED | IN，biz_type=RECHARGE | 充值金额进入可用余额 |
| SUBMITTED | 后台审核驳回 | REJECTED | 无 | 充值单终止 |

## 7. 产品与规则业务

后台维护对象：
- 地区 `region`
- GPU 型号 `gpu_model`
- GPU 机器产品 `product`
- AI 模型 `ai_model`
- 租赁周期规则 `rental_cycle_rule`

前台展示入口：
- 地区：`GET /api/regions`
- GPU 型号：`GET /api/gpu-models`
- 产品列表和详情：`GET /api/products`
- AI 模型：`GET /api/ai-models`
- 周期规则：`GET /api/rental-cycle-rules`

规则：
- 前台只展示 `status=ENABLED` 的地区、GPU 型号、产品、AI 模型、周期规则。
- 下单时后端重新读取产品、模型、周期规则，并保存订单快照。
- `product` 库存字段当前只用于展示，不参与真实库存锁和超卖控制。

状态变化：

| 对象 | 字段 | 触发 | 状态变化 |
|---|---|---|---|
| region | status | 后台启用/停用 | ENABLED / DISABLED |
| gpu_model | status | 后台启用/停用 | ENABLED / DISABLED |
| product | status | 后台上架/下架 | ENABLED / DISABLED |
| ai_model | status | 后台启用/停用 | ENABLED / DISABLED |
| rental_cycle_rule | status | 后台启用/停用 | ENABLED / DISABLED |

## 8. 租赁订单与 API 业务

### 8.1 预估收益

入口：`POST /api/rental/estimate`

公式：
```text
expected_daily_profit = product.token_output_per_day
                      × ai_model.token_unit_price
                      × rental_cycle_rule.yield_multiplier

expected_total_profit = expected_daily_profit × cycle_days
```

规则：
- 只使用已启用产品、AI 模型和周期规则。
- 金额使用 `BigDecimal` 并按平台金额规则缩放。

### 8.2 创建租赁订单

入口：`POST /api/rental/orders`

流程：
```text
用户选择 productId、aiModelId、cycleRuleId
→ 后端校验三类资源均启用
→ 读取产品、模型、周期规则、地区、GPU 型号
→ 计算预计每日收益和预计总收益
→ 创建 rental_order
→ order_status=PENDING_PAY
→ profit_status=NOT_STARTED
→ settlement_status=UNSETTLED
→ 保存所有关键快照
```

闭环：
- 创建订单不扣款。
- 后续必须支付机器费才能生成 API 凭证。

### 8.3 支付机器费

入口：`POST /api/rental/orders/{orderNo}/pay`

流程：
```text
校验订单属于当前用户
→ 校验 order_status=PENDING_PAY
→ rental_order.order_status=PENDING_ACTIVATION
→ paid_amount=order_amount，paid_at=now，api_generated_at=now
→ 钱包 OUT 扣 order_amount，biz_type=RENT_PAY
→ 生成 api_credential，token_status=GENERATED
→ 写 machine_pay_tx_no
```

状态变化：

| 对象 | 当前状态 | 触发 | 下一状态 |
|---|---|---|---|
| rental_order.order_status | PENDING_PAY | 支付机器费成功 | PENDING_ACTIVATION |
| api_credential.token_status | 无记录 | 支付机器费成功生成凭证 | GENERATED |
| wallet_transaction | 无记录 | 扣机器费 | OUT / RENT_PAY |

说明：
- `RentalOrderStatus.PAID` 在枚举中存在，但当前主流程不会停留在该状态。

### 8.4 取消订单

入口：`POST /api/rental/orders/{orderNo}/cancel`

状态变化：

| 当前订单状态 | 触发 | 下一状态 | 钱包影响 | API 影响 |
|---|---|---|---|---|
| PENDING_PAY | 用户取消 | CANCELED | 未扣款，无退款 | 无凭证 |
| PENDING_ACTIVATION | 用户取消 | CANCELED | IN，biz_type=REFUND，退还机器费 | token_status=REVOKED |
| PENDING_ACTIVATION | 激活超时定时任务 | CANCELED | IN，biz_type=REFUND，退还机器费 | token_status=REVOKED |

当前不支持取消：
- `ACTIVATING`
- `PAUSED`
- `RUNNING`
- `SETTLING`
- `EXPIRED`
- `EARLY_CLOSED`

### 8.5 API 部署费支付与激活

入口：
- 部署信息：`GET /api/rental/orders/{orderNo}/deploy-info`
- 支付部署费：`POST /api/rental/orders/{orderNo}/deploy/pay`
- 部署单：`GET /api/rental/orders/{orderNo}/deploy-order`

流程：
```text
校验 order_status=PENDING_ACTIVATION
→ 校验 api_credential.token_status=GENERATED
→ 创建 api_deploy_order，status=PENDING_PAY
→ 钱包 OUT 扣 deploy_fee_snapshot，biz_type=API_DEPLOY_FEE
→ api_deploy_order.status=PAID
→ api_credential.token_status=ACTIVATING
→ rental_order.order_status=ACTIVATING
→ 设置 activation_paid_at、activated_at、auto_pause_at=now+24h
```

状态变化：

| 对象 | 当前状态 | 触发 | 下一状态 |
|---|---|---|---|
| api_deploy_order.status | PENDING_PAY | 部署费支付成功 | PAID |
| api_credential.token_status | GENERATED | 部署费支付成功 | ACTIVATING |
| rental_order.order_status | PENDING_ACTIVATION | 部署费支付成功 | ACTIVATING |
| wallet_transaction | 无记录 | 扣部署费 | OUT / API_DEPLOY_FEE |

说明：
- `ApiDeployOrderStatus.CANCELED`、`REFUNDED` 当前枚举存在，但当前代码没有状态入口。

### 8.6 24 小时自动暂停

任务：`auto_pause`  
默认频率：每 5 分钟  
锁：`scheduler:auto_pause`

扫描条件：
```text
rental_order.order_status = ACTIVATING
AND rental_order.auto_pause_at <= now
```

状态变化：

| 对象 | 当前状态 | 触发 | 下一状态 |
|---|---|---|---|
| rental_order.order_status | ACTIVATING | 自动暂停任务 | PAUSED |
| api_credential.token_status | ACTIVATING | 自动暂停任务 | PAUSED |

闭环：
- 激活中订单不会直接产生收益。
- 自动暂停后，必须用户手动启动才进入收益周期。

### 8.7 用户启动订单

入口：`POST /api/rental/orders/{orderNo}/start`

流程：
```text
校验 order_status=PAUSED
→ 校验 api_credential.token_status=PAUSED
→ rental_order.order_status=RUNNING
→ profit_status=RUNNING
→ started_at=now
→ profit_start_at=now
→ profit_end_at=now + cycle_days_snapshot
→ api_credential.token_status=ACTIVE
```

状态变化：

| 对象 | 当前状态 | 触发 | 下一状态 |
|---|---|---|---|
| rental_order.order_status | PAUSED | 用户启动 | RUNNING |
| rental_order.profit_status | NOT_STARTED | 用户启动 | RUNNING |
| api_credential.token_status | PAUSED | 用户启动 | ACTIVE |

闭环：
- 只有 `order_status=RUNNING` 且达到收益时间条件时，才会被每日收益任务扫描。

### 8.8 订单状态总表

| 状态 | 业务含义 | 当前代码是否进入 | 退出路径 |
|---|---|---:|---|
| PENDING_PAY | 待支付机器费 | 是 | 支付到 `PENDING_ACTIVATION`；取消到 `CANCELED` |
| PAID | 机器费已支付 | 保留 | 当前不会停留 |
| PENDING_ACTIVATION | 已生成 API，待支付部署费 | 是 | 支付部署费到 `ACTIVATING`；取消/超时到 `CANCELED` |
| ACTIVATING | API 激活中 | 是 | 自动暂停到 `PAUSED` |
| PAUSED | 等待用户启动 | 是 | 用户启动到 `RUNNING`；提前结算到 `EARLY_CLOSED` |
| RUNNING | 收益运行中 | 是 | 到期结算到 `EXPIRED`；提前结算到 `EARLY_CLOSED`；后台禁用用户时回到 `PAUSED` |
| SETTLING | 结算中短暂锁状态 | 是 | 结算完成到 `EXPIRED` 或 `EARLY_CLOSED` |
| SETTLED | 已结算 | 保留 | 当前以 `settlement_status=SETTLED` 表示结算完成 |
| EXPIRED | 到期结算完成 | 是 | 终态 |
| EARLY_CLOSED | 提前结算完成 | 是 | 终态 |
| CANCELED | 已取消 | 是 | 终态 |

### 8.9 API 凭证状态总表

| 状态 | 业务含义 | 进入路径 | 退出路径 |
|---|---|---|---|
| GENERATED | 机器费支付后已生成，未部署 | 支付机器费 | 支付部署费到 `ACTIVATING`；取消/超时到 `REVOKED` |
| ACTIVATING | 部署费已支付，激活中 | 支付部署费 | 24 小时自动暂停到 `PAUSED` |
| PAUSED | 暂停，等待启动 | 自动暂停；后台禁用运行中用户 | 用户启动到 `ACTIVE`；提前结算到 `REVOKED` |
| ACTIVE | 运行中 | 用户启动 | 到期到 `EXPIRED`；提前结算到 `REVOKED`；后台禁用用户到 `PAUSED` |
| EXPIRED | 到期失效 | 到期结算 | 终态 |
| REVOKED | 已吊销 | 取消、超时取消、提前结算 | 终态 |

## 9. 每日收益业务

任务：`daily_profit`  
默认频率：每天 00:05  
锁：`scheduler:daily_profit:lock`，TTL 3600 秒

扫描条件：
```text
rental_order.order_status = RUNNING
AND profit_start_at <= now
AND profit_end_at >= 次日 00:00
```

处理逻辑：
```text
按页扫描 RUNNING 订单
→ 每条订单独立事务
→ 再次校验 today < DATE(profit_end_at)
→ 检查 rental_profit_record 是否已存在相同 orderId + profitDate
→ 创建收益记录，status=PENDING，commission_generated=0
→ 计算 base_profit = token_output_per_day_snapshot × token_unit_price_snapshot
→ 计算 final_profit = base_profit × yield_multiplier_snapshot
→ 钱包 IN 入账，biz_type=RENT_PROFIT
→ rental_profit_record.status=SETTLED
→ 写 wallet_tx_no、settled_at
```

状态变化：

| 对象 | 当前状态 | 触发 | 下一状态 |
|---|---|---|---|
| rental_profit_record.status | PENDING | 收益钱包入账成功 | SETTLED |
| rental_profit_record.commission_generated | 0 | 佣金任务处理完成 | 1 |
| user_wallet.total_profit | 原值 | 收益入账 | 增加 final_profit |

闭环：
- 收益记录唯一约束防止同一天重复入账。
- 钱包幂等键：`RENT_PROFIT:{orderNo}:{profitDate}`。
- 佣金不在收益入账事务内直接生成，而是由 `commission_generate` 定时任务异步补齐。

## 10. 佣金与团队业务

### 10.1 团队关系

团队关系在注册时写入：
```text
新用户 X 使用上级 C 的邀请码注册
→ user_referral_relation 记录 X 的直属上级和 1/2/3 级返佣上级
→ user_team_relation 写 C/B/A... 到 X 的所有祖先闭包关系
```

查询入口：
- 团队概览：`GET /api/team/summary`
- 团队成员：`GET /api/team/members`

规则：
- 团队统计支持无限层级。
- 佣金只发三级。

### 10.2 佣金生成

任务：`commission_generate`  
默认频率：每 5 分钟  
锁：`scheduler:commission_generate:lock`，TTL 3600 秒

扫描条件：
```text
rental_profit_record.status = SETTLED
AND commission_generated = 0
```

流程：
```text
读取收益记录
→ 查询收益用户的 user_referral_relation
→ 取 level1_user_id、level2_user_id、level3_user_id
→ 读取启用的 commission_rule，缺省比例为 20% / 10% / 5%
→ 每级创建 commission_record，status=PENDING
→ 钱包 IN 入账，biz_type=COMMISSION_PROFIT
→ commission_record.status=SETTLED
→ 收益记录 commission_generated=1
```

状态变化：

| 对象 | 当前状态 | 触发 | 下一状态 |
|---|---|---|---|
| commission_record.status | PENDING | 佣金钱包入账成功 | SETTLED |
| rental_profit_record.commission_generated | 0 | 三级佣金处理完成，或无上级可发 | 1 |
| user_wallet.total_commission | 原值 | 佣金入账 | 增加 commission_amount |

闭环：
- `commission_record(source_profit_id, level_no)` 防止同一收益同一级重复发佣金。
- 钱包幂等键：`COMMISSION_PROFIT:{profitId}:{levelNo}`。

## 11. 结算业务

### 11.1 到期结算

任务：`expire_settlement`  
默认频率：每天 00:10  
锁：`scheduler:expire_settlement:lock`

扫描条件：
```text
rental_order.order_status = RUNNING
AND profit_end_at <= now
```

流程：
```text
锁定订单：order_status RUNNING → SETTLING
→ settlement_status UNSETTLED → SETTLING
→ 汇总已入账收益作为展示字段 profit_amount
→ 创建 rental_settlement_order，settlement_type=EXPIRE，status=SETTLED
→ 钱包 IN 入账退还本金，biz_type=SETTLEMENT
→ rental_order.order_status=EXPIRED
→ profit_status=FINISHED
→ settlement_status=SETTLED
→ api_credential.token_status=EXPIRED
```

状态变化：

| 对象 | 当前状态 | 触发 | 下一状态 |
|---|---|---|---|
| rental_order.order_status | RUNNING | 到期结算开始 | SETTLING |
| rental_order.order_status | SETTLING | 结算完成 | EXPIRED |
| rental_order.profit_status | RUNNING | 结算完成 | FINISHED |
| rental_order.settlement_status | UNSETTLED | 结算开始 | SETTLING |
| rental_order.settlement_status | SETTLING | 结算完成 | SETTLED |
| api_credential.token_status | ACTIVE | 结算完成 | EXPIRED |
| rental_settlement_order.status | 新建 | 当前代码直接创建完成态 | SETTLED |

闭环：
- 到期后本金退回用户可用余额。
- 收益已按日发放，不在结算时重复发收益。

### 11.2 提前结算

入口：`POST /api/rental/orders/{orderNo}/settle-early`

当前支持状态：
- `RUNNING`
- `PAUSED`

流程：
```text
校验订单属于当前用户
→ 校验 order_status in (RUNNING, PAUSED)
→ 如果已存在 EARLY_TERMINATE 结算单则直接返回
→ 锁定订单：order_status → SETTLING，settlement_status → SETTLING
→ penalty_amount = order_amount × early_penalty_rate_snapshot
→ actual_settle_amount = order_amount - penalty_amount
→ 创建 rental_settlement_order，settlement_type=EARLY_TERMINATE，status=SETTLED
→ 记录 EARLY_PENALTY 展示流水，不额外扣用户余额
→ 钱包 IN 入账 actual_settle_amount，biz_type=SETTLEMENT
→ rental_order.order_status=EARLY_CLOSED
→ profit_status=FINISHED
→ settlement_status=SETTLED
→ api_credential.token_status=REVOKED
```

状态变化：

| 对象 | 当前状态 | 触发 | 下一状态 |
|---|---|---|---|
| rental_order.order_status | RUNNING / PAUSED | 提前结算开始 | SETTLING |
| rental_order.order_status | SETTLING | 提前结算完成 | EARLY_CLOSED |
| rental_order.profit_status | RUNNING / NOT_STARTED | 提前结算完成 | FINISHED |
| api_credential.token_status | ACTIVE / PAUSED | 提前结算完成 | REVOKED |
| rental_settlement_order.status | 新建 | 当前代码直接创建完成态 | SETTLED |

说明：
- `RentalSettlementOrderStatus.PENDING / REJECTED / CANCELED` 枚举存在，但当前结算服务直接创建 `SETTLED` 状态结算单，没有后台审核流。

## 12. 提现业务

入口：
- 提交提现：`POST /api/withdraw/orders`
- 我的提现单：`GET /api/withdraw/orders`
- 用户取消：`POST /api/withdraw/orders/{withdrawNo}/cancel`
- 后台审核通过：`POST /api/admin/withdraw/orders/{withdrawNo}/approve`
- 后台驳回：`POST /api/admin/withdraw/orders/{withdrawNo}/reject`
- 后台标记打款：`POST /api/admin/withdraw/orders/{withdrawNo}/paid`

规则：
- 币种固定 USDT。
- 最低提现金额：`withdraw.min_amount`，默认 10。
- 每日提现上限：`withdraw.max_daily_amount`，默认 100000。
- 日限额统计状态：`PENDING_REVIEW`、`APPROVED`、`PAID`。
- 手续费：小于 `withdraw.fee_free_threshold` 默认 100 时，按 `withdraw.fee_rate` 默认 5% 收取；大于等于阈值免手续费。
- 地址校验：
  - TRC20：`^T[1-9A-HJ-NP-Za-km-z]{33}$`
  - ERC20/BEP20：`^0x[0-9a-fA-F]{40}$`

流程：
```text
用户提交提现金额、网络、收款地址
→ 校验金额、日限额和地址格式
→ 计算 fee_amount 和 actual_amount
→ 创建 withdraw_order，status=PENDING_REVIEW
→ 钱包 FREEZE：available_balance 减少 apply_amount，frozen_balance 增加 apply_amount
→ 后台审核
  → 通过：status=APPROVED，余额仍冻结
  → 驳回：status=REJECTED，钱包 UNFREEZE 解冻
→ 审核通过后后台标记打款
→ status=PAID
→ 钱包 OUT_FROM_FROZEN：frozen_balance 减少 apply_amount，total_withdraw 增加 actual_amount
```

状态变化：

| 当前状态 | 触发 | 下一状态 | 钱包影响 | 闭环 |
|---|---|---|---|---|
| PENDING_REVIEW | 用户取消 | CANCELED | UNFREEZE | 提现单终止，资金回可用余额 |
| PENDING_REVIEW | 后台审核通过 | APPROVED | 无新增变动，资金仍冻结 | 等待打款 |
| PENDING_REVIEW | 后台驳回 | REJECTED | UNFREEZE | 提现单终止，资金回可用余额 |
| APPROVED | 后台驳回 | REJECTED | UNFREEZE | 提现单终止，资金回可用余额 |
| APPROVED | 后台标记打款 | PAID | OUT_FROM_FROZEN | 提现完成 |

## 13. 后台业务

### 13.1 后台认证

入口：
- 登录：`POST /api/admin/auth/login`
- 当前管理员：`GET /api/admin/auth/me`
- 登出：`POST /api/admin/auth/logout`
- 管理员列表：`GET /api/admin/auth/admins`
- 创建管理员：`POST /api/admin/auth/register`

流程：
```text
管理员提交用户名密码
→ 校验 sys_admin 存在且 status=ENABLED
→ BCrypt 校验密码
→ 更新 last_login_at
→ 写 sys_admin_log
→ 返回 ADMIN 身份 JWT
```

说明：
- 当前登出只写日志，Token 黑名单未实现。

### 13.2 用户禁用与启用

入口：
- 禁用：`POST /api/admin/users/{userId}/disable`
- 启用：`POST /api/admin/users/{userId}/enable`

禁用流程：
```text
app_user.status=0
→ 扫描该用户 RUNNING 订单
→ rental_order.order_status=PAUSED
→ api_credential.token_status=PAUSED
→ 写 sys_admin_log
```

启用流程：
```text
app_user.status=1
→ 写 sys_admin_log
→ 不自动恢复已暂停订单
```

状态变化：

| 对象 | 当前状态 | 触发 | 下一状态 |
|---|---|---|---|
| app_user.status | ENABLED | 后台禁用 | DISABLED |
| rental_order.order_status | RUNNING | 后台禁用用户 | PAUSED |
| api_credential.token_status | ACTIVE | 后台禁用用户 | PAUSED |
| app_user.status | DISABLED | 后台启用 | ENABLED |

### 13.3 后台查询与看板

后台查询覆盖：
- 用户、钱包、钱包流水
- 租赁订单、API 凭证、API 部署单
- 收益记录、结算单、佣金记录、团队关系
- 管理员操作日志

看板覆盖：
- 用户总量、新增、启用/禁用
- 充值、提现、订单、收益、佣金累计和今日指标
- 订单状态分布、收益状态分布
- 钱包可用余额和冻结余额汇总

### 13.4 系统配置

入口：
- 系统配置列表、详情、更新：`/api/admin/sys-configs`

当前关键配置：

| 配置键 | 默认值 | 用途 |
|---|---:|---|
| recharge.min_amount | 500 | 全局最低充值金额 |
| withdraw.min_amount | 10 | 最低提现金额 |
| withdraw.fee_free_threshold | 100 | 免手续费阈值 |
| withdraw.fee_rate | 0.05 | 提现手续费率 |
| withdraw.max_daily_amount | 100000 | 每日提现上限 |
| order.pending_activation_timeout_minutes | 60 | API 待激活超时取消分钟数 |
| email_code.rate_limit_per_minute | 5 | 邮箱验证码每分钟发送上限 |

闭环：
- 配置更新写 `sys_admin_log`。
- 充值、提现、激活超时任务实时读取配置。

### 13.5 调度任务

后台可手动触发：
- 激活超时取消
- 自动暂停
- 每日收益
- 到期结算
- 佣金生成

调度日志状态：

| 状态 | 含义 |
|---|---|
| RUNNING | 任务已开始 |
| SUCCESS | 全部成功 |
| PARTIAL_FAIL | 部分失败 |
| FAIL | 全部失败 |
| SKIPPED | 未获取到锁，当前只作为接口返回，不写入枚举表 |

## 14. 通知业务

入口：
- 用户通知列表、详情、标记已读、全部已读：`/api/notifications`
- 后台通知列表、创建、广播、删除：`/api/admin/notifications`

流程：
```text
后台创建单用户通知或广播通知
→ sys_notification.read_status=UNREAD
→ 用户查看详情或主动标记已读
→ read_status=READ，read_at=now
```

状态变化：

| 对象 | 当前状态 | 触发 | 下一状态 |
|---|---|---|---|
| sys_notification.read_status | UNREAD | 查看详情、标记已读、全部已读 | READ |

当前实现说明：
- 通知服务已具备手动创建、广播、已读闭环。
- 充值成功、提现成功/驳回、API 激活、订单取消/到期、佣金成功等枚举已存在，但部分业务服务里还是 TODO，尚未自动创建业务通知。

## 15. 博客业务

入口：
- 前台博客分类、标签、文章列表、详情：`/api/blog/**`
- 后台博客分类、标签、文章管理：`/api/admin/blog/**`

流程：
```text
后台创建分类/标签
→ 状态 ENABLED 后可在前台筛选
→ 后台创建文章，默认 DRAFT
→ 发布文章，publish_status=PUBLISHED，设置 published_at
→ 前台可见并可查看详情
→ 查看详情时 view_count + 1
→ 后台下线文章，publish_status=OFFLINE
→ 后台删除文章时同步删除文章标签关系
```

状态变化：

| 对象 | 当前状态 | 触发 | 下一状态 |
|---|---|---|---|
| blog_category.status | ENABLED / DISABLED | 后台启用/停用 | ENABLED / DISABLED |
| blog_tag.status | ENABLED / DISABLED | 后台启用/停用 | ENABLED / DISABLED |
| blog_post.publish_status | DRAFT | 后台发布 | PUBLISHED |
| blog_post.publish_status | PUBLISHED | 后台下线 | OFFLINE |
| blog_post.publish_status | DRAFT / PUBLISHED / OFFLINE | 后台删除 | 物理删除 |

## 16. 状态机汇总

### 16.1 通用状态

| 值 | 含义 |
|---:|---|
| 1 | ENABLED，启用/正常/有效 |
| 0 | DISABLED，停用/禁用/无效 |

### 16.2 订单相关状态

| 字段 | 状态 |
|---|---|
| rental_order.order_status | PENDING_PAY、PAID、PENDING_ACTIVATION、ACTIVATING、PAUSED、RUNNING、EXPIRED、SETTLING、SETTLED、EARLY_CLOSED、CANCELED |
| rental_order.profit_status | NOT_STARTED、RUNNING、PAUSED、FINISHED |
| rental_order.settlement_status | UNSETTLED、SETTLING、SETTLED |
| api_credential.token_status | GENERATED、ACTIVATING、PAUSED、ACTIVE、EXPIRED、REVOKED |
| api_deploy_order.status | PENDING_PAY、PAID、CANCELED、REFUNDED |
| rental_settlement_order.status | PENDING、SETTLED、REJECTED、CANCELED |
| rental_settlement_order.settlement_type | EXPIRE、EARLY_TERMINATE、MANUAL |

当前未实际流转或保留状态：
- `rental_order.order_status=PAID`、`SETTLED`
- `rental_order.profit_status=PAUSED`
- `api_deploy_order.status=CANCELED / REFUNDED`
- `rental_settlement_order.status=PENDING / REJECTED / CANCELED`
- `rental_settlement_order.settlement_type=MANUAL`

### 16.3 财务相关状态

| 字段 | 状态 |
|---|---|
| recharge_order.status | SUBMITTED、APPROVED、REJECTED、CANCELED |
| withdraw_order.status | PENDING_REVIEW、APPROVED、PAID、REJECTED、CANCELED |
| rental_profit_record.status | PENDING、SETTLED、CANCELED |
| commission_record.status | PENDING、SETTLED、CANCELED |
| wallet_transaction.tx_type | IN、OUT、FREEZE、UNFREEZE |
| wallet_transaction.biz_type | RECHARGE、WITHDRAW、RENT_PAY、API_DEPLOY_FEE、RENT_PROFIT、COMMISSION_PROFIT、SETTLEMENT、EARLY_PENALTY、REFUND、ADJUST |

当前未实际流转或保留状态：
- `rental_profit_record.status=CANCELED`
- `commission_record.status=CANCELED`
- `wallet_transaction.biz_type=ADJUST` 尚无调账入口。

### 16.4 内容与通知状态

| 字段 | 状态 |
|---|---|
| blog_post.publish_status | DRAFT、PUBLISHED、OFFLINE |
| sys_notification.read_status | UNREAD、READ |
| sys_notification.type | FINANCIAL、SYSTEM、BLOG |
| sys_notification.biz_type | RECHARGE_SUCCESS、WITHDRAW_SUCCESS、WITHDRAW_REJECTED、PROFIT_SUCCESS、COMMISSION_SUCCESS、API_ACTIVATED、ORDER_CANCELED、ORDER_EXPIRED、BLOG_UPDATE |

## 17. 关键验收口径

账号：
- 注册成功后必须同时有 `app_user`、`user_wallet`、`user_referral_relation`。
- 使用邀请码注册时，必须写入直接上级和团队闭包关系。

充值：
- 低于全局/渠道最低金额不能提交。
- 审核通过必须入账钱包并写流水。
- 同一外部交易号不能重复提交。

租赁订单：
- 未支付机器费不能生成 API 凭证。
- 支付机器费后必须扣钱包并生成 API 凭证。
- 未支付部署费不能进入激活中。
- `ACTIVATING` 不产生收益。
- 只有用户手动启动后的 `RUNNING` 状态才产生收益。

收益：
- 同一订单同一收益日期只能入账一次。
- 收益入账必须增加钱包可用余额和 `total_profit`。
- 收益入账后佣金任务最终将 `commission_generated` 置为 1。

佣金：
- 只发三级。
- 每个收益记录每一级只能生成一条佣金。
- 佣金入账必须增加受益人钱包可用余额和 `total_commission`。

结算：
- 到期结算必须退回本金，订单终态为 `EXPIRED`。
- 提前结算必须扣除违约金后退回本金，订单终态为 `EARLY_CLOSED`。
- 结算完成后 API 凭证必须过期或吊销。

提现：
- 提现申请必须冻结全额申请金额。
- 取消或驳回必须解冻。
- 标记打款必须从冻结余额扣减并增加累计提现。

后台：
- 用户禁用必须暂停运行中订单和 API。
- 关键配置更新、产品规则变更、博客管理和管理员登录必须写操作日志。

## 18. 当前实现缺口和产品风险

| 项目 | 当前情况 | 风险/影响 |
|---|---|---|
| 自动业务通知 | 通知服务存在，但充值、提现、订单、收益、佣金等业务里多处仍未自动调用 | 用户端消息中心无法自动反映核心业务结果 |
| 订单 `ACTIVATING` 提前处理 | 当前不能取消，也不能提前结算，只能等待自动暂停 | 用户支付部署费后 24 小时内没有手动关闭入口 |
| 结算审核流 | 结算单当前直接创建为 `SETTLED` | 不支持后台复核、驳回或人工结算 |
| 邮箱验证码过期状态 | 通过时间判断过期，不主动写 `EXPIRED` | 后台查看验证码状态时会看到部分过期验证码仍是 `UNUSED` |
| 调账 | 枚举存在 `ADJUST`，但无实际入口 | 后台无法通过系统化流程做钱包调账 |
| 商品库存 | 库存仅展示，不参与锁定和扣减 | 产品层面不防超卖，需由运营口径兜底 |
| 登出 | 后台登出只写日志，没有 Token 黑名单 | JWT 在过期前仍可能继续有效 |
| MQ | 项目有 RabbitMQ 配置，但核心流程当前主要走同步事务和定时任务 | 如需异步通知或外部事件，需补消息闭环 |

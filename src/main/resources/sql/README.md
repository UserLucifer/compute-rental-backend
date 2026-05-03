# SQL 脚本目录

后续数据库脚本按文档建表顺序放置，例如：

```text
001_user_account.sql
002_wallet_finance.sql
003_product_rule.sql
004_order_api_profit_settlement.sql
005_commission.sql
006_system_admin_notification.sql
```

金额字段统一使用 `DECIMAL(20,8)`，时间字段按 `Asia/Shanghai` 口径写入。

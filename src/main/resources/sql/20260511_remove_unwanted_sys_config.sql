-- Remove system config entries that are no longer exposed or enforced.
-- Safe to run repeatedly.

DELETE FROM `sys_config`
WHERE `config_key` IN (
  'withdraw.max_daily_amount',
  'recharge.max_amount',
  'recharge.fee_rate'
);

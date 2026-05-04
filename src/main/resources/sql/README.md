# SQL scripts

Only two executable SQL scripts are kept in this directory:

1. `schema.sql` - table structure, columns, constraints, and indexes.
2. `init-data.sql` - initial system data, product catalog seed data, and blog seed data.

Recommended execution order:

```bash
mysql -h <host> -P <port> -u <user> -p <database> < schema.sql
mysql -h <host> -P <port> -u <user> -p <database> < init-data.sql
```

Review default admin credentials and business seed data before running `init-data.sql` in production.

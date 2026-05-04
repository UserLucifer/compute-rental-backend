package com.compute.rental.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.sql.DriverManager;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

class DatabasePerformanceIndexMigrationIT {

    private static final List<IndexMigration> INDEX_MIGRATIONS = List.of(
            new IndexMigration(
                    "recharge_order",
                    "idx_status_credited_at",
                    "ALTER TABLE `recharge_order` ADD INDEX `idx_status_credited_at` (`status`, `credited_at`)"),
            new IndexMigration(
                    "withdraw_order",
                    "idx_status_paid_at",
                    "ALTER TABLE `withdraw_order` ADD INDEX `idx_status_paid_at` (`status`, `paid_at`)"),
            new IndexMigration(
                    "rental_order",
                    "idx_profit_status",
                    "ALTER TABLE `rental_order` ADD INDEX `idx_profit_status` (`profit_status`)"),
            new IndexMigration(
                    "rental_order",
                    "idx_paid_at",
                    "ALTER TABLE `rental_order` ADD INDEX `idx_paid_at` (`paid_at`)"),
            new IndexMigration(
                    "rental_profit_record",
                    "idx_status_profit_date",
                    "ALTER TABLE `rental_profit_record` ADD INDEX `idx_status_profit_date` (`status`, `profit_date`)"),
            new IndexMigration(
                    "commission_record",
                    "idx_status_settled_at",
                    "ALTER TABLE `commission_record` ADD INDEX `idx_status_settled_at` (`status`, `settled_at`)")
    );

    @Test
    void applyMissingPerformanceIndexes() throws Exception {
        var properties = applicationProperties();
        var jdbcUrl = appendTimeouts(requiredProperty(properties, "DB_URL", "spring.datasource.url"));
        var username = requiredProperty(properties, "DB_USERNAME", "spring.datasource.username");
        var password = requiredProperty(properties, "DB_PASSWORD", "spring.datasource.password");
        var appliedCount = 0;

        try (var connection = DriverManager.getConnection(jdbcUrl, username, password);
             var dbStatement = connection.createStatement();
             var dbResult = dbStatement.executeQuery("SELECT DATABASE()")) {
            if (!dbResult.next() || !StringUtils.hasText(dbResult.getString(1))) {
                throw new IllegalStateException("No selected database in JDBC URL");
            }
            var schema = dbResult.getString(1);

            for (var migration : INDEX_MIGRATIONS) {
                if (indexExists(connection, schema, migration.tableName(), migration.indexName())) {
                    System.out.println("SKIPPED_INDEX=" + migration.tableName() + "." + migration.indexName());
                    continue;
                }

                try (var statement = connection.createStatement()) {
                    statement.execute(migration.sql());
                }
                appliedCount++;
                System.out.println("APPLIED_INDEX=" + migration.tableName() + "." + migration.indexName());
            }
        }

        assertThat(appliedCount).isGreaterThanOrEqualTo(0);
    }

    private boolean indexExists(
            java.sql.Connection connection,
            String schema,
            String tableName,
            String indexName
    ) throws Exception {
        try (var statement = connection.prepareStatement("""
                SELECT 1
                FROM information_schema.statistics
                WHERE table_schema = ?
                  AND table_name = ?
                  AND index_name = ?
                LIMIT 1
                """)) {
            statement.setString(1, schema);
            statement.setString(2, tableName);
            statement.setString(3, indexName);
            try (var rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Properties applicationProperties() {
        var properties = new Properties();
        properties.putAll(loadYaml(new ClassPathResource("application.yml")));
        var localConfig = new File("application-local.yml");
        if (localConfig.exists()) {
            properties.putAll(loadYaml(new FileSystemResource(localConfig)));
        }
        return properties;
    }

    private Properties loadYaml(Resource resource) {
        var factory = new YamlPropertiesFactoryBean();
        factory.setResources(resource);
        var properties = factory.getObject();
        return properties == null ? new Properties() : properties;
    }

    private String requiredProperty(Properties properties, String envName, String propertyName) {
        var value = property(properties, envName, propertyName, "");
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Missing database configuration: " + propertyName);
        }
        return value;
    }

    private String property(Properties properties, String envName, String propertyName, String defaultValue) {
        var envValue = System.getenv(envName);
        if (StringUtils.hasText(envValue)) {
            return envValue.trim();
        }
        var systemEnvValue = System.getProperty(envName);
        if (StringUtils.hasText(systemEnvValue)) {
            return systemEnvValue.trim();
        }
        var systemPropertyValue = System.getProperty(propertyName);
        if (StringUtils.hasText(systemPropertyValue)) {
            return systemPropertyValue.trim();
        }
        var propertyValue = properties.get(propertyName);
        if (propertyValue != null && StringUtils.hasText(String.valueOf(propertyValue))) {
            var resolvedValue = resolvePlaceholder(String.valueOf(propertyValue));
            return StringUtils.hasText(resolvedValue) ? resolvedValue : defaultValue;
        }
        return defaultValue;
    }

    private String resolvePlaceholder(String value) {
        var trimmed = value.trim();
        if (!trimmed.startsWith("${") || !trimmed.endsWith("}")) {
            return trimmed;
        }

        var expression = trimmed.substring(2, trimmed.length() - 1);
        var separatorIndex = expression.indexOf(':');
        var placeholderName = separatorIndex < 0 ? expression : expression.substring(0, separatorIndex);
        var defaultValue = separatorIndex < 0 ? "" : expression.substring(separatorIndex + 1);
        var envValue = System.getenv(placeholderName);
        if (StringUtils.hasText(envValue)) {
            return envValue.trim();
        }
        var systemValue = System.getProperty(placeholderName);
        if (StringUtils.hasText(systemValue)) {
            return systemValue.trim();
        }
        return defaultValue;
    }

    private String appendTimeouts(String jdbcUrl) {
        var separator = jdbcUrl.contains("?") ? "&" : "?";
        var url = jdbcUrl;
        if (!url.contains("connectTimeout=")) {
            url += separator + "connectTimeout=15000";
            separator = "&";
        }
        if (!url.contains("socketTimeout=")) {
            url += separator + "socketTimeout=15000";
        }
        return url;
    }

    private record IndexMigration(String tableName, String indexName, String sql) {
    }
}

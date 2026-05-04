package com.compute.rental.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

class DatabaseSchemaSyncIT {

    @Test
    void projectSqlSchemaShouldMatchDatabase() throws Exception {
        var expected = expectedSchema();
        var actual = actualSchema();
        var findings = new ArrayList<String>();

        for (var entry : expected.entrySet()) {
            var tableName = entry.getKey();
            var expectedTable = entry.getValue();
            var actualTable = actual.get(tableName);
            if (actualTable == null) {
                findings.add("missing table: " + tableName);
                continue;
            }
            for (var column : expectedTable.columns()) {
                if (!actualTable.columns().contains(column)) {
                    findings.add("missing column: " + tableName + "." + column);
                }
            }
            for (var index : expectedTable.indexes()) {
                if (!actualTable.indexes().contains(index)) {
                    findings.add("missing index: " + tableName + "." + index);
                }
            }
        }

        assertThat(findings).isEmpty();
    }

    private Map<String, TableSchema> expectedSchema() throws Exception {
        var expected = new TreeMap<String, TableSchema>(String.CASE_INSENSITIVE_ORDER);
        parseCreateTables(Path.of("src/main/resources/sql/schema.sql"), expected);
        return expected;
    }

    private Map<String, TableSchema> actualSchema() throws Exception {
        var properties = applicationProperties();
        var jdbcUrl = appendTimeouts(requiredProperty(properties, "DB_URL", "spring.datasource.url"));
        var username = requiredProperty(properties, "DB_USERNAME", "spring.datasource.username");
        var password = requiredProperty(properties, "DB_PASSWORD", "spring.datasource.password");
        var actual = new TreeMap<String, TableSchema>(String.CASE_INSENSITIVE_ORDER);

        try (var connection = DriverManager.getConnection(jdbcUrl, username, password);
             var dbStatement = connection.createStatement();
             var dbResult = dbStatement.executeQuery("SELECT DATABASE()")) {
            if (!dbResult.next() || !StringUtils.hasText(dbResult.getString(1))) {
                throw new IllegalStateException("No selected database in JDBC URL");
            }
            var schema = dbResult.getString(1);

            try (var statement = connection.prepareStatement("""
                    SELECT table_name
                    FROM information_schema.tables
                    WHERE table_schema = ?
                    """)) {
                statement.setString(1, schema);
                try (var rs = statement.executeQuery()) {
                    while (rs.next()) {
                        actual.computeIfAbsent(rs.getString(1), ignored -> new TableSchema());
                    }
                }
            }

            try (var statement = connection.prepareStatement("""
                    SELECT table_name, column_name
                    FROM information_schema.columns
                    WHERE table_schema = ?
                    """)) {
                statement.setString(1, schema);
                try (var rs = statement.executeQuery()) {
                    while (rs.next()) {
                        actual.computeIfAbsent(rs.getString(1), ignored -> new TableSchema())
                                .columns()
                                .add(rs.getString(2));
                    }
                }
            }

            try (var statement = connection.prepareStatement("""
                    SELECT table_name, index_name
                    FROM information_schema.statistics
                    WHERE table_schema = ?
                    """)) {
                statement.setString(1, schema);
                try (var rs = statement.executeQuery()) {
                    while (rs.next()) {
                        actual.computeIfAbsent(rs.getString(1), ignored -> new TableSchema())
                                .indexes()
                                .add(rs.getString(2));
                    }
                }
            }
        }
        return actual;
    }

    private void parseCreateTables(Path path, Map<String, TableSchema> expected) throws Exception {
        if (!Files.exists(path)) {
            return;
        }
        var sql = Files.readString(path, StandardCharsets.UTF_8);
        var tablePattern = Pattern.compile(
                "(?is)CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?`([^`]+)`\\s*\\((.*?)\\)\\s*ENGINE");
        var matcher = tablePattern.matcher(sql);
        while (matcher.find()) {
            var table = expected.computeIfAbsent(matcher.group(1), ignored -> new TableSchema());
            for (var rawLine : matcher.group(2).split("\\R")) {
                var line = rawLine.trim();
                var columnMatcher = Pattern.compile("^`([^`]+)`\\s+").matcher(line);
                if (columnMatcher.find()) {
                    table.columns().add(columnMatcher.group(1));
                    continue;
                }
                if (line.startsWith("PRIMARY KEY")) {
                    table.indexes().add("PRIMARY");
                    continue;
                }
                var indexMatcher = Pattern.compile("^(?:UNIQUE\\s+)?KEY\\s+`([^`]+)`").matcher(line);
                if (indexMatcher.find()) {
                    table.indexes().add(indexMatcher.group(1));
                }
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

    private record TableSchema(Set<String> columns, Set<String> indexes) {

        private TableSchema() {
            this(
                    new TreeSet<>(String.CASE_INSENSITIVE_ORDER),
                    new TreeSet<>(String.CASE_INSENSITIVE_ORDER)
            );
        }
    }
}

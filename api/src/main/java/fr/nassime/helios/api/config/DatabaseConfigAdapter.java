package fr.nassime.helios.api.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Adapter to convert DatabaseConfig to provider-specific configuration objects.
 * This allows the multi-database system to work with existing provider implementations.
 */
public class DatabaseConfigAdapter {

    /**
     * Convert DatabaseConfig to a generic HeliosConfiguration that providers can use.
     * This creates a minimal configuration object that can be processed by ServiceLoader providers.
     */
    public static HeliosConfiguration toHeliosConfiguration(DatabaseConfig dbConfig) {
        return new HeliosConfiguration() {
            @Override
            public String getProvider() {
                return dbConfig.getType();
            }

            @Override
            public String getConnectionUrl() {
                if (dbConfig.isSql()) {
                    return dbConfig.getJdbcUrl();
                } else {
                    return dbConfig.getMongoConnectionString();
                }
            }

            @Override
            public String getUsername() {
                return dbConfig.getUsername();
            }

            @Override
            public String getPassword() {
                return dbConfig.getPassword();
            }

            @Override
            public String getDatabase() {
                String url = dbConfig.getUrl();
                if (url.contains("/")) {
                    int lastSlash = url.lastIndexOf("/");
                    String dbName = url.substring(lastSlash + 1);
                    // Remove query parameters if any
                    if (dbName.contains("?")) {
                        dbName = dbName.substring(0, dbName.indexOf("?"));
                    }
                    return dbName;
                }
                return "helios";
            }

            @Override
            public String getSchema() {
                return null;
            }

            @Override
            public ConnectionPoolConfig getConnectionPoolConfig() {
                return ConnectionPoolConfig.builder()
                        .maximumPoolSize(dbConfig.getPoolSize())
                        .minimumIdle(dbConfig.getEffectiveMinPoolSize())
                        .connectionTimeout(dbConfig.getConnectionTimeout())
                        .idleTimeout(dbConfig.getIdleTimeout())
                        .maxLifetime(dbConfig.getMaxLifetime())
                        .leakDetectionThreshold(dbConfig.getLeakDetectionThreshold())
                        .build();
            }

            @Override
            public Properties getProperties() {
                return new Properties();
            }

            @Override
            public String getProperty(String key) {
                return null;
            }

            @Override
            public String getProperty(String key, String defaultValue) {
                return defaultValue;
            }

            @Override
            public Map<String, Object> toMap() {
                Map<String, Object> map = new HashMap<>();
                map.put("provider", getProvider());
                map.put("connectionUrl", getConnectionUrl());
                map.put("username", getUsername());
                map.put("database", getDatabase());
                return map;
            }
        };
    }
}
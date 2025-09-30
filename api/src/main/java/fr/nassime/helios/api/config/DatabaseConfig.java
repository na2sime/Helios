package fr.nassime.helios.api.config;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.Map;

/**
 * Configuration for a single database connection.
 * Supports PostgreSQL, MariaDB, and MongoDB with advanced connection pooling options.
 */
@Getter
@Builder
public class DatabaseConfig {

    /**
     * Unique name identifier for this database connection.
     * Used in @Persistable(database = "name") to route entities.
     */
    private final String name;

    /**
     * Database type: "postgresql", "mariadb", or "mongodb"
     */
    private final String type;

    /**
     * Connection URL or host
     * Examples:
     * - PostgreSQL: "localhost:5432/mydb" or "jdbc:postgresql://localhost:5432/mydb"
     * - MariaDB: "localhost:3306/mydb" or "jdbc:mariadb://localhost:3306/mydb"
     * - MongoDB: "mongodb://localhost:27017/mydb"
     */
    private final String url;

    /**
     * Database username
     */
    private final String username;

    /**
     * Database password
     */
    private final String password;

    /**
     * Maximum connection pool size (default: 10)
     */
    @Builder.Default
    private final int poolSize = 10;

    /**
     * Minimum idle connections in pool (default: poolSize / 4)
     */
    @Builder.Default
    private final int minPoolSize = -1;

    /**
     * Connection timeout in milliseconds (default: 30000 = 30 seconds)
     */
    @Builder.Default
    private final long connectionTimeout = 30000;

    /**
     * Idle timeout in milliseconds (default: 600000 = 10 minutes)
     */
    @Builder.Default
    private final long idleTimeout = 600000;

    /**
     * Max lifetime of a connection in milliseconds (default: 1800000 = 30 minutes)
     */
    @Builder.Default
    private final long maxLifetime = 1800000;

    /**
     * Enable connection leak detection (default: false)
     */
    @Builder.Default
    private final boolean leakDetection = false;

    /**
     * Leak detection threshold in milliseconds (default: 0 = disabled)
     */
    @Builder.Default
    private final long leakDetectionThreshold = 0;

    /**
     * Enable SSL/TLS for connections (default: false)
     */
    @Builder.Default
    private final boolean ssl = false;

    /**
     * Schema name for SQL databases (optional)
     */
    private final String schema;

    /**
     * Custom properties for database-specific configuration
     */
    @Singular
    private final Map<String, Object> properties;

    /**
     * Check if this is a SQL database (PostgreSQL or MariaDB)
     */
    public boolean isSql() {
        return "postgresql".equalsIgnoreCase(type) || "mariadb".equalsIgnoreCase(type);
    }

    /**
     * Check if this is a MongoDB database
     */
    public boolean isMongo() {
        return "mongodb".equalsIgnoreCase(type);
    }

    /**
     * Get the effective minimum pool size (calculated if not set)
     */
    public int getEffectiveMinPoolSize() {
        return minPoolSize > 0 ? minPoolSize : Math.max(1, poolSize / 4);
    }

    /**
     * Get JDBC URL for SQL databases
     */
    public String getJdbcUrl() {
        if (url.startsWith("jdbc:")) {
            return url;
        }

        // Convert simple format to JDBC URL
        String jdbcUrl;
        if ("postgresql".equalsIgnoreCase(type)) {
            jdbcUrl = "jdbc:postgresql://" + url;
        } else if ("mariadb".equalsIgnoreCase(type)) {
            jdbcUrl = "jdbc:mariadb://" + url;
        } else {
            return url;
        }

        // Add SSL if enabled
        if (ssl && !url.contains("ssl=")) {
            String separator = jdbcUrl.contains("?") ? "&" : "?";
            jdbcUrl += separator + "ssl=true";
        }

        return jdbcUrl;
    }

    /**
     * Get MongoDB connection string
     */
    public String getMongoConnectionString() {
        if (url.startsWith("mongodb://") || url.startsWith("mongodb+srv://")) {
            return url;
        }
        return "mongodb://" + url;
    }

    /**
     * Get a custom property value
     */
    public Object getProperty(String key) {
        return properties != null ? properties.get(key) : null;
    }

    /**
     * Get a custom property value with default
     */
    public Object getProperty(String key, Object defaultValue) {
        Object value = getProperty(key);
        return value != null ? value : defaultValue;
    }
}
package fr.nassime.helios.api.config;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for multiple database connections.
 * Allows connecting to PostgreSQL, MariaDB, and MongoDB simultaneously.
 */
@Getter
public class MultiDatabaseConfig {

    private final Map<String, DatabaseConfig> databases = new HashMap<>();
    private String defaultDatabase;

    /**
     * Add a database configuration
     */
    public void addDatabase(DatabaseConfig config) {
        databases.put(config.getName(), config);

        // First database becomes default
        if (defaultDatabase == null) {
            defaultDatabase = config.getName();
        }
    }

    /**
     * Set the default database for entities without explicit database specification
     */
    public void setDefaultDatabase(String name) {
        if (!databases.containsKey(name)) {
            throw new IllegalArgumentException("Database '" + name + "' not configured");
        }
        this.defaultDatabase = name;
    }

    /**
     * Get configuration for a specific database
     */
    public DatabaseConfig getDatabase(String name) {
        DatabaseConfig config = databases.get(name);
        if (config == null) {
            throw new IllegalArgumentException("Database '" + name + "' not configured");
        }
        return config;
    }

    /**
     * Check if a database is configured
     */
    public boolean hasDatabase(String name) {
        return databases.containsKey(name);
    }

    /**
     * Get the default database configuration
     */
    public DatabaseConfig getDefaultDatabase() {
        if (defaultDatabase == null) {
            throw new IllegalStateException("No default database configured");
        }
        return databases.get(defaultDatabase);
    }
}
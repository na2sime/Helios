package fr.nassime.helios.api.config;

import java.util.Map;
import java.util.Properties;

/**
 * Configuration interface for Helios ORM.
 * Each database provider can extend this with specific configuration options.
 */
public interface HeliosConfiguration {
    
    /**
     * Get the database provider name (e.g., "postgresql", "mongodb").
     */
    String getProvider();
    
    /**
     * Get the connection URL/string.
     */
    String getConnectionUrl();
    
    /**
     * Get the username for database authentication.
     */
    String getUsername();
    
    /**
     * Get the password for database authentication.
     */
    String getPassword();
    
    /**
     * Get the database name.
     */
    String getDatabase();
    
    /**
     * Get the schema name (for relational databases).
     */
    String getSchema();
    
    /**
     * Get connection pool settings.
     */
    ConnectionPoolConfig getConnectionPoolConfig();
    
    /**
     * Get additional provider-specific properties.
     */
    Properties getProperties();
    
    /**
     * Get a specific property value.
     */
    String getProperty(String key);
    
    /**
     * Get a specific property value with a default.
     */
    String getProperty(String key, String defaultValue);
    
    /**
     * Get all configuration as a map.
     */
    Map<String, Object> toMap();
}
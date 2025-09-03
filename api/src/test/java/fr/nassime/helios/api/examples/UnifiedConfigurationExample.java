package fr.nassime.helios.api.examples;

import fr.nassime.helios.api.config.UnifiedHeliosConfiguration;
import fr.nassime.helios.api.id.StandardIdConverters;

/**
 * Examples demonstrating the unified configuration system.
 * Shows how to configure different storage backends with a consistent API.
 */
public class UnifiedConfigurationExample {
    
    /**
     * PostgreSQL configuration example.
     */
    public static UnifiedHeliosConfiguration postgresqlExample() {
        return UnifiedHeliosConfiguration.builder()
                .postgresql()
                    .host("localhost", 5432)
                    .database("myapp")
                    .credentials("user", "password")
                    .schema("public")
                    .ssl(true)
                .and()
                .pooling(UnifiedHeliosConfiguration.poolConfig()
                    .maxSize(20)
                    .minSize(5)
                    .connectionTimeout(5000))
                .idConverter(Long.class, StandardIdConverters.longId())
                .property("timezone", "UTC")
                .build();
    }
    
    /**
     * MongoDB configuration example.
     */
    public static UnifiedHeliosConfiguration mongodbExample() {
        return UnifiedHeliosConfiguration.builder()
                .mongodb()
                    .connectionString("mongodb://localhost:27017/myapp")
                    .credentials("user", "password")
                    .authDatabase("admin")
                    .replicaSet("rs0")
                .and()
                .idConverter(String.class, StandardIdConverters.mongoObjectId())
                .property("readPreference", "primaryPreferred")
                .property("writeConcern", "majority")
                .build();
    }
    
    /**
     * MariaDB configuration example.
     */
    public static UnifiedHeliosConfiguration mariadbExample() {
        return UnifiedHeliosConfiguration.builder()
                .mariadb()
                    .host("db.example.com", 3306)
                    .database("production")
                    .credentials("app_user", "secure_password")
                    .charset("utf8mb4")
                    .ssl(true)
                .and()
                .pooling(UnifiedHeliosConfiguration.poolConfig()
                    .maxSize(15)
                    .minSize(3))
                .idConverter(Long.class, StandardIdConverters.longId())
                .build();
    }
    
    /**
     * Auto-detection configuration example.
     * Helios will choose the best available provider.
     */
    public static UnifiedHeliosConfiguration autoDetectExample() {
        return UnifiedHeliosConfiguration.autoDetect()
                .connectionUrl("jdbc:postgresql://localhost:5432/myapp") // fallback to PostgreSQL
                .credentials("user", "password")
                .pooling(UnifiedHeliosConfiguration.poolConfig()
                    .maxSize(10)
                    .minSize(2))
                .build();
    }
    
    /**
     * Hybrid configuration example.
     * Uses multiple backends simultaneously.
     */
    public static UnifiedHeliosConfiguration hybridExample() {
        return UnifiedHeliosConfiguration.builder()
                .provider("hybrid")
                .property("primary.provider", "postgresql")
                .property("primary.connectionUrl", "jdbc:postgresql://localhost:5432/main")
                .property("secondary.provider", "mongodb")
                .property("secondary.connectionString", "mongodb://localhost:27017/analytics")
                .idConverter(Long.class, StandardIdConverters.longId())
                .idConverter(String.class, StandardIdConverters.mongoObjectId())
                .build();
    }
    
    /**
     * Advanced configuration with custom properties.
     */
    public static UnifiedHeliosConfiguration advancedExample() {
        return UnifiedHeliosConfiguration.builder("postgresql")
                .host("cluster.example.com", 5432)
                .database("enterprise")
                .credentials("admin", "complex_password")
                .property("applicationName", "HeliosApp")
                .property("connectTimeout", 10)
                .property("socketTimeout", 30)
                .property("loginTimeout", 5)
                .property("preparedStatementCacheSize", 100)
                .property("batchSize", 50)
                .pooling(UnifiedHeliosConfiguration.poolConfig()
                    .maxSize(50)
                    .minSize(10)
                    .maxLifetime(3600000) // 1 hour
                    .idleTimeout(900000)  // 15 minutes
                    .connectionTimeout(10000)) // 10 seconds
                .idConverter(Long.class, StandardIdConverters.longId())
                .idConverter(java.util.UUID.class, StandardIdConverters.uuid())
                .build();
    }
    
    // Usage examples
    
    public static void main(String[] args) {
        // Example 1: Simple PostgreSQL setup
        UnifiedHeliosConfiguration pgConfig = postgresqlExample();
        System.out.println("PostgreSQL Provider: " + pgConfig.getProvider());
        System.out.println("Database: " + pgConfig.getStringProperty("database"));
        
        // Example 2: MongoDB setup
        UnifiedHeliosConfiguration mongoConfig = mongodbExample();
        System.out.println("MongoDB Connection String: " + pgConfig.getStringProperty("connectionUrl"));
        
        // Example 3: Auto-detection
        UnifiedHeliosConfiguration autoConfig = autoDetectExample();
        System.out.println("Auto-detect provider: " + autoConfig.getProvider());
    }
}
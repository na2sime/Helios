package fr.nassime.helios.api.examples;

import fr.nassime.helios.api.Helios;

/**
 * Examples demonstrating advanced configuration options with HeliosBuilder.
 * Shows connection pooling, timeouts, SSL, leak detection, and custom properties.
 */
public class AdvancedConfigurationExample {

    /**
     * Simple configuration (recommended for most use cases)
     */
    public static Helios simpleConfiguration() {
        return Helios.configure()
                .postgres("main", "localhost:5432/mydb", "user", "password")
                .mongo("analytics", "mongodb://localhost:27017/analytics")
                .defaultDatabase("main")
                .build();
    }

    /**
     * Configuration with custom pool sizes
     */
    public static Helios customPoolConfiguration() {
        return Helios.configure()
                .postgres("main", "localhost:5432/mydb", "user", "password", 20)
                .mariadb("legacy", "localhost:3306/olddb", "user", "password", 5)
                .mongo("analytics", "mongodb://localhost:27017/analytics")
                .defaultDatabase("main")
                .build();
    }

    /**
     * Advanced configuration with fine-grained control
     */
    public static Helios advancedConfiguration() {
        return Helios.configure()
                .database("main")
                    .postgres("localhost:5432/production", "admin", "secure_pass")
                    .poolSize(50, 10)  // Max: 50, Min idle: 10
                    .timeout(30000, 600000, 1800000)  // Connection, Idle, Max lifetime
                    .ssl(true)
                    .leakDetection(true)
                    .schema("public")
                    .property("preparedStatementCacheSize", 250)
                    .property("cachePrepStmts", true)
                .and()
                .database("analytics")
                    .mongo("mongodb://mongo-cluster:27017/analytics?replicaSet=rs0")
                    .property("readPreference", "secondaryPreferred")
                    .property("writeConcern", "majority")
                .and()
                .defaultDatabase("main")
                .build();
    }

    /**
     * High-traffic production configuration
     */
    public static Helios productionConfiguration() {
        return Helios.configure()
                .database("primary-db")
                    .postgres("prod-cluster.example.com:5432/maindb", "app_user", "prod_password")
                    .poolSize(100, 20)  // Large pool for high traffic
                    .timeout(10000, 300000, 1800000)  // Aggressive timeouts
                    .ssl(true)
                    .leakDetectionThreshold(120000)  // Detect leaks after 2 minutes
                    .property("applicationName", "HeliosApp")
                    .property("connectTimeout", 10)
                .and()
                .database("cache-db")
                    .mariadb("cache-server:3306/cache", "cache_user", "cache_pass")
                    .poolSize(30, 5)
                    .connectionTimeout(5000)  // Fast fail for cache
                    .ssl(false)  // Local network, no SSL needed
                .and()
                .database("logs")
                    .mongo("mongodb://log-cluster:27017/logs")
                    .property("maxPoolSize", 50)
                    .property("minPoolSize", 10)
                    .property("maxIdleTimeMS", 600000)
                .and()
                .defaultDatabase("primary-db")
                .build();
    }

    /**
     * Development configuration with debugging
     */
    public static Helios developmentConfiguration() {
        return Helios.configure()
                .database("dev-db")
                    .postgres("localhost:5432/dev", "dev", "dev")
                    .poolSize(5, 1)  // Small pool for development
                    .leakDetection(true)
                    .leakDetectionThreshold(30000)  // Detect leaks quickly in dev
                    .property("logUnclosedConnections", true)
                .and()
                .database("dev-mongo")
                    .mongo("mongodb://localhost:27017/dev")
                .and()
                .defaultDatabase("dev-db")
                .build();
    }

    /**
     * Multi-region configuration with read replicas
     */
    public static Helios multiRegionConfiguration() {
        return Helios.configure()
                .database("primary-write")
                    .postgres("primary.example.com:5432/db", "writer", "pass")
                    .poolSize(20, 5)
                    .ssl(true)
                .and()
                .database("replica-read")
                    .postgres("replica.example.com:5432/db", "reader", "pass")
                    .poolSize(50, 10)  // More connections for reads
                    .ssl(true)
                    .property("readOnly", true)
                .and()
                .database("analytics")
                    .mongo("mongodb://analytics-cluster:27017/metrics")
                    .property("readPreference", "nearest")
                .and()
                .defaultDatabase("primary-write")
                .build();
    }

    /**
     * Minimal configuration for testing
     */
    public static Helios testConfiguration() {
        return Helios.configure()
                .postgres("test", "localhost:5432/test", "test", "test")
                .build();
    }

    /**
     * SSL-only secure configuration
     */
    public static Helios secureConfiguration() {
        return Helios.configure()
                .database("secure-db")
                    .postgres("secure.example.com:5432/db", "user", "pass")
                    .ssl(true)
                    .poolSize(20, 5)
                    .property("sslmode", "require")
                    .property("sslcert", "/path/to/client-cert.pem")
                    .property("sslkey", "/path/to/client-key.pem")
                    .property("sslrootcert", "/path/to/ca-cert.pem")
                .and()
                .build();
    }

    public static void main(String[] args) {
        // Simple usage
        Helios helios = simpleConfiguration();
        System.out.println("Simple configuration created successfully");

        // Advanced usage
        Helios advanced = advancedConfiguration();
        System.out.println("Advanced configuration created successfully");

        // Remember to close when done
        helios.close();
        advanced.close();
    }
}
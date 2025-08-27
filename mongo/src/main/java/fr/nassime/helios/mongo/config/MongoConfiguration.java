package fr.nassime.helios.mongo.config;

import lombok.Builder;
import lombok.Getter;

/**
 * Configuration class for MongoDB connections.
 */
@Getter
@Builder
public class MongoConfiguration {
    
    /**
     * MongoDB connection string (URI).
     * Example: "mongodb://localhost:27017"
     */
    private final String connectionString;
    
    /**
     * Default database name.
     */
    private final String database;
    
    /**
     * Connection timeout in milliseconds.
     */
    @Builder.Default
    private final int connectionTimeoutMs = 10000;
    
    /**
     * Socket timeout in milliseconds.
     */
    @Builder.Default
    private final int socketTimeoutMs = 0; // 0 = no timeout
    
    /**
     * Server selection timeout in milliseconds.
     */
    @Builder.Default
    private final int serverSelectionTimeoutMs = 30000;
    
    /**
     * Maximum connection pool size.
     */
    @Builder.Default
    private final int maxConnectionPoolSize = 100;
    
    /**
     * Minimum connection pool size.
     */
    @Builder.Default
    private final int minConnectionPoolSize = 0;
    
    /**
     * Maximum connection idle time in milliseconds.
     */
    @Builder.Default
    private final int maxConnectionIdleTimeMs = 0; // 0 = no timeout
    
    /**
     * Maximum connection lifetime in milliseconds.
     */
    @Builder.Default
    private final int maxConnectionLifeTimeMs = 0; // 0 = no timeout
}
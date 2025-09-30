package fr.nassime.helios.api.config;

import lombok.Builder;
import lombok.Data;

/**
 * Configuration for database connection pooling.
 */
@Data
@Builder
public class ConnectionPoolConfig {
    
    /**
     * Maximum number of connections in the pool.
     */
    @Builder.Default
    private int maximumPoolSize = 10;
    
    /**
     * Minimum number of idle connections in the pool.
     */
    @Builder.Default
    private int minimumIdle = 1;
    
    /**
     * Maximum time (in milliseconds) to wait for a connection.
     */
    @Builder.Default
    private long connectionTimeout = 30000;
    
    /**
     * Maximum time (in milliseconds) a connection can stay idle.
     */
    @Builder.Default
    private long idleTimeout = 600000;
    
    /**
     * Maximum lifetime (in milliseconds) of a connection.
     */
    @Builder.Default
    private long maxLifetime = 1800000;
    
    /**
     * Connection leak detection threshold (in milliseconds).
     */
    @Builder.Default
    private long leakDetectionThreshold = 0;
}
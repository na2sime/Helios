package fr.nassime.helios.postgres.connection;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.nassime.helios.api.config.ConnectionPoolConfig;
import fr.nassime.helios.api.config.HeliosConfiguration;
import fr.nassime.helios.api.exception.HeliosException;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Manages PostgreSQL database connections using HikariCP.
 */
@Slf4j
public class PostgreSQLConnectionManager implements AutoCloseable {
    
    private final HikariDataSource dataSource;
    private final HeliosConfiguration configuration;
    
    public PostgreSQLConnectionManager(HeliosConfiguration configuration) {
        this.configuration = configuration;
        this.dataSource = createDataSource();
        log.info("PostgreSQL connection manager initialized for database: {}", configuration.getDatabase());
    }
    
    private HikariDataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        
        // Basic connection settings
        config.setJdbcUrl(configuration.getConnectionUrl());
        config.setUsername(configuration.getUsername());
        config.setPassword(configuration.getPassword());
        config.setDriverClassName("org.postgresql.Driver");
        
        // Schema setting
        if (configuration.getSchema() != null) {
            config.setSchema(configuration.getSchema());
        }
        
        // Pool configuration
        ConnectionPoolConfig poolConfig = configuration.getConnectionPoolConfig();
        config.setMaximumPoolSize(poolConfig.getMaximumPoolSize());
        config.setMinimumIdle(poolConfig.getMinimumIdle());
        config.setConnectionTimeout(poolConfig.getConnectionTimeout());
        config.setIdleTimeout(poolConfig.getIdleTimeout());
        config.setMaxLifetime(poolConfig.getMaxLifetime());
        
        if (poolConfig.getLeakDetectionThreshold() > 0) {
            config.setLeakDetectionThreshold(poolConfig.getLeakDetectionThreshold());
        }
        
        // PostgreSQL specific settings
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        
        // Additional properties from configuration
        configuration.getProperties().forEach((key, value) -> {
            config.addDataSourceProperty(key.toString(), value.toString());
        });
        
        return new HikariDataSource(config);
    }
    
    /**
     * Get a connection from the pool.
     */
    public Connection getConnection() throws SQLException {
        try {
            Connection connection = dataSource.getConnection();
            log.debug("Connection acquired from pool");
            return connection;
        } catch (SQLException e) {
            log.error("Failed to acquire connection from pool", e);
            throw new HeliosException("Unable to obtain database connection", e);
        }
    }
    
    /**
     * Check if the connection manager is closed.
     */
    public boolean isClosed() {
        return dataSource.isClosed();
    }
    
    /**
     * Get pool statistics for monitoring.
     */
    public String getPoolStats() {
        if (dataSource.isClosed()) {
            return "Pool is closed";
        }
        
        return String.format(
            "Pool Stats - Active: %d, Idle: %d, Total: %d, Waiting: %d",
            dataSource.getHikariPoolMXBean().getActiveConnections(),
            dataSource.getHikariPoolMXBean().getIdleConnections(),
            dataSource.getHikariPoolMXBean().getTotalConnections(),
            dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()
        );
    }
    
    @Override
    public void close() {
        if (!dataSource.isClosed()) {
            log.info("Closing PostgreSQL connection pool");
            dataSource.close();
        }
    }
}
package fr.nassime.helios.sql.session;

import fr.nassime.helios.api.HeliosSession;
import fr.nassime.helios.api.HeliosSessionFactory;
import fr.nassime.helios.api.config.HeliosConfiguration;
import fr.nassime.helios.api.exception.HeliosException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstract base class for SQL-based session factories.
 * Handles common HikariCP datasource management.
 */
@Slf4j
public abstract class AbstractSqlSessionFactory implements HeliosSessionFactory {
    
    private final HeliosConfiguration configuration;
    private final HikariDataSource dataSource;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final ThreadLocal<HeliosSession> currentSession = new ThreadLocal<>();
    
    protected AbstractSqlSessionFactory(HeliosConfiguration configuration) {
        this.configuration = configuration;
        this.dataSource = createDataSource(configuration);
        log.info("SQL session factory initialized");
    }
    
    private HikariDataSource createDataSource(HeliosConfiguration configuration) {
        HikariConfig config = new HikariConfig();
        
        config.setJdbcUrl(configuration.getConnectionUrl());
        config.setUsername(configuration.getUsername());
        config.setPassword(configuration.getPassword());
        
        // Set connection pool settings
        var poolConfig = configuration.getConnectionPoolConfig();
        config.setMinimumIdle(poolConfig.getMinimumIdle());
        config.setMaximumPoolSize(poolConfig.getMaximumPoolSize());
        config.setConnectionTimeout(poolConfig.getConnectionTimeout());
        config.setIdleTimeout(poolConfig.getIdleTimeout());
        config.setMaxLifetime(poolConfig.getMaxLifetime());
        
        // Set additional properties
        configuration.getProperties().forEach((key, value) -> 
            config.addDataSourceProperty(key.toString(), value));
        
        return new HikariDataSource(config);
    }
    
    /**
     * Create a new session instance. Must be implemented by subclasses.
     */
    protected abstract HeliosSession createSession();
    
    protected HikariDataSource getDataSource() {
        return dataSource;
    }
    
    @Override
    public HeliosSession openSession() {
        if (isClosed()) {
            throw new HeliosException("SessionFactory is closed");
        }
        
        HeliosSession session = createSession();
        log.debug("New SQL session opened");
        return session;
    }
    
    @Override
    public HeliosSession getCurrentSession() {
        HeliosSession session = currentSession.get();
        if (session == null || !isSessionActive(session)) {
            session = openSession();
            currentSession.set(session);
        }
        return session;
    }
    
    private boolean isSessionActive(HeliosSession session) {
        try {
            return session != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public HeliosConfiguration getConfiguration() {
        return configuration;
    }
    
    @Override
    public boolean isClosed() {
        return closed.get();
    }
    
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            log.info("Closing SQL session factory");
            
            // Close current session if exists
            HeliosSession session = currentSession.get();
            if (session != null) {
                try {
                    session.close();
                } catch (Exception e) {
                    log.warn("Error closing current session", e);
                }
                currentSession.remove();
            }
            
            // Close datasource
            try {
                if (dataSource != null && !dataSource.isClosed()) {
                    dataSource.close();
                }
            } catch (Exception e) {
                log.error("Error closing datasource", e);
            }
            
            log.info("SQL session factory closed");
        }
    }
}
package fr.nassime.helios.postgres.session;

import fr.nassime.helios.api.HeliosSession;
import fr.nassime.helios.api.HeliosSessionFactory;
import fr.nassime.helios.api.config.HeliosConfiguration;
import fr.nassime.helios.api.exception.HeliosException;
import fr.nassime.helios.postgres.connection.PostgreSQLConnectionManager;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * PostgreSQL implementation of HeliosSessionFactory.
 */
@Slf4j
public class PostgreSQLSessionFactory implements HeliosSessionFactory {
    
    private final HeliosConfiguration configuration;
    private final PostgreSQLConnectionManager connectionManager;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final ThreadLocal<HeliosSession> currentSession = new ThreadLocal<>();
    
    public PostgreSQLSessionFactory(HeliosConfiguration configuration) {
        this.configuration = configuration;
        this.connectionManager = new PostgreSQLConnectionManager(configuration);
        log.info("PostgreSQL session factory created");
    }
    
    @Override
    public HeliosSession openSession() {
        if (isClosed()) {
            throw new HeliosException("SessionFactory is closed");
        }
        
        PostgreSQLSession session = new PostgreSQLSession(connectionManager, configuration);
        log.debug("New PostgreSQL session opened");
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
            // Simple check - in a real implementation you might want to check the connection
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
            log.info("Closing PostgreSQL session factory");
            
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
            
            // Close connection manager
            try {
                connectionManager.close();
            } catch (Exception e) {
                log.error("Error closing connection manager", e);
            }
            
            log.info("PostgreSQL session factory closed");
        }
    }
}
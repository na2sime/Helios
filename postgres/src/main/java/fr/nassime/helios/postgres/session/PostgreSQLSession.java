package fr.nassime.helios.postgres.session;

import fr.nassime.helios.api.config.HeliosConfiguration;
import fr.nassime.helios.api.exception.HeliosException;
import fr.nassime.helios.api.query.Query;
import fr.nassime.helios.api.transaction.Transaction;
import fr.nassime.helios.postgres.connection.PostgreSQLConnectionManager;
import fr.nassime.helios.postgres.query.PostgreSQLQuery;
import fr.nassime.helios.postgres.sql.PostgreSQLSqlBuilder;
import fr.nassime.helios.postgres.transaction.PostgreSQLTransaction;
import fr.nassime.helios.sql.mapping.EntityMetadata;
import fr.nassime.helios.sql.query.SqlBuilder;
import fr.nassime.helios.sql.session.AbstractSqlSession;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * PostgreSQL implementation of HeliosSession.
 * Extends AbstractSqlSession to inherit common SQL functionality.
 */
@Slf4j
public class PostgreSQLSession extends AbstractSqlSession {
    
    private final PostgreSQLConnectionManager connectionManager;
    private final HeliosConfiguration configuration;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private PostgreSQLTransaction currentTransaction;
    
    public PostgreSQLSession(PostgreSQLConnectionManager connectionManager, HeliosConfiguration configuration) {
        super();
        this.connectionManager = connectionManager;
        this.configuration = configuration;
        log.debug("PostgreSQL session created");
    }
    
    @Override
    protected SqlBuilder.PreparedQuery buildInsertQuery(EntityMetadata metadata, Object entity) {
        // Use PostgreSQL-specific insert builder with RETURNING clause
        return PostgreSQLSqlBuilder.buildInsert(metadata, entity);
    }
    
    @Override
    protected Query createSqlQuery(String queryString) {
        return new PostgreSQLQuery<>(Object.class, this, queryString);
    }
    
    @Override
    protected <T> Query<T> createEntityQuery(Class<T> entityClass) {
        return new PostgreSQLQuery<>(entityClass, this);
    }
    
    @Override
    public <T> T executeWithConnection(Function<Connection, T> operation) {
        if (isClosed()) {
            throw new HeliosException("Session is closed");
        }
        
        try {
            Connection connection = connectionManager.getConnection();
            connection.setAutoCommit(false);
            try {
                T result = operation.apply(connection);
                connection.commit();
                return result;
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.close();
            }
        } catch (SQLException e) {
            throw new HeliosException("Database operation failed", e);
        }
    }
    
    @Override
    public Transaction beginTransaction() {
        if (isClosed()) {
            throw new HeliosException("Session is closed");
        }
        
        if (currentTransaction != null && currentTransaction.isActive()) {
            throw new HeliosException("Transaction already active");
        }
        
        try {
            Connection connection = connectionManager.getConnection();
            currentTransaction = new PostgreSQLTransaction(connection);
            log.debug("PostgreSQL transaction started");
            return currentTransaction;
        } catch (SQLException e) {
            throw new HeliosException("Failed to begin transaction", e);
        }
    }
    
    @Override
    public void flush() {
        if (isClosed()) {
            throw new HeliosException("Session is closed");
        }
        
        if (currentTransaction != null && currentTransaction.isActive()) {
            try {
                currentTransaction.commit();
                log.debug("PostgreSQL session flushed - transaction committed");
            } catch (Exception e) {
                log.error("Failed to flush session", e);
                throw new HeliosException("Failed to flush session", e);
            }
        } else {
            log.debug("No active transaction to flush");
        }
    }
    
    @Override
    public void clear() {
        if (isClosed()) {
            throw new HeliosException("Session is closed");
        }
        
        // Close current transaction without committing (rollback)
        if (currentTransaction != null && currentTransaction.isActive()) {
            try {
                currentTransaction.rollback();
                currentTransaction = null;
                log.debug("PostgreSQL session cleared - transaction rolled back");
            } catch (Exception e) {
                log.error("Failed to clear session", e);
                throw new HeliosException("Failed to clear session", e);
            }
        } else {
            log.debug("No active transaction to clear");
        }
    }
    
    private boolean isClosed() {
        return closed.get();
    }
    
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            // Rollback active transaction if exists
            if (currentTransaction != null && currentTransaction.isActive()) {
                try {
                    currentTransaction.rollback();
                } catch (Exception e) {
                    log.warn("Error rolling back transaction during session close", e);
                }
            }
            
            log.debug("PostgreSQL session closed");
        }
    }
}
package fr.nassime.helios.mariadb.session;

import fr.nassime.helios.api.exception.HeliosException;
import fr.nassime.helios.api.query.Query;
import fr.nassime.helios.api.transaction.Transaction;
import fr.nassime.helios.mariadb.query.MariaDBQuery;
import fr.nassime.helios.mariadb.sql.MariaDBSqlBuilder;
import fr.nassime.helios.mariadb.transaction.MariaDBTransaction;
import fr.nassime.helios.sql.mapping.EntityMetadata;
import fr.nassime.helios.sql.query.SqlBuilder;
import fr.nassime.helios.sql.session.AbstractSqlSession;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * MariaDB implementation of HeliosSession.
 * Extends AbstractSqlSession to inherit common SQL functionality.
 */
@Slf4j
public class MariaDBSession extends AbstractSqlSession {
    
    private final HikariDataSource dataSource;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private MariaDBTransaction currentTransaction;
    
    public MariaDBSession(HikariDataSource dataSource) {
        super();
        this.dataSource = dataSource;
        log.debug("MariaDB session created");
    }
    
    @Override
    protected Query createSqlQuery(String queryString) {
        return new MariaDBQuery(queryString, dataSource);
    }
    
    @Override
    protected <T> Query<T> createEntityQuery(Class<T> entityClass) {
        return new MariaDBQuery<>(entityClass, this, dataSource);
    }
    
    @Override
    protected SqlBuilder.PreparedQuery buildInsertQuery(EntityMetadata metadata, Object entity) {
        // Use MariaDB-specific insert builder
        return MariaDBSqlBuilder.buildInsert(metadata, entity);
    }
    
    @Override
    public <T> T executeWithConnection(Function<Connection, T> operation) {
        if (isClosed()) {
            throw new HeliosException("Session is closed");
        }
        
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = operation.apply(connection);
                connection.commit();
                return result;
            } catch (Exception e) {
                connection.rollback();
                throw e;
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
            Connection connection = dataSource.getConnection();
            currentTransaction = new MariaDBTransaction(connection);
            log.debug("MariaDB transaction started");
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
                log.debug("MariaDB session flushed - transaction committed");
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
                log.debug("MariaDB session cleared - transaction rolled back");
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
            
            log.debug("MariaDB session closed");
        }
    }
}
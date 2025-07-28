package fr.nassime.helios.postgres.session;

import fr.nassime.helios.api.HeliosSession;
import fr.nassime.helios.api.config.HeliosConfiguration;
import fr.nassime.helios.api.exception.EntityNotFoundException;
import fr.nassime.helios.api.exception.HeliosException;
import fr.nassime.helios.api.query.Query;
import fr.nassime.helios.api.transaction.Transaction;
import fr.nassime.helios.postgres.connection.PostgreSQLConnectionManager;
import fr.nassime.helios.postgres.query.PostgreSQLQuery;
import fr.nassime.helios.postgres.transaction.PostgreSQLTransaction;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * PostgreSQL implementation of HeliosSession.
 */
@Slf4j
public class PostgreSQLSession implements HeliosSession {
    
    private final PostgreSQLConnectionManager connectionManager;
    private final HeliosConfiguration configuration;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private PostgreSQLTransaction currentTransaction;
    
    public PostgreSQLSession(PostgreSQLConnectionManager connectionManager, HeliosConfiguration configuration) {
        this.connectionManager = connectionManager;
        this.configuration = configuration;
        log.debug("PostgreSQL session created");
    }
    
    @Override
    public <T, ID> Optional<T> findById(Class<T> entityClass, ID id) {
        if (isClosed()) {
            throw new HeliosException("Session is closed");
        }
        
        // TODO: Implement entity finding logic
        // For now, returning empty - will be implemented with entity mappers
        log.debug("Finding entity {} with id {}", entityClass.getSimpleName(), id);
        return Optional.empty();
    }
    
    @Override
    public <T> List<T> findAll(Class<T> entityClass) {
        if (isClosed()) {
            throw new HeliosException("Session is closed");
        }
        
        // TODO: Implement find all logic
        log.debug("Finding all entities of type {}", entityClass.getSimpleName());
        return List.of();
    }
    
    @Override
    public <T> T save(T entity) {
        if (isClosed()) {
            throw new HeliosException("Session is closed");
        }
        
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        
        // TODO: Implement save logic with entity mappers
        log.debug("Saving entity {}", entity.getClass().getSimpleName());
        return entity;
    }
    
    @Override
    public <T> boolean delete(T entity) {
        if (isClosed()) {
            throw new HeliosException("Session is closed");
        }
        
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        
        // TODO: Implement delete logic
        log.debug("Deleting entity {}", entity.getClass().getSimpleName());
        return false;
    }
    
    @Override
    public <T> Query<T> createQuery(Class<T> entityClass) {
        if (isClosed()) {
            throw new HeliosException("Session is closed");
        }
        
        return new PostgreSQLQuery<>(entityClass, this);
    }
    
    @Override
    public <T> List<T> executeNativeQuery(String query, Class<T> resultClass, Object... parameters) {
        if (isClosed()) {
            throw new HeliosException("Session is closed");
        }
        
        return executeWithConnection(connection -> {
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                // Set parameters
                for (int i = 0; i < parameters.length; i++) {
                    stmt.setObject(i + 1, parameters[i]);
                }
                
                try (ResultSet rs = stmt.executeQuery()) {
                    // TODO: Map ResultSet to entities
                    // For now, returning empty list
                    return List.of();
                }
            } catch (SQLException e) {
                throw new HeliosException("Failed to execute native query", e);
            }
        });
    }
    
    @Override
    public int executeUpdate(String query, Object... parameters) {
        if (isClosed()) {
            throw new HeliosException("Session is closed");
        }
        
        return executeWithConnection(connection -> {
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                // Set parameters
                for (int i = 0; i < parameters.length; i++) {
                    stmt.setObject(i + 1, parameters[i]);
                }
                
                return stmt.executeUpdate();
            } catch (SQLException e) {
                throw new HeliosException("Failed to execute update query", e);
            }
        });
    }
    
    @Override
    public <T> void loadRelation(T entity, String relationName) {
        if (isClosed()) {
            throw new HeliosException("Session is closed");
        }
        
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        
        // TODO: Implement relation loading
        log.debug("Loading relation {} for entity {}", relationName, entity.getClass().getSimpleName());
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
    public <T> T executeInTransaction(Function<HeliosSession, T> operation) {
        Transaction tx = beginTransaction();
        try {
            T result = operation.apply(this);
            tx.commit();
            return result;
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }
    
    @Override
    public void executeInTransaction(Consumer<HeliosSession> operation) {
        executeInTransaction(session -> {
            operation.accept(session);
            return null;
        });
    }
    
    /**
     * Execute operation with direct connection access.
     */
    public <T> T executeWithConnection(Function<Connection, T> operation) {
        Transaction tx = beginTransaction();
        try {
            Connection connection = ((PostgreSQLTransaction) tx).getConnection();
            T result = operation.apply(connection);
            tx.commit();
            return result;
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }
    
    @Override
    public void flush() {
        if (isClosed()) {
            throw new HeliosException("Session is closed");
        }
        
        // TODO: Implement flush logic - commit pending changes
        log.debug("Flushing PostgreSQL session");
    }
    
    @Override
    public void clear() {
        if (isClosed()) {
            throw new HeliosException("Session is closed");
        }
        
        // TODO: Implement clear logic - clear session cache
        log.debug("Clearing PostgreSQL session");
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
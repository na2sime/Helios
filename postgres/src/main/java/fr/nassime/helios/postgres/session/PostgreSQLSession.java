package fr.nassime.helios.postgres.session;

import fr.nassime.helios.api.HeliosSession;
import fr.nassime.helios.api.config.HeliosConfiguration;
import fr.nassime.helios.api.exception.EntityNotFoundException;
import fr.nassime.helios.api.exception.HeliosException;
import fr.nassime.helios.api.query.Query;
import fr.nassime.helios.api.transaction.Transaction;
import fr.nassime.helios.postgres.connection.PostgreSQLConnectionManager;
import fr.nassime.helios.postgres.mapping.EntityMapper;
import fr.nassime.helios.postgres.mapping.EntityMetadata;
import fr.nassime.helios.postgres.mapping.ResultSetMapper;
import fr.nassime.helios.postgres.query.PostgreSQLQuery;
import fr.nassime.helios.postgres.relation.RelationLoader;
import fr.nassime.helios.postgres.sql.SqlBuilder;
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
        
        if (id == null) {
            return Optional.empty();
        }
        
        log.debug("Finding entity {} with id {}", entityClass.getSimpleName(), id);
        
        EntityMetadata metadata = EntityMapper.getMetadata(entityClass);
        SqlBuilder.PreparedQuery query = SqlBuilder.buildSelectById(metadata);
        
        return executeWithConnection(connection -> {
            try (PreparedStatement stmt = connection.prepareStatement(query.getSql())) {
                stmt.setObject(1, id);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    ResultSetMapper mapper = new ResultSetMapper();
                    T entity = mapper.mapToEntity(rs, entityClass);
                    return Optional.ofNullable(entity);
                }
            } catch (SQLException e) {
                throw new HeliosException("Failed to find entity by ID", e);
            }
        });
    }
    
    @Override
    public <T> List<T> findAll(Class<T> entityClass) {
        if (isClosed()) {
            throw new HeliosException("Session is closed");
        }
        
        log.debug("Finding all entities of type {}", entityClass.getSimpleName());
        
        EntityMetadata metadata = EntityMapper.getMetadata(entityClass);
        SqlBuilder.PreparedQuery query = SqlBuilder.buildSelectAll(metadata);
        
        return executeWithConnection(connection -> {
            try (PreparedStatement stmt = connection.prepareStatement(query.getSql());
                 ResultSet rs = stmt.executeQuery()) {
                
                ResultSetMapper mapper = new ResultSetMapper();
                return mapper.mapToList(rs, entityClass);
            } catch (SQLException e) {
                throw new HeliosException("Failed to find all entities", e);
            }
        });
    }
    
    @Override
    public <T> T save(T entity) {
        if (isClosed()) {
            throw new HeliosException("Session is closed");
        }
        
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        
        @SuppressWarnings("unchecked")
        Class<T> entityClass = (Class<T>) entity.getClass();
        EntityMetadata metadata = EntityMapper.getMetadata(entityClass);
        
        // Check if entity has ID (update) or not (insert)
        Object idValue = metadata.getColumns().get(metadata.getIdField().getName()).getValue(entity);
        boolean isUpdate = idValue != null && (!(idValue instanceof Number) || ((Number) idValue).longValue() != 0);
        
        if (isUpdate) {
            log.debug("Updating entity {} with id {}", entityClass.getSimpleName(), idValue);
            return updateEntity(entity, metadata);
        } else {
            log.debug("Inserting new entity {}", entityClass.getSimpleName());
            return insertEntity(entity, metadata);
        }
    }
    
    private <T> T insertEntity(T entity, EntityMetadata metadata) {
        SqlBuilder.PreparedQuery query = SqlBuilder.buildInsert(metadata, entity);
        
        return executeWithConnection(connection -> {
            try (PreparedStatement stmt = connection.prepareStatement(query.getSql())) {
                // Set parameters
                List<Object> parameters = query.getParameters();
                for (int i = 0; i < parameters.size(); i++) {
                    stmt.setObject(i + 1, parameters.get(i));
                }
                
                if (metadata.isIdGenerated()) {
                    // Get generated ID
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            Object generatedId = rs.getObject(1);
                            metadata.getColumns().get(metadata.getIdField().getName()).setValue(entity, generatedId);
                        }
                    }
                } else {
                    stmt.executeUpdate();
                }
                
                return entity;
            } catch (SQLException e) {
                throw new HeliosException("Failed to insert entity", e);
            }
        });
    }
    
    private <T> T updateEntity(T entity, EntityMetadata metadata) {
        SqlBuilder.PreparedQuery query = SqlBuilder.buildUpdate(metadata, entity);
        
        return executeWithConnection(connection -> {
            try (PreparedStatement stmt = connection.prepareStatement(query.getSql())) {
                // Set parameters
                List<Object> parameters = query.getParameters();
                for (int i = 0; i < parameters.size(); i++) {
                    stmt.setObject(i + 1, parameters.get(i));
                }
                
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected == 0) {
                    throw new HeliosException("No rows affected during update - entity may not exist");
                }
                
                return entity;
            } catch (SQLException e) {
                throw new HeliosException("Failed to update entity", e);
            }
        });
    }
    
    @Override
    public <T> boolean delete(T entity) {
        if (isClosed()) {
            throw new HeliosException("Session is closed");
        }
        
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        
        EntityMetadata metadata = EntityMapper.getMetadata(entity.getClass());
        Object idValue = metadata.getColumns().get(metadata.getIdField().getName()).getValue(entity);
        
        if (idValue == null) {
            throw new HeliosException("Cannot delete entity without ID");
        }
        
        log.debug("Deleting entity {} with id {}", entity.getClass().getSimpleName(), idValue);
        
        SqlBuilder.PreparedQuery query = SqlBuilder.buildDelete(metadata, entity);
        
        return executeWithConnection(connection -> {
            try (PreparedStatement stmt = connection.prepareStatement(query.getSql())) {
                // Set parameters
                List<Object> parameters = query.getParameters();
                for (int i = 0; i < parameters.size(); i++) {
                    stmt.setObject(i + 1, parameters.get(i));
                }
                
                int rowsAffected = stmt.executeUpdate();
                return rowsAffected > 0;
            } catch (SQLException e) {
                throw new HeliosException("Failed to delete entity", e);
            }
        });
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
                    ResultSetMapper mapper = new ResultSetMapper();
                    return mapper.mapToList(rs, resultClass);
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
        
        RelationLoader relationLoader = new RelationLoader(this);
        relationLoader.loadRelation(entity, relationName);
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
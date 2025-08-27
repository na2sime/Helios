package fr.nassime.helios.sql.session;

import fr.nassime.helios.api.HeliosSession;
import fr.nassime.helios.api.exception.HeliosException;
import fr.nassime.helios.api.query.Query;
import fr.nassime.helios.api.transaction.Transaction;
import fr.nassime.helios.sql.mapping.EntityMapper;
import fr.nassime.helios.sql.mapping.EntityMetadata;
import fr.nassime.helios.sql.mapping.ResultSetMapper;
import fr.nassime.helios.sql.query.SqlBuilder;
import fr.nassime.helios.sql.relation.RelationLoader;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Abstract base class for SQL-based Helios sessions.
 * Provides common functionality for all SQL database implementations.
 */
@Slf4j
public abstract class AbstractSqlSession implements HeliosSession {
    
    private final ResultSetMapper resultSetMapper;
    private final RelationLoader relationLoader;
    
    protected AbstractSqlSession() {
        this.resultSetMapper = new ResultSetMapper();
        this.relationLoader = new RelationLoader(this::executeWithConnection);
    }
    
    @Override
    public <T> T save(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        
        EntityMetadata metadata = EntityMapper.getMetadata(entity.getClass());
        log.debug("Saving entity of type: {}", entity.getClass().getSimpleName());
        
        // Check if entity has an ID (update vs insert)
        Object id = metadata.getColumns().get(metadata.getIdField().getName()).getValue(entity);
        
        if (id == null || (metadata.isIdGenerated() && isNewEntity(id))) {
            return insert(entity, metadata);
        } else {
            return update(entity, metadata);
        }
    }
    
    @Override
    public <T, ID> Optional<T> findById(Class<T> entityClass, ID id) {
        if (id == null) {
            return Optional.empty();
        }
        
        EntityMetadata metadata = EntityMapper.getMetadata(entityClass);
        SqlBuilder.PreparedQuery query = SqlBuilder.buildSelectById(metadata);
        
        log.debug("Finding entity by ID: {} for class: {}", id, entityClass.getSimpleName());
        
        return executeWithConnection(connection -> {
            try (PreparedStatement stmt = connection.prepareStatement(query.getSql())) {
                stmt.setObject(1, id);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    T entity = resultSetMapper.mapToEntity(rs, entityClass);
                    return Optional.ofNullable(entity);
                }
            } catch (SQLException e) {
                throw new HeliosException("Failed to find entity by ID", e);
            }
        });
    }
    
    @Override
    public <T> List<T> findAll(Class<T> entityClass) {
        EntityMetadata metadata = EntityMapper.getMetadata(entityClass);
        SqlBuilder.PreparedQuery query = SqlBuilder.buildSelectAll(metadata);
        
        log.debug("Finding all entities for class: {}", entityClass.getSimpleName());
        
        return executeWithConnection(connection -> {
            try (PreparedStatement stmt = connection.prepareStatement(query.getSql())) {
                try (ResultSet rs = stmt.executeQuery()) {
                    return resultSetMapper.mapToList(rs, entityClass);
                }
            } catch (SQLException e) {
                throw new HeliosException("Failed to find all entities", e);
            }
        });
    }
    
    @Override
    public <T> boolean delete(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        
        EntityMetadata metadata = EntityMapper.getMetadata(entity.getClass());
        
        // Verify entity has an ID
        Object id = metadata.getColumns().get(metadata.getIdField().getName()).getValue(entity);
        if (id == null) {
            throw new HeliosException("Cannot delete entity without ID: " + entity.getClass().getSimpleName());
        }
        
        SqlBuilder.PreparedQuery query = SqlBuilder.buildDelete(metadata, entity);
        
        log.debug("Deleting entity of type: {}", entity.getClass().getSimpleName());
        
        return executeWithConnection(connection -> {
            try (PreparedStatement stmt = connection.prepareStatement(query.getSql())) {
                setParameters(stmt, query.getParameters());
                int rowsAffected = stmt.executeUpdate();
                
                log.debug("Deleted {} rows", rowsAffected);
                return rowsAffected > 0;
            } catch (SQLException e) {
                throw new HeliosException("Failed to delete entity", e);
            }
        });
    }
    
    @Override
    public <T> void loadRelation(T entity, String relationName) {
        relationLoader.loadRelation(entity, relationName);
    }
    
    @Override
    public <T> Query<T> createQuery(Class<T> entityClass) {
        return createEntityQuery(entityClass);
    }
    
    /**
     * Create an entity-based query. Should be implemented by subclasses.
     */
    protected abstract <T> Query<T> createEntityQuery(Class<T> entityClass);
    
    @Override
    public <T> List<T> executeNativeQuery(String query, Class<T> resultClass, Object... parameters) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be null or empty");
        }
        if (resultClass == null) {
            throw new IllegalArgumentException("Result class cannot be null");
        }
        
        log.debug("Executing native query: {} with result class: {}", query, resultClass.getSimpleName());
        
        return executeWithConnection(connection -> {
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                // Set parameters if provided
                if (parameters != null && parameters.length > 0) {
                    for (int i = 0; i < parameters.length; i++) {
                        stmt.setObject(i + 1, parameters[i]);
                    }
                }
                
                try (ResultSet rs = stmt.executeQuery()) {
                    List<T> results = resultSetMapper.mapToList(rs, resultClass);
                    log.debug("Native query returned {} results", results.size());
                    return results;
                }
            } catch (SQLException e) {
                throw new HeliosException("Failed to execute native query: " + query, e);
            }
        });
    }
    
    @Override
    public int executeUpdate(String query, Object... parameters) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be null or empty");
        }
        
        log.debug("Executing update query: {}", query);
        
        return executeWithConnection(connection -> {
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                // Set parameters if provided
                if (parameters != null && parameters.length > 0) {
                    for (int i = 0; i < parameters.length; i++) {
                        stmt.setObject(i + 1, parameters[i]);
                    }
                }
                
                int rowsAffected = stmt.executeUpdate();
                log.debug("Update query affected {} rows", rowsAffected);
                return rowsAffected;
            } catch (SQLException e) {
                throw new HeliosException("Failed to execute update query: " + query, e);
            }
        });
    }
    
    @Override
    public <T> T executeInTransaction(Function<HeliosSession, T> operation) {
        if (operation == null) {
            throw new IllegalArgumentException("Operation cannot be null");
        }
        
        Transaction tx = beginTransaction();
        try {
            T result = operation.apply(this);
            tx.commit();
            log.debug("Transaction completed successfully");
            return result;
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
                log.debug("Transaction rolled back due to exception: {}", e.getMessage());
            }
            throw e;
        }
    }
    
    @Override
    public void executeInTransaction(java.util.function.Consumer<HeliosSession> operation) {
        if (operation == null) {
            throw new IllegalArgumentException("Operation cannot be null");
        }
        
        Transaction tx = beginTransaction();
        try {
            operation.accept(this);
            tx.commit();
            log.debug("Transaction completed successfully");
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
                log.debug("Transaction rolled back due to exception: {}", e.getMessage());
            }
            throw e;
        }
    }
    
    @Override
    public void flush() {
        // This method should be implemented by subclasses
        throw new UnsupportedOperationException("flush not implemented yet");
    }
    
    @Override
    public void clear() {
        // This method should be implemented by subclasses
        throw new UnsupportedOperationException("clear not implemented yet");
    }
    
    @Override
    public Transaction beginTransaction() {
        // This method should be implemented by subclasses
        throw new UnsupportedOperationException("beginTransaction not implemented yet");
    }
    
    @Override
    public void close() {
        // This method should be implemented by subclasses
        throw new UnsupportedOperationException("close not implemented yet");
    }
    
    /**
     * Create a query with a custom query string.
     * This is for backwards compatibility with the createQuery(String) method.
     */
    public Query createQuery(String query) {
        return createSqlQuery(query);
    }
    
    /**
     * Insert a new entity.
     */
    protected <T> T insert(T entity, EntityMetadata metadata) {
        SqlBuilder.PreparedQuery query = buildInsertQuery(metadata, entity);
        
        return executeWithConnection(connection -> {
            try (PreparedStatement stmt = connection.prepareStatement(query.getSql(), 
                    metadata.isIdGenerated() ? PreparedStatement.RETURN_GENERATED_KEYS : PreparedStatement.NO_GENERATED_KEYS)) {
                
                setParameters(stmt, query.getParameters());
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected == 0) {
                    throw new HeliosException("Failed to insert entity - no rows affected");
                }
                
                // Handle generated keys
                if (metadata.isIdGenerated()) {
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            Object generatedId = generatedKeys.getObject(1);
                            metadata.getColumns().get(metadata.getIdField().getName()).setValue(entity, generatedId);
                        }
                    }
                }
                
                log.debug("Inserted entity with {} rows affected", rowsAffected);
                return entity;
            } catch (SQLException e) {
                throw new HeliosException("Failed to insert entity", e);
            }
        });
    }
    
    /**
     * Update an existing entity.
     */
    protected <T> T update(T entity, EntityMetadata metadata) {
        SqlBuilder.PreparedQuery query = SqlBuilder.buildUpdate(metadata, entity);
        
        executeWithConnection(connection -> {
            try (PreparedStatement stmt = connection.prepareStatement(query.getSql())) {
                setParameters(stmt, query.getParameters());
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected == 0) {
                    throw new HeliosException("No rows were updated - entity may not exist");
                }
                
                log.debug("Updated entity with {} rows affected", rowsAffected);
                return null;
            } catch (SQLException e) {
                throw new HeliosException("Failed to update entity", e);
            }
        });
        
        return entity;
    }
    
    /**
     * Check if an entity is new (for ID generation logic).
     */
    protected boolean isNewEntity(Object id) {
        if (id instanceof Number) {
            return ((Number) id).longValue() <= 0;
        }
        return false;
    }
    
    /**
     * Set parameters on a prepared statement.
     */
    protected void setParameters(PreparedStatement stmt, List<Object> parameters) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            stmt.setObject(i + 1, parameters.get(i));
        }
    }
    
    /**
     * Build database-specific insert query.
     * Can be overridden by subclasses for database-specific features.
     */
    protected SqlBuilder.PreparedQuery buildInsertQuery(EntityMetadata metadata, Object entity) {
        return SqlBuilder.buildInsert(metadata, entity);
    }
    
    /**
     * Create a database-specific query implementation.
     */
    protected abstract Query createSqlQuery(String queryString);
    
    /**
     * Execute a function with a database connection.
     * This method should be implemented by subclasses to provide connection management.
     */
    public abstract <T> T executeWithConnection(Function<Connection, T> operation);
}
package fr.nassime.helios.api;

import fr.nassime.helios.api.query.Query;
import fr.nassime.helios.api.transaction.Transaction;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Main interface for Helios ORM session management.
 * Provides a unified API for database operations across different providers.
 */
public interface HeliosSession extends AutoCloseable {
    
    /**
     * Find an entity by its ID.
     */
    <T, ID> Optional<T> findById(Class<T> entityClass, ID id);
    
    /**
     * Find all entities of a given type.
     */
    <T> List<T> findAll(Class<T> entityClass);
    
    /**
     * Save or update an entity.
     */
    <T> T save(T entity);
    
    /**
     * Delete an entity.
     */
    <T> boolean delete(T entity);
    
    /**
     * Create a query for the given entity type.
     */
    <T> Query<T> createQuery(Class<T> entityClass);
    
    /**
     * Execute a native query.
     */
    <T> List<T> executeNativeQuery(String query, Class<T> resultClass, Object... parameters);
    
    /**
     * Execute an update query.
     */
    int executeUpdate(String query, Object... parameters);
    
    /**
     * Load a relationship lazily.
     */
    <T> void loadRelation(T entity, String relationName);
    
    /**
     * Start a new transaction.
     */
    Transaction beginTransaction();
    
    /**
     * Execute operations within a transaction.
     */
    <T> T executeInTransaction(Function<HeliosSession, T> operation);
    
    /**
     * Execute operations within a transaction without return value.
     */
    void executeInTransaction(Consumer<HeliosSession> operation);
    
    /**
     * Flush pending operations to the database.
     */
    void flush();
    
    /**
     * Clear the session cache.
     */
    void clear();
    
    @Override
    void close();
}
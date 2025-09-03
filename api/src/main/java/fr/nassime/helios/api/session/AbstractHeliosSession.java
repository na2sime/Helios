package fr.nassime.helios.api.session;

import fr.nassime.helios.api.HeliosSession;
import fr.nassime.helios.api.query.Query;
import fr.nassime.helios.api.transaction.Transaction;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Abstract base implementation of HeliosSession providing common functionality
 * across all storage backends (SQL, Document, etc.).
 * 
 * This class implements the Template Method pattern to define the skeleton
 * of session operations while letting subclasses override specific steps.
 */
public abstract class AbstractHeliosSession implements HeliosSession {
    
    protected volatile boolean closed = false;
    protected Transaction currentTransaction;
    
    /**
     * Validates that the session is not closed.
     * 
     * @throws IllegalStateException if the session is closed
     */
    protected final void checkNotClosed() {
        if (closed) {
            throw new IllegalStateException("Session is closed");
        }
    }
    
    /**
     * Validates that the entity is not null.
     * 
     * @param entity the entity to validate
     * @throws IllegalArgumentException if entity is null
     */
    protected final void validateEntity(Object entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
    }
    
    /**
     * Validates that the entity class is not null.
     * 
     * @param entityClass the entity class to validate
     * @throws IllegalArgumentException if entityClass is null
     */
    protected final void validateEntityClass(Class<?> entityClass) {
        if (entityClass == null) {
            throw new IllegalArgumentException("Entity class cannot be null");
        }
    }
    
    /**
     * Validates that the ID is not null.
     * 
     * @param id the ID to validate
     * @throws IllegalArgumentException if id is null
     */
    protected final void validateId(Object id) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }
    }
    
    @Override
    public final <T, ID> Optional<T> findById(Class<T> entityClass, ID id) {
        checkNotClosed();
        validateEntityClass(entityClass);
        validateId(id);
        
        return doFindById(entityClass, id);
    }
    
    @Override
    public final <T> List<T> findAll(Class<T> entityClass) {
        checkNotClosed();
        validateEntityClass(entityClass);
        
        return doFindAll(entityClass);
    }
    
    @Override
    public final <T> T save(T entity) {
        checkNotClosed();
        validateEntity(entity);
        
        return doSave(entity);
    }
    
    @Override
    public final <T> boolean delete(T entity) {
        checkNotClosed();
        validateEntity(entity);
        
        return doDelete(entity);
    }
    
    @Override
    public final <T> Query<T> createQuery(Class<T> entityClass) {
        checkNotClosed();
        validateEntityClass(entityClass);
        
        return doCreateQuery(entityClass);
    }
    
    @Override
    public final <T> List<T> executeNativeQuery(String query, Class<T> resultClass, Object... parameters) {
        checkNotClosed();
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be null or empty");
        }
        validateEntityClass(resultClass);
        
        return doExecuteNativeQuery(query, resultClass, parameters);
    }
    
    @Override
    public final int executeUpdate(String query, Object... parameters) {
        checkNotClosed();
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be null or empty");
        }
        
        return doExecuteUpdate(query, parameters);
    }
    
    @Override
    public final <T> void loadRelation(T entity, String relationName) {
        checkNotClosed();
        validateEntity(entity);
        if (relationName == null || relationName.trim().isEmpty()) {
            throw new IllegalArgumentException("Relation name cannot be null or empty");
        }
        
        doLoadRelation(entity, relationName);
    }
    
    @Override
    public final Transaction beginTransaction() {
        checkNotClosed();
        
        if (currentTransaction != null && currentTransaction.isActive()) {
            throw new IllegalStateException("Transaction already active");
        }
        
        currentTransaction = doBeginTransaction();
        return currentTransaction;
    }
    
    @Override
    public final <T> T executeInTransaction(Function<HeliosSession, T> operation) {
        checkNotClosed();
        if (operation == null) {
            throw new IllegalArgumentException("Operation cannot be null");
        }
        
        Transaction tx = beginTransaction();
        try {
            T result = operation.apply(this);
            tx.commit();
            return result;
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
    }
    
    @Override
    public final void executeInTransaction(Consumer<HeliosSession> operation) {
        executeInTransaction(session -> {
            operation.accept(session);
            return null;
        });
    }
    
    @Override
    public final void flush() {
        checkNotClosed();
        doFlush();
    }
    
    @Override
    public final void clear() {
        checkNotClosed();
        doClear();
    }
    
    @Override
    public final void close() {
        if (!closed) {
            try {
                if (currentTransaction != null && currentTransaction.isActive()) {
                    currentTransaction.rollback();
                }
                doClose();
            } finally {
                closed = true;
                currentTransaction = null;
            }
        }
    }
    
    /**
     * Get the current transaction if any.
     * 
     * @return the current transaction or null
     */
    protected final Transaction getCurrentTransaction() {
        return currentTransaction;
    }
    
    /**
     * Check if a transaction is currently active.
     * 
     * @return true if a transaction is active
     */
    protected final boolean hasActiveTransaction() {
        return currentTransaction != null && currentTransaction.isActive();
    }
    
    // Template methods to be implemented by subclasses
    
    /**
     * Implementation-specific findById logic.
     */
    protected abstract <T, ID> Optional<T> doFindById(Class<T> entityClass, ID id);
    
    /**
     * Implementation-specific findAll logic.
     */
    protected abstract <T> List<T> doFindAll(Class<T> entityClass);
    
    /**
     * Implementation-specific save logic.
     */
    protected abstract <T> T doSave(T entity);
    
    /**
     * Implementation-specific delete logic.
     */
    protected abstract <T> boolean doDelete(T entity);
    
    /**
     * Implementation-specific createQuery logic.
     */
    protected abstract <T> Query<T> doCreateQuery(Class<T> entityClass);
    
    /**
     * Implementation-specific executeNativeQuery logic.
     */
    protected abstract <T> List<T> doExecuteNativeQuery(String query, Class<T> resultClass, Object... parameters);
    
    /**
     * Implementation-specific executeUpdate logic.
     */
    protected abstract int doExecuteUpdate(String query, Object... parameters);
    
    /**
     * Implementation-specific loadRelation logic.
     */
    protected abstract <T> void doLoadRelation(T entity, String relationName);
    
    /**
     * Implementation-specific beginTransaction logic.
     */
    protected abstract Transaction doBeginTransaction();
    
    /**
     * Implementation-specific flush logic.
     */
    protected abstract void doFlush();
    
    /**
     * Implementation-specific clear logic.
     */
    protected abstract void doClear();
    
    /**
     * Implementation-specific close logic.
     */
    protected abstract void doClose();
}
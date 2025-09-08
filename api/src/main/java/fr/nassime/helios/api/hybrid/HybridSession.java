package fr.nassime.helios.api.hybrid;

import fr.nassime.helios.api.HeliosSession;

import java.util.Optional;

/**
 * Extended session interface for hybrid entity operations.
 * Provides methods specific to managing entities across multiple storage types.
 */
public interface HybridSession extends HeliosSession {
    
    /**
     * Save a hybrid entity, distributing fields across appropriate storage types.
     */
    <T> T saveHybrid(T entity);
    
    /**
     * Find a hybrid entity by ID, loading from both storage types.
     */
    <T, ID> Optional<T> findHybridById(Class<T> entityClass, ID id);
    
    /**
     * Load document fields for a hybrid entity (lazy loading support).
     */
    <T> T loadDocumentFields(T entity);
    
    /**
     * Load relational fields for a hybrid entity (lazy loading support).
     */
    <T> T loadRelationalFields(T entity);
    
    /**
     * Synchronize data between storage types for a hybrid entity.
     */
    <T> void synchronize(T entity);
    
    /**
     * Delete a hybrid entity from all storage types.
     */
    <T> boolean deleteHybrid(T entity);
    
    /**
     * Force synchronization of all pending hybrid operations.
     */
    void flushHybrid();
}
package fr.nassime.helios.hybrid.synchronizer;

import fr.nassime.helios.api.HeliosSession;
import fr.nassime.helios.api.exception.HeliosException;
import fr.nassime.helios.api.hybrid.HybridEntityMetadata;
import fr.nassime.helios.hybrid.mapper.HybridEntityMapper;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;

/**
 * Handles synchronization between relational and document storage for hybrid entities.
 */
@Slf4j
public class HybridSynchronizer {
    
    private final HeliosSession relationalSession;
    private final HeliosSession documentSession;
    
    public HybridSynchronizer(HeliosSession relationalSession, HeliosSession documentSession) {
        this.relationalSession = relationalSession;
        this.documentSession = documentSession;
    }
    
    /**
     * Synchronize a hybrid entity across both storage types.
     */
    public <T> T synchronize(T entity) {
        if (!HybridEntityMapper.isHybridEntity(entity.getClass())) {
            throw new HeliosException("Entity " + entity.getClass().getSimpleName() + " is not a hybrid entity");
        }
        
        HybridEntityMetadata metadata = HybridEntityMapper.getMetadata(entity.getClass());
        
        return switch (metadata.getStrategy()) {
            case LINKED -> synchronizeLinked(entity, metadata);
            case EMBEDDED -> synchronizeEmbedded(entity, metadata);
            case UNIFIED -> synchronizeUnified(entity, metadata);
            case MANUAL -> entity; // No automatic sync
        };
    }
    
    /**
     * Asynchronously synchronize a hybrid entity.
     */
    public <T> CompletableFuture<T> synchronizeAsync(T entity) {
        return CompletableFuture.supplyAsync(() -> synchronize(entity));
    }
    
    /**
     * Save entity using LINKED strategy.
     * Relational data is saved first, then document data with relational ID reference.
     */
    private <T> T synchronizeLinked(T entity, HybridEntityMetadata metadata) {
        try {
            log.debug("Synchronizing {} using LINKED strategy", entity.getClass().getSimpleName());
            
            // 1. Save relational part first to get/ensure ID
            T savedEntity = saveRelationalPart(entity, metadata);
            Object entityId = getEntityId(savedEntity, metadata);
            
            if (entityId == null) {
                throw new HeliosException("Cannot synchronize hybrid entity without valid ID");
            }
            
            // 2. Save document part with relational ID reference  
            if (metadata.hasDocumentFields()) {
                saveDocumentPartWithReference(savedEntity, metadata, entityId);
            }
            
            log.debug("Successfully synchronized hybrid entity {} using LINKED strategy", 
                     entity.getClass().getSimpleName());
            return savedEntity;
            
        } catch (Exception e) {
            throw new HeliosException("Failed to synchronize hybrid entity using LINKED strategy", e);
        }
    }
    
    /**
     * Save entity using EMBEDDED strategy.
     * Both parts are saved with embedded references to each other.
     */
    private <T> T synchronizeEmbedded(T entity, HybridEntityMetadata metadata) {
        try {
            log.debug("Synchronizing {} using EMBEDDED strategy", entity.getClass().getSimpleName());
            
            // Save both parts with embedded IDs
            CompletableFuture<T> relationalFuture = null;
            CompletableFuture<T> documentFuture = null;
            
            if (metadata.hasRelationalFields()) {
                relationalFuture = CompletableFuture.supplyAsync(() -> 
                    saveRelationalPart(entity, metadata)
                );
            }
            
            if (metadata.hasDocumentFields()) {
                documentFuture = CompletableFuture.supplyAsync(() -> 
                    saveDocumentPart(entity, metadata)
                );
            }
            
            // Wait for completion
            T result = entity;
            if (relationalFuture != null && documentFuture != null) {
                CompletableFuture.allOf(relationalFuture, documentFuture).join();
                result = relationalFuture.get();
            } else if (relationalFuture != null) {
                result = relationalFuture.get();
            } else if (documentFuture != null) {
                result = documentFuture.get();
            }
            
            log.debug("Successfully synchronized hybrid entity {} using EMBEDDED strategy", 
                     entity.getClass().getSimpleName());
            return result;
            
        } catch (Exception e) {
            throw new HeliosException("Failed to synchronize hybrid entity using EMBEDDED strategy", e);
        }
    }
    
    /**
     * Save entity using UNIFIED strategy.
     * Both stores maintain synchronized copies with conflict resolution.
     */
    private <T> T synchronizeUnified(T entity, HybridEntityMetadata metadata) {
        try {
            log.debug("Synchronizing {} using UNIFIED strategy", entity.getClass().getSimpleName());
            
            // Save to both stores simultaneously
            CompletableFuture<T> relationalFuture = null;
            CompletableFuture<T> documentFuture = null;
            
            if (metadata.hasRelationalFields()) {
                relationalFuture = CompletableFuture.supplyAsync(() -> 
                    relationalSession.save(entity)
                );
            }
            
            if (metadata.hasDocumentFields()) {
                documentFuture = CompletableFuture.supplyAsync(() -> 
                    documentSession.save(entity)
                );
            }
            
            // Wait for both operations
            T result = entity;
            if (relationalFuture != null && documentFuture != null) {
                T relationalResult = relationalFuture.get();
                T documentResult = documentFuture.get();
                
                // Use relational result as primary (configurable in future)
                result = relationalResult;
            } else if (relationalFuture != null) {
                result = relationalFuture.get();
            } else if (documentFuture != null) {
                result = documentFuture.get();
            }
            
            log.debug("Successfully synchronized hybrid entity {} using UNIFIED strategy", 
                     entity.getClass().getSimpleName());
            return result;
            
        } catch (Exception e) {
            throw new HeliosException("Failed to synchronize hybrid entity using UNIFIED strategy", e);
        }
    }
    
    /**
     * Save only the relational parts of an entity.
     * In a complete implementation, this would create a filtered entity.
     */
    private <T> T saveRelationalPart(T entity, HybridEntityMetadata metadata) {
        if (!metadata.hasRelationalFields()) {
            return entity;
        }
        
        // For now, delegate to the relational session
        // TODO: Create filtered entity with only relational fields
        return relationalSession.save(entity);
    }
    
    /**
     * Save only the document parts of an entity.
     * In a complete implementation, this would create a filtered entity.
     */
    private <T> T saveDocumentPart(T entity, HybridEntityMetadata metadata) {
        if (!metadata.hasDocumentFields()) {
            return entity;
        }
        
        // For now, delegate to the document session
        // TODO: Create filtered entity with only document fields
        return documentSession.save(entity);
    }
    
    /**
     * Save document part with a reference to the relational entity ID.
     */
    private <T> void saveDocumentPartWithReference(T entity, HybridEntityMetadata metadata, Object relationalId) {
        // TODO: Enhance document with relational ID reference
        // This would modify the entity to include a field linking to the relational ID
        documentSession.save(entity);
    }
    
    /**
     * Extract entity ID using reflection.
     */
    private Object getEntityId(Object entity, HybridEntityMetadata metadata) {
        try {
            Field idField = metadata.getIdField();
            idField.setAccessible(true);
            return idField.get(entity);
        } catch (IllegalAccessException e) {
            throw new HeliosException("Failed to access entity ID field", e);
        }
    }
    
    /**
     * Load a hybrid entity by ID from both storage types.
     */
    public <T> T loadHybridEntity(Class<T> entityClass, Object id) {
        if (!HybridEntityMapper.isHybridEntity(entityClass)) {
            throw new HeliosException("Class " + entityClass.getSimpleName() + " is not a hybrid entity");
        }
        
        HybridEntityMetadata metadata = HybridEntityMapper.getMetadata(entityClass);
        
        // Load from primary storage first
        T entity = null;
        
        if (metadata.hasRelationalFields()) {
            entity = relationalSession.findById(entityClass, id).orElse(null);
        }
        
        if (entity == null && metadata.hasDocumentFields()) {
            entity = documentSession.findById(entityClass, id).orElse(null);
        }
        
        return entity;
    }
}
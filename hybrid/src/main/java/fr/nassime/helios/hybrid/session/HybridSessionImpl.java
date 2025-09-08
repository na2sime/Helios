package fr.nassime.helios.hybrid.session;

import fr.nassime.helios.api.HeliosSession;
import fr.nassime.helios.api.hybrid.HybridEntityMetadata;
import fr.nassime.helios.api.hybrid.HybridSession;
import fr.nassime.helios.api.query.Query;
import fr.nassime.helios.api.transaction.Transaction;
import fr.nassime.helios.hybrid.synchronizer.HybridSynchronizer;
import fr.nassime.helios.hybrid.mapper.HybridEntityMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Concrete implementation of HybridSession that coordinates between 
 * relational and document storage sessions.
 */
@Slf4j
public class HybridSessionImpl implements HybridSession {
    
    private final HeliosSession relationalSession;
    private final HeliosSession documentSession;
    private final HybridSynchronizer synchronizer;
    
    public HybridSessionImpl(HeliosSession relationalSession, HeliosSession documentSession) {
        this.relationalSession = relationalSession;
        this.documentSession = documentSession;
        this.synchronizer = new HybridSynchronizer(relationalSession, documentSession);
    }
    
    @Override
    public <T> T saveHybrid(T entity) {
        log.debug("Saving hybrid entity: {}", entity.getClass().getSimpleName());
        return synchronizer.synchronize(entity);
    }
    
    @Override
    public <T, ID> Optional<T> findHybridById(Class<T> entityClass, ID id) {
        log.debug("Finding hybrid entity {} by ID: {}", entityClass.getSimpleName(), id);
        T entity = synchronizer.loadHybridEntity(entityClass, id);
        return Optional.ofNullable(entity);
    }
    
    @Override
    public <T> T loadDocumentFields(T entity) {
        // TODO: Implement lazy loading of document fields
        log.debug("Loading document fields for: {}", entity.getClass().getSimpleName());
        return entity;
    }
    
    @Override
    public <T> T loadRelationalFields(T entity) {
        // TODO: Implement lazy loading of relational fields
        log.debug("Loading relational fields for: {}", entity.getClass().getSimpleName());
        return entity;
    }
    
    @Override
    public <T> void synchronize(T entity) {
        log.debug("Manually synchronizing hybrid entity: {}", entity.getClass().getSimpleName());
        synchronizer.synchronize(entity);
    }
    
    @Override
    public <T> boolean deleteHybrid(T entity) {
        if (!HybridEntityMapper.isHybridEntity(entity.getClass())) {
            return delete(entity);
        }
        
        log.debug("Deleting hybrid entity: {}", entity.getClass().getSimpleName());
        HybridEntityMetadata metadata = HybridEntityMapper.getMetadata(entity.getClass());
        
        boolean relationalDeleted = true;
        boolean documentDeleted = true;
        
        if (metadata.hasRelationalFields()) {
            relationalDeleted = relationalSession.delete(entity);
        }
        
        if (metadata.hasDocumentFields()) {
            documentDeleted = documentSession.delete(entity);
        }
        
        return relationalDeleted && documentDeleted;
    }
    
    @Override
    public void flushHybrid() {
        log.debug("Flushing hybrid session operations");
        relationalSession.flush();
        documentSession.flush();
    }
    
    // Delegate standard HeliosSession methods to appropriate storage
    
    @Override
    public <T, ID> Optional<T> findById(Class<T> entityClass, ID id) {
        if (HybridEntityMapper.isHybridEntity(entityClass)) {
            return findHybridById(entityClass, id);
        }
        
        // Determine which session to use based on entity annotations
        // Default to relational for non-hybrid entities
        return relationalSession.findById(entityClass, id);
    }
    
    @Override
    public <T> List<T> findAll(Class<T> entityClass) {
        if (HybridEntityMapper.isHybridEntity(entityClass)) {
            // For hybrid entities, this is complex - need to merge results
            // For now, delegate to relational session
            return relationalSession.findAll(entityClass);
        }
        
        return relationalSession.findAll(entityClass);
    }
    
    @Override
    public <T> T save(T entity) {
        if (HybridEntityMapper.isHybridEntity(entity.getClass())) {
            return saveHybrid(entity);
        }
        
        // Default to relational session for non-hybrid entities
        return relationalSession.save(entity);
    }
    
    @Override
    public <T> boolean delete(T entity) {
        if (HybridEntityMapper.isHybridEntity(entity.getClass())) {
            return deleteHybrid(entity);
        }
        
        return relationalSession.delete(entity);
    }
    
    @Override
    public <T> Query<T> createQuery(Class<T> entityClass) {
        // For hybrid entities, this would need special handling
        // For now, default to relational session
        return relationalSession.createQuery(entityClass);
    }
    
    @Override
    public <T> List<T> executeNativeQuery(String query, Class<T> resultClass, Object... parameters) {
        // Native queries would need provider-specific handling
        return relationalSession.executeNativeQuery(query, resultClass, parameters);
    }
    
    @Override
    public int executeUpdate(String query, Object... parameters) {
        return relationalSession.executeUpdate(query, parameters);
    }
    
    @Override
    public <T> void loadRelation(T entity, String relationName) {
        // Delegate to appropriate session based on field storage type
        relationalSession.loadRelation(entity, relationName);
    }
    
    @Override
    public Transaction beginTransaction() {
        // For hybrid entities, we'd need distributed transactions
        // For now, use relational transaction
        return relationalSession.beginTransaction();
    }
    
    @Override
    public <T> T executeInTransaction(Function<HeliosSession, T> operation) {
        return relationalSession.executeInTransaction(operation);
    }
    
    @Override
    public void executeInTransaction(Consumer<HeliosSession> operation) {
        relationalSession.executeInTransaction(operation);
    }
    
    @Override
    public void flush() {
        flushHybrid();
    }
    
    @Override
    public void clear() {
        relationalSession.clear();
        documentSession.clear();
    }
    
    @Override
    public void close() {
        try {
            relationalSession.close();
        } catch (Exception e) {
            log.warn("Error closing relational session", e);
        }
        
        try {
            documentSession.close();
        } catch (Exception e) {
            log.warn("Error closing document session", e);
        }
    }
}
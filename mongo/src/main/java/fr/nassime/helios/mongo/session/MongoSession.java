package fr.nassime.helios.mongo.session;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import fr.nassime.helios.api.HeliosSession;
import fr.nassime.helios.api.exception.HeliosException;
import fr.nassime.helios.api.query.Query;
import fr.nassime.helios.api.transaction.Transaction;
import fr.nassime.helios.mongo.mapping.DocumentMapper;
import fr.nassime.helios.mongo.query.MongoQuery;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * MongoDB implementation of HeliosSession.
 * Provides CRUD operations and query capabilities for MongoDB documents.
 */
@Slf4j
public class MongoSession implements HeliosSession {
    
    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final DocumentMapper documentMapper;
    
    public MongoSession(MongoClient mongoClient, String databaseName) {
        this.mongoClient = mongoClient;
        this.database = mongoClient.getDatabase(databaseName);
        this.documentMapper = new DocumentMapper();
        log.debug("MongoDB session created for database: {}", databaseName);
    }
    
    @Override
    public <T> T save(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        
        log.debug("Saving entity: {}", entity.getClass().getSimpleName());
        
        try {
            String collectionName = documentMapper.getCollectionName(entity.getClass());
            MongoCollection<Document> collection = database.getCollection(collectionName);
            
            Document document = documentMapper.toDocument(entity);
            
            // Check if entity has ID (update vs insert)
            Object id = documentMapper.getId(entity);
            if (id != null && documentMapper.exists(collection, id)) {
                // Update existing document
                collection.replaceOne(new Document("_id", id), document);
                log.debug("Updated existing document with ID: {}", id);
            } else {
                // Insert new document
                collection.insertOne(document);
                // Set generated ID back to entity if it was generated
                Object generatedId = document.get("_id");
                if (generatedId != null) {
                    documentMapper.setId(entity, generatedId);
                }
                log.debug("Inserted new document with ID: {}", generatedId);
            }
            
            return entity;
            
        } catch (Exception e) {
            log.error("Failed to save entity: {}", entity.getClass().getSimpleName(), e);
            throw new HeliosException("Failed to save entity: " + e.getMessage(), e);
        }
    }
    
    public <T> List<T> saveAll(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return entities;
        }
        
        log.debug("Saving {} entities", entities.size());
        
        // For now, save one by one. Could be optimized with bulk operations later
        for (T entity : entities) {
            save(entity);
        }
        
        return entities;
    }
    
    @Override
    public <T, ID> Optional<T> findById(Class<T> entityClass, ID id) {
        if (entityClass == null || id == null) {
            throw new IllegalArgumentException("EntityClass and ID cannot be null");
        }
        
        log.debug("Finding entity by ID: {} for class: {}", id, entityClass.getSimpleName());
        
        try {
            String collectionName = documentMapper.getCollectionName(entityClass);
            MongoCollection<Document> collection = database.getCollection(collectionName);
            
            Document document = collection.find(new Document("_id", id)).first();
            if (document == null) {
                log.debug("No document found with ID: {}", id);
                return Optional.empty();
            }
            
            T entity = documentMapper.fromDocument(document, entityClass);
            log.debug("Found entity with ID: {}", id);
            return Optional.of(entity);
            
        } catch (Exception e) {
            log.error("Failed to find entity by ID: {} for class: {}", id, entityClass.getSimpleName(), e);
            throw new HeliosException("Failed to find entity by ID: " + e.getMessage(), e);
        }
    }
    
    @Override
    public <T> List<T> findAll(Class<T> entityClass) {
        if (entityClass == null) {
            throw new IllegalArgumentException("EntityClass cannot be null");
        }
        
        log.debug("Finding all entities for class: {}", entityClass.getSimpleName());
        
        try {
            String collectionName = documentMapper.getCollectionName(entityClass);
            MongoCollection<Document> collection = database.getCollection(collectionName);
            
            List<T> entities = collection.find()
                .map(document -> documentMapper.fromDocument(document, entityClass))
                .into(new java.util.ArrayList<>());
            
            log.debug("Found {} entities for class: {}", entities.size(), entityClass.getSimpleName());
            return entities;
            
        } catch (Exception e) {
            log.error("Failed to find all entities for class: {}", entityClass.getSimpleName(), e);
            throw new HeliosException("Failed to find all entities: " + e.getMessage(), e);
        }
    }
    
    @Override
    public <T> boolean delete(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        
        Object id = documentMapper.getId(entity);
        if (id == null) {
            throw new IllegalArgumentException("Entity must have an ID to be deleted");
        }
        
        return deleteById(entity.getClass(), id);
    }
    
    public <T> boolean deleteById(Class<T> entityClass, Object id) {
        if (entityClass == null || id == null) {
            throw new IllegalArgumentException("EntityClass and ID cannot be null");
        }
        
        log.debug("Deleting entity by ID: {} for class: {}", id, entityClass.getSimpleName());
        
        try {
            String collectionName = documentMapper.getCollectionName(entityClass);
            MongoCollection<Document> collection = database.getCollection(collectionName);
            
            long deletedCount = collection.deleteOne(new Document("_id", id)).getDeletedCount();
            
            if (deletedCount == 0) {
                log.warn("No document found to delete with ID: {}", id);
                return false;
            } else {
                log.debug("Deleted entity with ID: {}", id);
                return true;
            }
            
        } catch (Exception e) {
            log.error("Failed to delete entity by ID: {} for class: {}", id, entityClass.getSimpleName(), e);
            throw new HeliosException("Failed to delete entity by ID: " + e.getMessage(), e);
        }
    }
    
    public <T> void deleteAll(Class<T> entityClass) {
        if (entityClass == null) {
            throw new IllegalArgumentException("EntityClass cannot be null");
        }
        
        log.debug("Deleting all entities for class: {}", entityClass.getSimpleName());
        
        try {
            String collectionName = documentMapper.getCollectionName(entityClass);
            MongoCollection<Document> collection = database.getCollection(collectionName);
            
            long deletedCount = collection.deleteMany(new Document()).getDeletedCount();
            log.debug("Deleted {} entities for class: {}", deletedCount, entityClass.getSimpleName());
            
        } catch (Exception e) {
            log.error("Failed to delete all entities for class: {}", entityClass.getSimpleName(), e);
            throw new HeliosException("Failed to delete all entities: " + e.getMessage(), e);
        }
    }
    
    @Override
    public <T> Query<T> createQuery(Class<T> entityClass) {
        if (entityClass == null) {
            throw new IllegalArgumentException("EntityClass cannot be null");
        }
        
        log.debug("Creating query for class: {}", entityClass.getSimpleName());
        return new MongoQuery<>(entityClass, this, database);
    }
    
    @Override
    public <T> List<T> executeNativeQuery(String query, Class<T> resultClass, Object... parameters) {
        // MongoDB doesn't use SQL, but we could support MongoDB queries in JSON format
        throw new UnsupportedOperationException("Native SQL queries are not supported in MongoDB. Use createQuery() instead.");
    }
    
    @Override
    public int executeUpdate(String query, Object... parameters) {
        // MongoDB doesn't use SQL, but we could support MongoDB update operations
        throw new UnsupportedOperationException("SQL update queries are not supported in MongoDB. Use save() or createQuery() instead.");
    }
    
    @Override
    public <T> void loadRelation(T entity, String relationName) {
        // MongoDB relations would need to be implemented differently
        // For now, throw unsupported operation
        throw new UnsupportedOperationException("Relation loading is not yet implemented for MongoDB");
    }
    
    @Override
    public Transaction beginTransaction() {
        // MongoDB transactions would need to be implemented
        throw new UnsupportedOperationException("Transactions are not yet implemented for MongoDB");
    }
    
    @Override
    public <T> T executeInTransaction(Function<HeliosSession, T> operation) {
        // For now, just execute without transaction support
        return operation.apply(this);
    }
    
    @Override
    public void executeInTransaction(Consumer<HeliosSession> operation) {
        // For now, just execute without transaction support
        operation.accept(this);
    }
    
    @Override
    public void flush() {
        // MongoDB operations are immediately flushed, no-op
    }
    
    @Override
    public void clear() {
        // MongoDB doesn't have a session cache, no-op
    }
    
    @Override
    public void close() {
        log.debug("Closing MongoDB session");
        // MongoDB sessions are lightweight, no special cleanup needed
        // The client will be closed by the session factory
    }
    
    /**
     * Get the MongoDB database for advanced operations.
     */
    public MongoDatabase getDatabase() {
        return database;
    }
    
    /**
     * Get the document mapper for advanced operations.
     */
    public DocumentMapper getDocumentMapper() {
        return documentMapper;
    }
}
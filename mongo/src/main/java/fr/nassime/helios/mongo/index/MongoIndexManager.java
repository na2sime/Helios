package fr.nassime.helios.mongo.index;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import fr.nassime.helios.api.annotations.Document;
import fr.nassime.helios.api.annotations.Index;
import fr.nassime.helios.api.annotations.Indexes;
import fr.nassime.helios.api.exception.HeliosException;
import fr.nassime.helios.mongo.mapping.DocumentMapper;
import lombok.extern.slf4j.Slf4j;
import org.bson.conversions.Bson;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.Indexes.*;

/**
 * Manages automatic index creation for MongoDB collections based on entity annotations.
 * Creates indexes when collections are first accessed or during application startup.
 */
@Slf4j
public class MongoIndexManager {
    
    private final MongoDatabase database;
    private final DocumentMapper documentMapper;
    
    public MongoIndexManager(MongoDatabase database, DocumentMapper documentMapper) {
        this.database = database;
        this.documentMapper = documentMapper;
    }
    
    /**
     * Ensure all indexes for the given entity class are created in MongoDB.
     * This method is idempotent - calling it multiple times is safe.
     */
    public <T> void ensureIndexes(Class<T> entityClass) {
        if (!entityClass.isAnnotationPresent(Document.class)) {
            log.debug("Class {} is not a document entity, skipping index creation", entityClass.getSimpleName());
            return;
        }
        
        try {
            String collectionName = documentMapper.getCollectionName(entityClass);
            MongoCollection<org.bson.Document> collection = database.getCollection(collectionName);
            
            log.debug("Ensuring indexes for collection: {}", collectionName);
            
            // Create field-level indexes
            createFieldIndexes(entityClass, collection);
            
            // Create class-level compound indexes
            createClassIndexes(entityClass, collection);
            
            log.debug("Index creation completed for collection: {}", collectionName);
            
        } catch (Exception e) {
            log.error("Failed to create indexes for class: {}", entityClass.getSimpleName(), e);
            throw new HeliosException("Failed to create indexes: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create indexes defined on individual fields.
     */
    private <T> void createFieldIndexes(Class<T> entityClass, MongoCollection<org.bson.Document> collection) {
        Field[] fields = entityClass.getDeclaredFields();
        
        for (Field field : fields) {
            Index indexAnnotation = field.getAnnotation(Index.class);
            if (indexAnnotation == null) {
                continue;
            }
            
            String fieldName = documentMapper.getFieldName(field);
            createSingleFieldIndex(collection, fieldName, indexAnnotation);
        }
    }
    
    /**
     * Create compound indexes defined on the class level.
     */
    private <T> void createClassIndexes(Class<T> entityClass, MongoCollection<org.bson.Document> collection) {
        // Handle single @Index annotation
        Index indexAnnotation = entityClass.getAnnotation(Index.class);
        if (indexAnnotation != null) {
            createCompoundIndex(collection, indexAnnotation);
        }
        
        // Handle multiple @Index annotations via @Indexes
        Indexes indexesAnnotation = entityClass.getAnnotation(Indexes.class);
        if (indexesAnnotation != null) {
            for (Index index : indexesAnnotation.value()) {
                createCompoundIndex(collection, index);
            }
        }
    }
    
    /**
     * Create a single field index.
     */
    private void createSingleFieldIndex(MongoCollection<org.bson.Document> collection, String fieldName, Index indexAnnotation) {
        try {
            // Determine index direction
            int direction = indexAnnotation.directions().length > 0 ? indexAnnotation.directions()[0] : 1;
            
            Bson indexBson = direction == 1 ? ascending(fieldName) : descending(fieldName);
            
            // Build index options
            IndexOptions options = buildIndexOptions(indexAnnotation);
            
            // Set index name if specified
            String indexName = indexAnnotation.name().isEmpty() ? 
                fieldName + "_" + (direction == 1 ? "1" : "-1") : indexAnnotation.name();
            options.name(indexName);
            
            // Create the index
            collection.createIndex(indexBson, options);
            log.debug("Created single field index '{}' on field '{}' with direction {}", 
                indexName, fieldName, direction);
                
        } catch (Exception e) {
            log.error("Failed to create index on field: {}", fieldName, e);
            throw new HeliosException("Failed to create index on field " + fieldName + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Create a compound index.
     */
    private void createCompoundIndex(MongoCollection<org.bson.Document> collection, Index indexAnnotation) {
        if (indexAnnotation.fields().length == 0) {
            log.warn("Compound index annotation has no fields specified, skipping");
            return;
        }
        
        try {
            // Build index options first
            IndexOptions options = buildIndexOptions(indexAnnotation);
            
            // Set index name
            String indexName = indexAnnotation.name().isEmpty() ? 
                generateCompoundIndexName(indexAnnotation.fields(), indexAnnotation.directions()) : indexAnnotation.name();
            options.name(indexName);
            
            List<Bson> indexFields = new ArrayList<>();
            int[] directions = indexAnnotation.directions();
            
            // Build compound index fields
            for (int i = 0; i < indexAnnotation.fields().length; i++) {
                String fieldName = indexAnnotation.fields()[i];
                int direction = i < directions.length ? directions[i] : 1;
                
                Bson indexField = direction == 1 ? ascending(fieldName) : descending(fieldName);
                indexFields.add(indexField);
            }
            
            // Pour l'instant, on créé des index séparés au lieu d'un compound index
            // TODO: Implémenter compound index correctement avec MongoDB driver
            for (Bson indexField : indexFields) {
                collection.createIndex(indexField, options);
            }
            log.debug("Created separate indexes for compound index '{}' on fields: {}", indexName, String.join(", ", indexAnnotation.fields()));
            
        } catch (Exception e) {
            log.error("Failed to create compound index on fields: {}", String.join(", ", indexAnnotation.fields()), e);
            throw new HeliosException("Failed to create compound index: " + e.getMessage(), e);
        }
    }
    
    /**
     * Build index options from annotation.
     */
    private IndexOptions buildIndexOptions(Index indexAnnotation) {
        IndexOptions options = new IndexOptions();
        
        if (indexAnnotation.unique()) {
            options.unique(true);
        }
        
        if (indexAnnotation.sparse()) {
            options.sparse(true);
        }
        
        if (indexAnnotation.background()) {
            options.background(true);
        }
        
        return options;
    }
    
    /**
     * Generate a default name for compound indexes.
     */
    private String generateCompoundIndexName(String[] fields, int[] directions) {
        StringBuilder nameBuilder = new StringBuilder();
        
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                nameBuilder.append("_");
            }
            nameBuilder.append(fields[i]);
            
            int direction = i < directions.length ? directions[i] : 1;
            nameBuilder.append("_").append(direction == 1 ? "1" : "-1");
        }
        
        return nameBuilder.toString();
    }
    
    /**
     * Drop all indexes for a collection (except _id).
     * Useful for development/testing.
     */
    public <T> void dropAllIndexes(Class<T> entityClass) {
        try {
            String collectionName = documentMapper.getCollectionName(entityClass);
            MongoCollection<org.bson.Document> collection = database.getCollection(collectionName);
            
            collection.dropIndexes();
            log.debug("Dropped all indexes for collection: {}", collectionName);
            
        } catch (Exception e) {
            log.error("Failed to drop indexes for class: {}", entityClass.getSimpleName(), e);
            throw new HeliosException("Failed to drop indexes: " + e.getMessage(), e);
        }
    }
    
    /**
     * List all indexes for a collection.
     */
    public <T> void listIndexes(Class<T> entityClass) {
        try {
            String collectionName = documentMapper.getCollectionName(entityClass);
            MongoCollection<org.bson.Document> collection = database.getCollection(collectionName);
            
            log.info("Indexes for collection '{}': ", collectionName);
            for (org.bson.Document index : collection.listIndexes()) {
                log.info("  - {}", index.toJson());
            }
            
        } catch (Exception e) {
            log.error("Failed to list indexes for class: {}", entityClass.getSimpleName(), e);
        }
    }
}
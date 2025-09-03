package fr.nassime.helios.mongo.relations;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import fr.nassime.helios.api.annotations.*;
import fr.nassime.helios.api.annotations.enums.FetchType;
import fr.nassime.helios.api.exception.HeliosException;
import fr.nassime.helios.mongo.mapping.DocumentMapper;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Handles loading of relationships for MongoDB entities.
 * Supports @OneToMany, @ManyToOne, and @ManyToMany relationships.
 */
@Slf4j
public class MongoRelationLoader {
    
    private final MongoDatabase database;
    private final DocumentMapper documentMapper;
    
    public MongoRelationLoader(MongoDatabase database, DocumentMapper documentMapper) {
        this.database = database;
        this.documentMapper = documentMapper;
    }
    
    /**
     * Load a specific relation for an entity.
     */
    public <T> void loadRelation(T entity, String relationName) {
        if (entity == null || relationName == null || relationName.isEmpty()) {
            throw new IllegalArgumentException("Entity and relation name cannot be null or empty");
        }
        
        try {
            Field relationField = findRelationField(entity.getClass(), relationName);
            if (relationField == null) {
                throw new IllegalArgumentException("Relation field '" + relationName + "' not found in class " + entity.getClass().getName());
            }
            
            loadRelationField(entity, relationField);
            
        } catch (Exception e) {
            log.error("Failed to load relation '{}' for entity: {}", relationName, entity.getClass().getSimpleName(), e);
            throw new HeliosException("Failed to load relation: " + e.getMessage(), e);
        }
    }
    
    /**
     * Load all EAGER relationships for an entity.
     */
    public <T> void loadEagerRelations(T entity) {
        if (entity == null) {
            return;
        }
        
        try {
            Field[] fields = entity.getClass().getDeclaredFields();
            
            for (Field field : fields) {
                if (isRelationField(field) && isEagerFetch(field)) {
                    loadRelationField(entity, field);
                }
            }
            
        } catch (Exception e) {
            log.error("Failed to load eager relations for entity: {}", entity.getClass().getSimpleName(), e);
            // Don't throw exception for eager loading failures
        }
    }
    
    /**
     * Load a specific relation field.
     */
    private <T> void loadRelationField(T entity, Field field) throws Exception {
        field.setAccessible(true);
        
        if (field.isAnnotationPresent(OneToMany.class)) {
            loadOneToMany(entity, field);
        } else if (field.isAnnotationPresent(ManyToOne.class)) {
            loadManyToOne(entity, field);
        } else if (field.isAnnotationPresent(ManyToMany.class)) {
            loadManyToMany(entity, field);
        } else if (field.isAnnotationPresent(OneToOne.class)) {
            loadOneToOne(entity, field);
        }
    }
    
    /**
     * Load OneToMany relationship (array of references in MongoDB).
     */
    private <T> void loadOneToMany(T entity, Field field) throws Exception {
        OneToMany annotation = field.getAnnotation(OneToMany.class);
        
        // Get the target entity class
        Class<?> targetClass = getTargetClass(field, annotation.targetEntity());
        
        // Get the parent entity's ID
        Object parentId = documentMapper.getId(entity);
        if (parentId == null) {
            log.warn("Cannot load OneToMany relation for entity without ID");
            return;
        }
        // Find the foreign key field name
        String foreignKeyField = getForeignKeyField(annotation.mappedBy(), entity.getClass());
        
        // Convert ObjectId to String for foreign key comparison
        Object foreignKeyValue = (parentId instanceof org.bson.types.ObjectId) ? parentId.toString() : parentId;
        
        // Query for related entities
        String collectionName = documentMapper.getCollectionName(targetClass);
        MongoCollection<Document> collection = database.getCollection(collectionName);
        
        log.debug("OneToMany query: collection={}, field={}, value={}", collectionName, foreignKeyField, foreignKeyValue);
        List<Document> documents = collection.find(new Document(foreignKeyField, foreignKeyValue))
                .into(new ArrayList<>());
        log.debug("OneToMany query result: {} documents found", documents.size());
        
        // Convert documents to entities
        List<Object> relatedEntities = new ArrayList<>();
        for (Document doc : documents) {
            Object relatedEntity = documentMapper.fromDocument(doc, targetClass);
            relatedEntities.add(relatedEntity);
        }
        
        // Set the collection on the parent entity
        if (Collection.class.isAssignableFrom(field.getType())) {
            field.set(entity, relatedEntities);
        }
        
        log.debug("Loaded {} entities for OneToMany relation '{}'", relatedEntities.size(), field.getName());
    }
    
    /**
     * Load ManyToOne relationship (single reference in MongoDB).
     */
    private <T> void loadManyToOne(T entity, Field field) throws Exception {
        ManyToOne annotation = field.getAnnotation(ManyToOne.class);
        
        // Get the target entity class
        Class<?> targetClass = getTargetClass(field, annotation.targetEntity());
        
        // Get the foreign key value from the current entity
        // Try multiple naming conventions for foreign key field
        String foreignKeyFieldName = field.getName() + "_id"; // Convention: field_name + "_id"
        Object foreignKeyValue = getForeignKeyValue(entity, foreignKeyFieldName);
        
        if (foreignKeyValue == null) {
            // Try camelCase version: department -> departmentId
            foreignKeyFieldName = field.getName() + "Id";
            foreignKeyValue = getForeignKeyValue(entity, foreignKeyFieldName);
        }
        
        if (foreignKeyValue == null) {
            if (!annotation.optional()) {
                log.warn("Required ManyToOne relation '{}' has null foreign key", field.getName());
            }
            return;
        }
        
        // Find the related entity
        String collectionName = documentMapper.getCollectionName(targetClass);
        MongoCollection<Document> collection = database.getCollection(collectionName);
        
        // Convert foreign key to proper format for MongoDB query
        Object queryId = convertToQueryId(foreignKeyValue);
        Document document = collection.find(new Document("_id", queryId)).first();
        
        if (document != null) {
            Object relatedEntity = documentMapper.fromDocument(document, targetClass);
            field.set(entity, relatedEntity);
            log.debug("Loaded ManyToOne relation '{}' with ID: {}", field.getName(), foreignKeyValue);
        } else if (!annotation.optional()) {
            log.warn("Required ManyToOne relation '{}' not found with ID: {}", field.getName(), foreignKeyValue);
        }
    }
    
    /**
     * Load ManyToMany relationship (array of references).
     */
    private <T> void loadManyToMany(T entity, Field field) throws Exception {
        ManyToMany annotation = field.getAnnotation(ManyToMany.class);
        
        // Get the target entity class
        Class<?> targetClass = getTargetClass(field, annotation.targetEntity());
        
        // Get array of foreign key values  
        // Convention: for field "projects" -> look for "projectIds" (singular + "Ids")
        String fieldName = field.getName();
        String foreignKeyArrayField;
        if (fieldName.endsWith("s")) {
            // Remove trailing 's' and add "Ids": "projects" -> "projectIds" 
            foreignKeyArrayField = fieldName.substring(0, fieldName.length() - 1) + "Ids";
        } else {
            // If field is not plural, just add "Ids": "project" -> "projectIds"
            foreignKeyArrayField = fieldName + "Ids";
        }
        List<Object> foreignKeyValues = getForeignKeyArrayValue(entity, foreignKeyArrayField);
        
        log.debug("ManyToMany field: {}, expected field: {}, values: {}", field.getName(), foreignKeyArrayField, foreignKeyValues);
        
        if (foreignKeyValues == null || foreignKeyValues.isEmpty()) {
            field.set(entity, new ArrayList<>());
            return;
        }
        
        // Convert foreign keys to proper format for MongoDB query
        List<Object> queryIds = new ArrayList<>();
        for (Object fk : foreignKeyValues) {
            // For ManyToMany, IDs stored in arrays are typically strings but we need to convert them to ObjectId for query
            queryIds.add(convertToQueryId(fk));
        }
        
        // Find related entities
        String collectionName = documentMapper.getCollectionName(targetClass);
        MongoCollection<Document> collection = database.getCollection(collectionName);
        
        List<Document> documents = collection.find(new Document("_id", new Document("$in", queryIds)))
                .into(new ArrayList<>());
        
        // Convert documents to entities
        List<Object> relatedEntities = new ArrayList<>();
        for (Document doc : documents) {
            Object relatedEntity = documentMapper.fromDocument(doc, targetClass);
            relatedEntities.add(relatedEntity);
        }
        
        field.set(entity, relatedEntities);
        log.debug("Loaded {} entities for ManyToMany relation '{}'", relatedEntities.size(), field.getName());
    }
    
    /**
     * Load OneToOne relationship.
     */
    private <T> void loadOneToOne(T entity, Field field) throws Exception {
        // OneToOne is similar to ManyToOne for MongoDB
        // Implementation would be similar to loadManyToOne
        log.debug("OneToOne relation loading not yet fully implemented for field: {}", field.getName());
    }
    
    // Helper methods
    
    private Field findRelationField(Class<?> entityClass, String relationName) {
        try {
            return entityClass.getDeclaredField(relationName);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }
    
    private boolean isRelationField(Field field) {
        return field.isAnnotationPresent(OneToMany.class) ||
               field.isAnnotationPresent(ManyToOne.class) ||
               field.isAnnotationPresent(ManyToMany.class) ||
               field.isAnnotationPresent(OneToOne.class);
    }
    
    private boolean isEagerFetch(Field field) {
        if (field.isAnnotationPresent(OneToMany.class)) {
            return field.getAnnotation(OneToMany.class).fetch() == FetchType.EAGER;
        } else if (field.isAnnotationPresent(ManyToOne.class)) {
            return field.getAnnotation(ManyToOne.class).fetch() == FetchType.EAGER;
        } else if (field.isAnnotationPresent(ManyToMany.class)) {
            return field.getAnnotation(ManyToMany.class).fetch() == FetchType.EAGER;
        }
        return false;
    }
    
    private Class<?> getTargetClass(Field field, Class<?> targetEntity) {
        if (targetEntity != void.class) {
            return targetEntity;
        }
        
        // Infer from generic type
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) genericType;
            Type[] typeArgs = paramType.getActualTypeArguments();
            if (typeArgs.length > 0) {
                return (Class<?>) typeArgs[0];
            }
        }
        
        return field.getType();
    }
    
    private String getForeignKeyField(String mappedBy, Class<?> parentClass) {
        if (!mappedBy.isEmpty()) {
            // Check both naming conventions for mapped field
            return mappedBy + "_id"; // Use underscore convention for MongoDB field names
        }
        // Default convention: parent class name in lowercase + "_id"
        return parentClass.getSimpleName().toLowerCase() + "_id";
    }
    
    private Object getForeignKeyValue(Object entity, String fieldName) {
        try {
            Field field = entity.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(entity);
        } catch (Exception e) {
            log.debug("Foreign key field '{}' not found or not accessible", fieldName);
            // Try with underscore convention if camelCase failed
            if (!fieldName.contains("_")) {
                String underscoreFieldName = convertToUnderscore(fieldName);
                try {
                    Field underscoreField = entity.getClass().getDeclaredField(underscoreFieldName);
                    underscoreField.setAccessible(true);
                    return underscoreField.get(entity);
                } catch (Exception ex) {
                    log.debug("Foreign key field with underscore '{}' also not found", underscoreFieldName);
                }
            }
            return null;
        }
    }
    
    @SuppressWarnings("unchecked")
    private List<Object> getForeignKeyArrayValue(Object entity, String fieldName) {
        try {
            Field field = entity.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(entity);
            if (value instanceof List) {
                return (List<Object>) value;
            }
        } catch (Exception e) {
            log.debug("Foreign key array field '{}' not found or not accessible", fieldName);
            // Try with underscore convention if camelCase failed
            if (!fieldName.contains("_")) {
                String underscoreFieldName = convertToUnderscore(fieldName);
                try {
                    Field underscoreField = entity.getClass().getDeclaredField(underscoreFieldName);
                    underscoreField.setAccessible(true);
                    Object value = underscoreField.get(entity);
                    if (value instanceof List) {
                        return (List<Object>) value;
                    }
                } catch (Exception ex) {
                    log.debug("Foreign key array field with underscore '{}' also not found", underscoreFieldName);
                }
            }
        }
        return new ArrayList<>();
    }
    
    private Object convertToQueryId(Object id) {
        if (id instanceof String && !((String) id).isEmpty()) {
            String stringId = (String) id;
            // Check if it's a valid 24-character hex ObjectId
            if (stringId.length() == 24 && stringId.matches("[a-fA-F0-9]+")) {
                try {
                    return new org.bson.types.ObjectId(stringId);
                } catch (IllegalArgumentException e) {
                    return id;
                }
            }
        }
        return id;
    }
    
    private String convertToUnderscore(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
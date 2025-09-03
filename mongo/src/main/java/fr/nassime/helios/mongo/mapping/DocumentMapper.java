package fr.nassime.helios.mongo.mapping;

import com.mongodb.client.MongoCollection;
import fr.nassime.helios.api.annotations.Document;
import fr.nassime.helios.api.annotations.Field;
import fr.nassime.helios.api.annotations.Id;
import fr.nassime.helios.api.exception.HeliosException;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps Java entities to MongoDB BSON documents and vice versa.
 * Handles field mapping, ID generation, and collection name resolution.
 */
@Slf4j
public class DocumentMapper {
    
    private final Map<Class<?>, DocumentMetadata> metadataCache = new ConcurrentHashMap<>();
    
    /**
     * Convert entity to BSON document.
     */
    public org.bson.Document toDocument(Object entity) {
        if (entity == null) {
            return null;
        }
        
        DocumentMetadata metadata = getMetadata(entity.getClass());
        org.bson.Document document = new org.bson.Document();
        
        for (FieldMapping field : metadata.getFields()) {
            try {
                java.lang.reflect.Field javaField = field.getJavaField();
                javaField.setAccessible(true);
                Object value = javaField.get(entity);
                
                if (value != null) {
                    String documentFieldName = field.getDocumentFieldName();
                    
                    // Handle ID field specially
                    if (field.isId()) {
                        // Convert String to ObjectId if it's a valid ObjectId hex string
                        if (value instanceof String && !((String) value).isEmpty()) {
                            String stringValue = (String) value;
                            // Check if it's a valid 24-character hex ObjectId
                            if (stringValue.length() == 24 && stringValue.matches("[a-fA-F0-9]+")) {
                                try {
                                    value = new ObjectId(stringValue);
                                } catch (IllegalArgumentException e) {
                                    log.debug("Failed to convert string to ObjectId in toDocument: {}", stringValue);
                                    // Keep as String if conversion fails
                                }
                            }
                        }
                        document.put("_id", value);
                    } else {
                        document.put(documentFieldName, convertToMongoValue(value));
                    }
                }
                
            } catch (IllegalAccessException e) {
                throw new HeliosException("Cannot access field: " + field.getJavaField().getName(), e);
            }
        }
        
        return document;
    }
    
    /**
     * Convert BSON document to entity.
     */
    public <T> T fromDocument(org.bson.Document document, Class<T> entityClass) {
        if (document == null) {
            return null;
        }
        
        DocumentMetadata metadata = getMetadata(entityClass);
        
        try {
            // Create instance using no-arg constructor
            Constructor<T> constructor = entityClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            T entity = constructor.newInstance();
            
            for (FieldMapping field : metadata.getFields()) {
                java.lang.reflect.Field javaField = field.getJavaField();
                javaField.setAccessible(true);
                
                Object value;
                if (field.isId()) {
                    value = document.get("_id");
                    // Convert ObjectId to String if the field is String type
                    if (value instanceof ObjectId && javaField.getType() == String.class) {
                        value = ((ObjectId) value).toHexString();
                    }
                } else {
                    String documentFieldName = field.getDocumentFieldName();
                    value = document.get(documentFieldName);
                }
                
                if (value != null) {
                    value = convertFromMongoValue(value, javaField.getType());
                    javaField.set(entity, value);
                }
            }
            
            return entity;
            
        } catch (NoSuchMethodException e) {
            throw new HeliosException("Entity class must have a no-argument constructor: " + entityClass.getName(), e);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new HeliosException("Cannot create instance of: " + entityClass.getName(), e);
        }
    }
    
    /**
     * Get collection name for entity class.
     */
    public String getCollectionName(Class<?> entityClass) {
        DocumentMetadata metadata = getMetadata(entityClass);
        return metadata.getCollectionName();
    }
    
    /**
     * Get ID value from entity.
     */
    public Object getId(Object entity) {
        if (entity == null) {
            return null;
        }
        
        DocumentMetadata metadata = getMetadata(entity.getClass());
        FieldMapping idField = metadata.getIdField();
        
        if (idField == null) {
            return null;
        }
        
        try {
            java.lang.reflect.Field javaField = idField.getJavaField();
            javaField.setAccessible(true);
            Object value = javaField.get(entity);
            
            // Convert String to ObjectId if it's a valid ObjectId hex string
            if (value instanceof String && !((String) value).isEmpty()) {
                String stringValue = (String) value;
                // Check if it's a valid 24-character hex ObjectId
                if (stringValue.length() == 24 && stringValue.matches("[a-fA-F0-9]+")) {
                    try {
                        return new ObjectId(stringValue);
                    } catch (IllegalArgumentException e) {
                        log.debug("Failed to convert string to ObjectId: {}", stringValue);
                        // Keep as String if conversion fails
                        return value;
                    }
                }
            }
            
            return value;
            
        } catch (IllegalAccessException e) {
            throw new HeliosException("Cannot access ID field", e);
        }
    }
    
    /**
     * Set ID value on entity.
     */
    public void setId(Object entity, Object id) {
        if (entity == null) {
            return;
        }
        
        DocumentMetadata metadata = getMetadata(entity.getClass());
        FieldMapping idField = metadata.getIdField();
        
        if (idField == null) {
            return;
        }
        
        try {
            java.lang.reflect.Field javaField = idField.getJavaField();
            javaField.setAccessible(true);
            
            Object value = id;
            // Convert ObjectId to String if the field is String type
            if (id instanceof ObjectId && javaField.getType() == String.class) {
                value = ((ObjectId) id).toHexString();
            }
            
            javaField.set(entity, value);
            
        } catch (IllegalAccessException e) {
            throw new HeliosException("Cannot set ID field", e);
        }
    }
    
    /**
     * Check if document exists by ID.
     */
    public boolean exists(MongoCollection<org.bson.Document> collection, Object id) {
        // Convert the ID to the appropriate format for MongoDB query
        Object queryId = id;
        if (id instanceof String && !((String) id).isEmpty()) {
            String stringId = (String) id;
            // Check if it's a valid 24-character hex ObjectId
            if (stringId.length() == 24 && stringId.matches("[a-fA-F0-9]+")) {
                try {
                    queryId = new ObjectId(stringId);
                    log.debug("Converted String ID to ObjectId for exists query: {}", stringId);
                } catch (IllegalArgumentException e) {
                    log.debug("Failed to convert string to ObjectId for exists, using as String: {}", stringId);
                    // Keep as String if conversion fails
                }
            }
        }
        return collection.countDocuments(new org.bson.Document("_id", queryId)) > 0;
    }
    
    /**
     * Get the document field name for a Java field (for index creation).
     */
    public String getFieldName(java.lang.reflect.Field field) {
        String documentFieldName = field.getName();
        
        // Check if field has @Id annotation - maps to _id
        if (field.isAnnotationPresent(Id.class)) {
            return "_id";
        }
        
        // Check if field has @Field annotation with custom name
        Field fieldAnnotation = field.getAnnotation(Field.class);
        if (fieldAnnotation != null && !fieldAnnotation.name().isEmpty()) {
            documentFieldName = fieldAnnotation.name();
        }
        
        return documentFieldName;
    }

    /**
     * Get metadata for entity class.
     */
    private DocumentMetadata getMetadata(Class<?> entityClass) {
        return metadataCache.computeIfAbsent(entityClass, this::buildMetadata);
    }
    
    /**
     * Build metadata for entity class.
     */
    private DocumentMetadata buildMetadata(Class<?> entityClass) {
        log.debug("Building metadata for entity class: {}", entityClass.getName());
        
        // Get collection name
        String collectionName = entityClass.getSimpleName().toLowerCase();
        Document documentAnnotation = entityClass.getAnnotation(Document.class);
        if (documentAnnotation != null && !documentAnnotation.collection().isEmpty()) {
            collectionName = documentAnnotation.collection();
        }
        
        // Get field mappings
        List<FieldMapping> fields = new ArrayList<>();
        FieldMapping idField = null;
        
        for (java.lang.reflect.Field field : getAllFields(entityClass)) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) || 
                java.lang.reflect.Modifier.isTransient(field.getModifiers())) {
                continue;
            }
            
            String documentFieldName = field.getName();
            boolean isId = field.isAnnotationPresent(Id.class);
            
            Field fieldAnnotation = field.getAnnotation(Field.class);
            if (fieldAnnotation != null && !fieldAnnotation.name().isEmpty()) {
                documentFieldName = fieldAnnotation.name();
            }
            
            FieldMapping mapping = new FieldMapping(field, documentFieldName, isId);
            fields.add(mapping);
            
            if (isId) {
                if (idField != null) {
                    throw new HeliosException("Multiple @Id fields found in class: " + entityClass.getName());
                }
                idField = mapping;
            }
        }
        
        return new DocumentMetadata(collectionName, fields, idField);
    }
    
    /**
     * Get all fields including inherited ones.
     */
    private List<java.lang.reflect.Field> getAllFields(Class<?> clazz) {
        List<java.lang.reflect.Field> fields = new ArrayList<>();
        
        while (clazz != null && clazz != Object.class) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        
        return fields;
    }
    
    /**
     * Convert Java value to MongoDB value.
     */
    private Object convertToMongoValue(Object value) {
        // For now, let MongoDB driver handle the conversion
        // Could be extended for custom types
        return value;
    }
    
    /**
     * Convert MongoDB value to Java value.
     */
    private Object convertFromMongoValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        
        // Handle basic type conversions
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }
        
        // Handle numeric conversions
        if (value instanceof Number) {
            Number num = (Number) value;
            if (targetType == Integer.class || targetType == int.class) {
                return num.intValue();
            } else if (targetType == Long.class || targetType == long.class) {
                return num.longValue();
            } else if (targetType == Double.class || targetType == double.class) {
                return num.doubleValue();
            } else if (targetType == Float.class || targetType == float.class) {
                return num.floatValue();
            }
        }
        
        // For now, return as is and let reflection handle it
        return value;
    }
    
    /**
     * Metadata for a document class.
     */
    private static class DocumentMetadata {
        private final String collectionName;
        private final List<FieldMapping> fields;
        private final FieldMapping idField;
        
        public DocumentMetadata(String collectionName, List<FieldMapping> fields, FieldMapping idField) {
            this.collectionName = collectionName;
            this.fields = fields;
            this.idField = idField;
        }
        
        public String getCollectionName() { return collectionName; }
        public List<FieldMapping> getFields() { return fields; }
        public FieldMapping getIdField() { return idField; }
    }
    
    /**
     * Mapping information for a field.
     */
    private static class FieldMapping {
        private final java.lang.reflect.Field javaField;
        private final String documentFieldName;
        private final boolean isId;
        
        public FieldMapping(java.lang.reflect.Field javaField, String documentFieldName, boolean isId) {
            this.javaField = javaField;
            this.documentFieldName = documentFieldName;
            this.isId = isId;
        }
        
        public java.lang.reflect.Field getJavaField() { return javaField; }
        public String getDocumentFieldName() { return documentFieldName; }
        public boolean isId() { return isId; }
    }
}
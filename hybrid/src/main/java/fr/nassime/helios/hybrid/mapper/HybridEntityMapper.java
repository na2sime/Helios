package fr.nassime.helios.hybrid.mapper;

import fr.nassime.helios.api.annotations.*;
import fr.nassime.helios.api.annotations.enums.FetchType;
import fr.nassime.helios.api.annotations.enums.PersistenceType;
import fr.nassime.helios.api.exception.ConfigurationException;
import fr.nassime.helios.api.hybrid.HybridEntityMetadata;
import fr.nassime.helios.api.hybrid.HybridFieldMetadata;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mapper responsible for analyzing and caching metadata for hybrid entities.
 */
@Slf4j
public class HybridEntityMapper {
    
    private static final Map<Class<?>, HybridEntityMetadata> metadataCache = new ConcurrentHashMap<>();
    
    /**
     * Get metadata for a hybrid entity class.
     */
    public static HybridEntityMetadata getMetadata(Class<?> entityClass) {
        return metadataCache.computeIfAbsent(entityClass, HybridEntityMapper::buildMetadata);
    }
    
    /**
     * Check if a class is a hybrid entity.
     */
    public static boolean isHybridEntity(Class<?> entityClass) {
        return entityClass.isAnnotationPresent(HybridEntity.class);
    }
    
    /**
     * Build metadata for a hybrid entity class.
     */
    private static HybridEntityMetadata buildMetadata(Class<?> entityClass) {
        log.debug("Building hybrid entity metadata for: {}", entityClass.getSimpleName());
        
        // Validate hybrid entity has both annotations
        HybridEntity hybridAnnotation = entityClass.getAnnotation(HybridEntity.class);
        if (hybridAnnotation == null) {
            throw new ConfigurationException("Class " + entityClass.getName() + " is not annotated with @HybridEntity");
        }
        
        Persistable persistableAnnotation = entityClass.getAnnotation(Persistable.class);
        if (persistableAnnotation == null) {
            throw new ConfigurationException("Hybrid entity " + entityClass.getName() + " must be annotated with @Persistable");
        }
        
        if (persistableAnnotation.type() != PersistenceType.HYBRID) {
            throw new ConfigurationException("Hybrid entity " + entityClass.getName() + " must have @Persistable(type = PersistenceType.HYBRID)");
        }
        
        List<HybridFieldMetadata> relationalFields = new ArrayList<>();
        List<HybridFieldMetadata> documentFields = new ArrayList<>();
        List<HybridFieldMetadata> bothFields = new ArrayList<>();
        
        Field idField = null;
        
        // Analyze all fields
        for (Field field : getAllFields(entityClass)) {
            field.setAccessible(true);
            
            // Check for ID field
            if (field.isAnnotationPresent(Id.class)) {
                idField = field;
            }
            
            HybridFieldMetadata fieldMetadata = buildFieldMetadata(field);
            if (fieldMetadata != null) {
                switch (fieldMetadata.getStorageType()) {
                    case RELATIONAL -> relationalFields.add(fieldMetadata);
                    case DOCUMENT -> documentFields.add(fieldMetadata);
                    case BOTH -> bothFields.add(fieldMetadata);
                }
            }
        }
        
        if (idField == null) {
            throw new ConfigurationException("Hybrid entity " + entityClass.getName() + " must have an @Id field");
        }
        
        return HybridEntityMetadata.builder()
                .entityClass(entityClass)
                .relationalTable(hybridAnnotation.relationalTable())
                .documentCollection(hybridAnnotation.documentCollection())
                .strategy(hybridAnnotation.strategy())
                .autoSync(hybridAnnotation.autoSync())
                .primaryStorage(hybridAnnotation.primaryStorage())
                .relationalFields(relationalFields)
                .documentFields(documentFields)
                .bothFields(bothFields)
                .idField(idField)
                .build();
    }
    
    /**
     * Build metadata for a specific field.
     */
    private static HybridFieldMetadata buildFieldMetadata(Field field) {
        HybridField hybridField = field.getAnnotation(HybridField.class);
        
        // If no HybridField annotation, determine storage based on other annotations
        StorageType storageType;
        String storageName = "";
        boolean sync = true;
        boolean required = false;
        FetchType fetchType = FetchType.EAGER;
        
        if (hybridField != null) {
            storageType = hybridField.storage();
            storageName = hybridField.name();
            sync = hybridField.sync();
            required = hybridField.required();
            fetchType = hybridField.fetch();
        } else {
            // Default behavior: Column annotations go to RELATIONAL, Field to DOCUMENT
            if (field.isAnnotationPresent(Column.class)) {
                storageType = StorageType.RELATIONAL;
                storageName = field.getAnnotation(Column.class).name();
            } else if (field.isAnnotationPresent(fr.nassime.helios.api.annotations.Field.class)) {
                storageType = StorageType.DOCUMENT;
                storageName = field.getAnnotation(fr.nassime.helios.api.annotations.Field.class).name();
            } else {
                // Skip fields without storage annotations
                return null;
            }
        }
        
        return HybridFieldMetadata.builder()
                .javaField(field)
                .fieldName(field.getName())
                .storageName(storageName)
                .storageType(storageType)
                .fetchType(fetchType)
                .sync(sync)
                .required(required)
                .fieldType(field.getType())
                .build();
    }
    
    /**
     * Get all fields including inherited ones.
     */
    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        
        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        
        return fields;
    }
    
    /**
     * Clear the metadata cache (useful for testing).
     */
    public static void clearCache() {
        metadataCache.clear();
    }
}
package fr.nassime.helios.hybrid.mapper;

import fr.nassime.helios.api.annotations.*;
import fr.nassime.helios.api.annotations.enums.FetchType;
import fr.nassime.helios.api.annotations.enums.PersistenceType;
import fr.nassime.helios.api.exception.ConfigurationException;
import fr.nassime.helios.hybrid.HybridEntityMetadata;
import fr.nassime.helios.hybrid.HybridFieldMetadata;
import fr.nassime.helios.hybrid.HybridRelationMetadata;
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
        List<HybridRelationMetadata> hybridRelations = new ArrayList<>();
        List<HybridRelationMetadata> crossStorageRelations = new ArrayList<>();
        
        Field idField = null;
        
        // Analyze all fields
        for (Field field : getAllFields(entityClass)) {
            field.setAccessible(true);
            
            // Check for ID field
            if (field.isAnnotationPresent(Id.class)) {
                idField = field;
                continue; // ID fields are handled separately
            }
            
            // Check if it's a relation field
            if (isRelationField(field)) {
                HybridRelationMetadata relationMetadata = buildRelationMetadata(field, entityClass);
                if (relationMetadata != null) {
                    hybridRelations.add(relationMetadata);
                    if (relationMetadata.isCrossStorageRelation()) {
                        crossStorageRelations.add(relationMetadata);
                    }
                }
                continue; // Relations are handled separately from regular fields
            }
            
            // Handle regular fields
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
                .hybridRelations(hybridRelations)
                .crossStorageRelations(crossStorageRelations)
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
     * Check if a field represents a relation.
     */
    private static boolean isRelationField(Field field) {
        return field.isAnnotationPresent(OneToMany.class) ||
               field.isAnnotationPresent(ManyToOne.class) ||
               field.isAnnotationPresent(OneToOne.class) ||
               field.isAnnotationPresent(ManyToMany.class) ||
               field.isAnnotationPresent(CrossStorageRelation.class);
    }
    
    /**
     * Build metadata for a relation field.
     */
    private static HybridRelationMetadata buildRelationMetadata(Field field, Class<?> sourceEntityClass) {
        log.debug("Building relation metadata for field: {} in class: {}", field.getName(), sourceEntityClass.getSimpleName());
        
        // Determine relation type
        HybridRelationMetadata.RelationType relationType = null;
        String mappedBy = "";
        FetchType fetchType = FetchType.LAZY;
        
        if (field.isAnnotationPresent(OneToMany.class)) {
            OneToMany oneToMany = field.getAnnotation(OneToMany.class);
            relationType = HybridRelationMetadata.RelationType.ONE_TO_MANY;
            mappedBy = oneToMany.mappedBy();
            fetchType = oneToMany.fetch();
        } else if (field.isAnnotationPresent(ManyToOne.class)) {
            ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
            relationType = HybridRelationMetadata.RelationType.MANY_TO_ONE;
            fetchType = manyToOne.fetch();
        } else if (field.isAnnotationPresent(OneToOne.class)) {
            OneToOne oneToOne = field.getAnnotation(OneToOne.class);
            relationType = HybridRelationMetadata.RelationType.ONE_TO_ONE;
            mappedBy = oneToOne.mappedBy();
            fetchType = oneToOne.fetch();
        } else if (field.isAnnotationPresent(ManyToMany.class)) {
            ManyToMany manyToMany = field.getAnnotation(ManyToMany.class);
            relationType = HybridRelationMetadata.RelationType.MANY_TO_MANY;
            mappedBy = manyToMany.mappedBy();
            fetchType = manyToMany.fetch();
        }
        
        if (relationType == null) {
            log.warn("Unknown relation type for field: {} in class: {}", field.getName(), sourceEntityClass.getSimpleName());
            return null;
        }
        
        // Determine target entity class
        Class<?> targetEntityClass = determineTargetEntityClass(field, relationType);
        
        // Analyze cross-storage relation
        CrossStorageRelation crossStorage = field.getAnnotation(CrossStorageRelation.class);
        HybridRelation hybridRelation = field.getAnnotation(HybridRelation.class);
        
        // Determine storage types
        StorageType sourceStorage = determineEntityStorageType(sourceEntityClass);
        StorageType targetStorage = crossStorage != null ? crossStorage.targetStorage() : determineEntityStorageType(targetEntityClass);
        
        // Build relation metadata
        return HybridRelationMetadata.builder()
                .relationField(field)
                .relationName(field.getName())
                .sourceEntityClass(sourceEntityClass)
                .targetEntityClass(targetEntityClass)
                .sourceStorage(sourceStorage)
                .targetStorage(targetStorage)
                .isCrossStorage(crossStorage != null)
                .relationType(relationType)
                .mappedBy(mappedBy)
                .foreignKey(crossStorage != null ? crossStorage.foreignKey() : "")
                .strategy(crossStorage != null ? crossStorage.strategy() : CrossStorageStrategy.REFERENCE)
                .fetchType(hybridRelation != null ? hybridRelation.fetch() : fetchType)
                .sync(hybridRelation != null ? hybridRelation.sync() : true)
                .maintainIntegrity(crossStorage != null ? crossStorage.maintainIntegrity() : true)
                .cached(crossStorage != null ? crossStorage.cached() : false)
                .cacheTtl(crossStorage != null ? crossStorage.cacheTtl() : 300)
                .partitionBy(hybridRelation != null ? hybridRelation.partitionBy() : "")
                .batchSize(hybridRelation != null ? hybridRelation.batchSize() : 100)
                .materialized(hybridRelation != null ? hybridRelation.materialized() : false)
                .queryHint(hybridRelation != null ? hybridRelation.queryHint() : "")
                .build();
    }
    
    /**
     * Determine the target entity class for a relation field.
     */
    private static Class<?> determineTargetEntityClass(Field field, HybridRelationMetadata.RelationType relationType) {
        Class<?> fieldType = field.getType();
        
        // For collection types (OneToMany, ManyToMany), get the generic type
        if (Collection.class.isAssignableFrom(fieldType)) {
            var genericType = field.getGenericType();
            if (genericType instanceof java.lang.reflect.ParameterizedType parameterizedType) {
                var actualTypeArguments = parameterizedType.getActualTypeArguments();
                if (actualTypeArguments.length > 0 && actualTypeArguments[0] instanceof Class<?>) {
                    return (Class<?>) actualTypeArguments[0];
                }
            }
            throw new ConfigurationException("Cannot determine target entity type for collection field: " + field.getName());
        }
        
        // For singular types (ManyToOne, OneToOne), use the field type directly
        return fieldType;
    }
    
    /**
     * Determine the primary storage type for an entity class.
     */
    private static StorageType determineEntityStorageType(Class<?> entityClass) {
        if (isHybridEntity(entityClass)) {
            HybridEntity hybridAnnotation = entityClass.getAnnotation(HybridEntity.class);
            return hybridAnnotation.primaryStorage();
        }
        
        // Check for other annotations to determine storage type
        if (entityClass.isAnnotationPresent(Persistable.class)) {
            Persistable persistable = entityClass.getAnnotation(Persistable.class);
            return switch (persistable.type()) {
                case SQL -> StorageType.RELATIONAL;
                case DOCUMENT -> StorageType.DOCUMENT;
                case HYBRID -> StorageType.RELATIONAL; // Default for hybrid
                case AUTO -> StorageType.RELATIONAL; // Default
            };
        }
        
        // Legacy support
        if (entityClass.isAnnotationPresent(Table.class)) {
            return StorageType.RELATIONAL;
        }
        if (entityClass.isAnnotationPresent(Document.class)) {
            return StorageType.DOCUMENT;
        }
        
        // Default to relational
        return StorageType.RELATIONAL;
    }
    
    /**
     * Clear the metadata cache (useful for testing).
     */
    public static void clearCache() {
        metadataCache.clear();
    }
}
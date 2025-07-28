package fr.nassime.helios.postgres.mapping;

import fr.nassime.helios.api.annotations.*;
import fr.nassime.helios.api.annotations.enums.GenerationType;
import fr.nassime.helios.api.exception.HeliosException;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps entity classes to database metadata.
 */
@Slf4j
public class EntityMapper {
    
    private static final Map<Class<?>, EntityMetadata> METADATA_CACHE = new ConcurrentHashMap<>();
    
    /**
     * Get metadata for an entity class.
     */
    public static EntityMetadata getMetadata(Class<?> entityClass) {
        return METADATA_CACHE.computeIfAbsent(entityClass, EntityMapper::analyzeEntity);
    }
    
    /**
     * Analyze an entity class and extract metadata.
     */
    private static EntityMetadata analyzeEntity(Class<?> entityClass) {
        log.debug("Analyzing entity class: {}", entityClass.getName());
        
        // Check if class is annotated with @Entity
        Entity entityAnnotation = entityClass.getAnnotation(Entity.class);
        if (entityAnnotation == null) {
            throw new HeliosException("Class " + entityClass.getName() + " is not annotated with @Entity");
        }
        
        String tableName = getTableName(entityClass, entityAnnotation);
        String schema = getSchema(entityClass);
        
        Field idField = null;
        String idColumnName = null;
        boolean idGenerated = false;
        Map<String, ColumnMetadata> columns = new HashMap<>();
        List<RelationMetadata> relations = new ArrayList<>();
        
        // Analyze all fields
        for (Field field : getAllFields(entityClass)) {
            field.setAccessible(true);
            
            // Check for @Id annotation
            Id idAnnotation = field.getAnnotation(Id.class);
            if (idAnnotation != null) {
                if (idField != null) {
                    throw new HeliosException("Multiple @Id fields found in " + entityClass.getName());
                }
                idField = field;
                idColumnName = getColumnName(field);
                idGenerated = idAnnotation.strategy() != GenerationType.ASSIGNED;
            }
            
            // Check for column mapping
            if (isColumnField(field)) {
                ColumnMetadata columnMetadata = analyzeColumn(field, idAnnotation != null);
                columns.put(field.getName(), columnMetadata);
            }
            
            // Check for relationships
            if (isRelationField(field)) {
                RelationMetadata relationMetadata = analyzeRelation(field);
                relations.add(relationMetadata);
            }
        }
        
        if (idField == null) {
            throw new HeliosException("No @Id field found in " + entityClass.getName());
        }
        
        return EntityMetadata.builder()
                .entityClass(entityClass)
                .tableName(tableName)
                .schema(schema)
                .idField(idField)
                .idColumnName(idColumnName)
                .idGenerated(idGenerated)
                .columns(columns)
                .relations(relations)
                .build();
    }
    
    /**
     * Get all fields including inherited ones.
     */
    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        
        while (clazz != null && clazz != Object.class) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        
        return fields;
    }
    
    /**
     * Get table name from entity annotation or class name.
     */
    private static String getTableName(Class<?> entityClass, Entity entityAnnotation) {
        if (!entityAnnotation.table().isEmpty()) {
            return entityAnnotation.table();
        }
        
        if (!entityAnnotation.name().isEmpty()) {
            return entityAnnotation.name();
        }
        
        // Convert CamelCase to snake_case
        String className = entityClass.getSimpleName();
        return camelToSnakeCase(className);
    }
    
    /**
     * Get schema name (could be extended to support @Table annotation).
     */
    private static String getSchema(Class<?> entityClass) {
        // For now, return null - could be extended later
        return null;
    }
    
    /**
     * Check if field should be mapped as a column.
     */
    private static boolean isColumnField(Field field) {
        // Skip static and transient fields
        if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) ||
            java.lang.reflect.Modifier.isTransient(field.getModifiers())) {
            return false;
        }
        
        // Skip relation fields
        if (isRelationField(field)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if field is a relationship field.
     */
    private static boolean isRelationField(Field field) {
        return field.getAnnotation(OneToOne.class) != null ||
               field.getAnnotation(OneToMany.class) != null ||
               field.getAnnotation(ManyToOne.class) != null ||
               field.getAnnotation(ManyToMany.class) != null;
    }
    
    /**
     * Analyze a column field.
     */
    private static ColumnMetadata analyzeColumn(Field field, boolean isId) {
        Column columnAnnotation = field.getAnnotation(Column.class);
        
        String columnName = getColumnName(field);
        boolean nullable = isId ? false : (columnAnnotation != null ? columnAnnotation.nullable() : true);
        boolean unique = columnAnnotation != null ? columnAnnotation.unique() : false;
        int length = columnAnnotation != null ? columnAnnotation.length() : 255;
        int precision = columnAnnotation != null ? columnAnnotation.precision() : 0;
        int scale = columnAnnotation != null ? columnAnnotation.scale() : 0;
        boolean insertable = columnAnnotation != null ? columnAnnotation.insertable() : true;
        boolean updatable = columnAnnotation != null ? columnAnnotation.updatable() : true;
        
        return ColumnMetadata.builder()
                .field(field)
                .fieldName(field.getName())
                .columnName(columnName)
                .fieldType(field.getType())
                .id(isId)
                .nullable(nullable)
                .unique(unique)
                .length(length)
                .precision(precision)
                .scale(scale)
                .insertable(insertable)
                .updatable(updatable)
                .build();
    }
    
    /**
     * Get column name from field and annotation.
     */
    private static String getColumnName(Field field) {
        Column columnAnnotation = field.getAnnotation(Column.class);
        
        if (columnAnnotation != null && !columnAnnotation.name().isEmpty()) {
            return columnAnnotation.name();
        }
        
        return camelToSnakeCase(field.getName());
    }
    
    /**
     * Analyze a relationship field.
     */
    private static RelationMetadata analyzeRelation(Field field) {
        // This is a simplified version - in a full implementation,
        // you'd analyze all the relationship annotations
        
        OneToMany oneToMany = field.getAnnotation(OneToMany.class);
        if (oneToMany != null) {
            return RelationMetadata.builder()
                    .field(field)
                    .fieldName(field.getName())
                    .targetEntity(oneToMany.targetEntity() != void.class ? oneToMany.targetEntity() : getGenericType(field))
                    .relationType(RelationMetadata.RelationType.ONE_TO_MANY)
                    .fetchType(oneToMany.fetch())
                    .cascadeTypes(oneToMany.cascade())
                    .mappedBy(oneToMany.mappedBy())
                    .orphanRemoval(oneToMany.orphanRemoval())
                    .build();
        }
        
        ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
        if (manyToOne != null) {
            JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
            String joinColumnName = joinColumn != null && !joinColumn.name().isEmpty()
                    ? joinColumn.name()
                    : camelToSnakeCase(field.getName()) + "_id";
            
            return RelationMetadata.builder()
                    .field(field)
                    .fieldName(field.getName())
                    .targetEntity(manyToOne.targetEntity() != void.class ? manyToOne.targetEntity() : field.getType())
                    .relationType(RelationMetadata.RelationType.MANY_TO_ONE)
                    .fetchType(manyToOne.fetch())
                    .cascadeTypes(manyToOne.cascade())
                    .joinColumn(joinColumnName)
                    .optional(manyToOne.optional())
                    .build();
        }
        
        // Add other relationship types as needed...
        
        throw new HeliosException("Unsupported relation type for field: " + field.getName());
    }
    
    /**
     * Get generic type from collection field.
     */
    private static Class<?> getGenericType(Field field) {
        // Simplified version - would need proper generic type resolution
        return Object.class;
    }
    
    /**
     * Convert camelCase to snake_case.
     */
    private static String camelToSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
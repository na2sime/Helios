package fr.nassime.helios.sql.mapping;

import lombok.Builder;
import lombok.Data;

import java.lang.reflect.Field;

/**
 * Metadata for a column mapping.
 */
@Data
@Builder
public class ColumnMetadata {
    
    private final Field field;
    private final String fieldName;
    private final String columnName;
    private final Class<?> fieldType;
    private final boolean id;
    private final boolean nullable;
    private final boolean unique;
    private final int length;
    private final int precision;
    private final int scale;
    private final boolean insertable;
    private final boolean updatable;
    
    /**
     * Get the field value from an entity instance.
     */
    public Object getValue(Object entity) {
        try {
            field.setAccessible(true);
            return field.get(entity);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to get field value: " + fieldName, e);
        }
    }
    
    /**
     * Set the field value on an entity instance.
     */
    public void setValue(Object entity, Object value) {
        try {
            field.setAccessible(true);
            field.set(entity, convertValue(value));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to set field value: " + fieldName, e);
        }
    }
    
    /**
     * Convert value to the appropriate field type.
     */
    private Object convertValue(Object value) {
        if (value == null) {
            return null;
        }
        
        // Handle common type conversions
        if (fieldType == Long.class || fieldType == long.class) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
        } else if (fieldType == Integer.class || fieldType == int.class) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
        } else if (fieldType == Boolean.class || fieldType == boolean.class) {
            if (value instanceof Boolean) {
                return value;
            } else if (value instanceof String) {
                return Boolean.parseBoolean((String) value);
            }
        }
        
        return value;
    }
}
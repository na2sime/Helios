package fr.nassime.helios.sql.mapping;

import fr.nassime.helios.api.annotations.enums.CascadeType;
import fr.nassime.helios.api.annotations.enums.FetchType;
import lombok.Builder;
import lombok.Data;

import java.lang.reflect.Field;
import java.util.Collection;

/**
 * Metadata for a relationship mapping.
 */
@Data
@Builder
public class RelationMetadata {
    
    private final Field field;
    private final String fieldName;
    private final Class<?> targetEntity;
    private final RelationType relationType;
    private final FetchType fetchType;
    private final CascadeType[] cascadeTypes;
    private final String mappedBy;
    private final String joinColumn;
    private final String inverseJoinColumn;
    private final String joinTable;
    private final boolean orphanRemoval;
    private final boolean optional;
    
    /**
     * Check if this is a collection-based relationship.
     */
    public boolean isCollection() {
        return Collection.class.isAssignableFrom(field.getType());
    }
    
    /**
     * Check if cascade type is present.
     */
    public boolean hasCascade(CascadeType cascadeType) {
        if (cascadeTypes == null || cascadeTypes.length == 0) {
            return false;
        }
        
        for (CascadeType type : cascadeTypes) {
            if (type == cascadeType || type == CascadeType.ALL) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get the field value from an entity instance.
     */
    public Object getValue(Object entity) {
        try {
            field.setAccessible(true);
            return field.get(entity);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to get relation value: " + fieldName, e);
        }
    }
    
    /**
     * Set the field value on an entity instance.
     */
    public void setValue(Object entity, Object value) {
        try {
            field.setAccessible(true);
            field.set(entity, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to set relation value: " + fieldName, e);
        }
    }
    
    /**
     * Relationship types.
     */
    public enum RelationType {
        ONE_TO_ONE,
        ONE_TO_MANY,
        MANY_TO_ONE,
        MANY_TO_MANY
    }
}
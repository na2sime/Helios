package fr.nassime.helios.sql.mapping;

import fr.nassime.helios.api.annotations.enums.GenerationType;
import lombok.Builder;
import lombok.Data;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * Metadata for an entity class.
 */
@Data
@Builder
public class EntityMetadata {
    
    private final Class<?> entityClass;
    private final String tableName;
    private final String schema;
    private final Field idField;
    private final String idColumnName;
    private final boolean idGenerated;
    private final GenerationType generationType;
    private final Map<String, ColumnMetadata> columns;
    private final List<RelationMetadata> relations;
    
    /**
     * Get column metadata by field name.
     */
    public ColumnMetadata getColumnByField(String fieldName) {
        return columns.get(fieldName);
    }
    
    /**
     * Get column metadata by column name.
     */
    public ColumnMetadata getColumnByName(String columnName) {
        return columns.values().stream()
                .filter(col -> col.getColumnName().equals(columnName))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Get all column names.
     */
    public List<String> getColumnNames() {
        return columns.values().stream()
                .map(ColumnMetadata::getColumnName)
                .toList();
    }
    
    /**
     * Get insertable columns (excluding generated IDs).
     */
    public List<ColumnMetadata> getInsertableColumns() {
        return columns.values().stream()
                .filter(col -> col.isInsertable() && !(col.isId() && idGenerated))
                .toList();
    }
    
    /**
     * Get updatable columns (excluding ID).
     */
    public List<ColumnMetadata> getUpdatableColumns() {
        return columns.values().stream()
                .filter(col -> col.isUpdatable() && !col.isId())
                .toList();
    }
}
package fr.nassime.helios.postgres.mapping;

import fr.nassime.helios.api.exception.HeliosException;
import lombok.extern.slf4j.Slf4j;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Maps ResultSet rows to entity objects.
 */
@Slf4j
public class ResultSetMapper {
    
    /**
     * Map a single ResultSet row to an entity.
     */
    public <T> T mapToEntity(ResultSet rs, Class<T> entityClass) throws SQLException {
        if (!rs.next()) {
            return null;
        }
        
        EntityMetadata metadata = EntityMapper.getMetadata(entityClass);
        return mapRowToEntity(rs, metadata);
    }
    
    /**
     * Map all ResultSet rows to a list of entities.
     */
    public <T> List<T> mapToList(ResultSet rs, Class<T> entityClass) throws SQLException {
        List<T> entities = new ArrayList<>();
        EntityMetadata metadata = EntityMapper.getMetadata(entityClass);
        
        while (rs.next()) {
            T entity = mapRowToEntity(rs, metadata);
            entities.add(entity);
        }
        
        return entities;
    }
    
    /**
     * Map a single ResultSet row to an entity instance.
     */
    @SuppressWarnings("unchecked")
    private <T> T mapRowToEntity(ResultSet rs, EntityMetadata metadata) throws SQLException {
        try {
            // Create new instance
            T entity = (T) metadata.getEntityClass().getDeclaredConstructor().newInstance();
            
            // Get available columns from ResultSet
            ResultSetMetaData rsMetadata = rs.getMetaData();
            List<String> availableColumns = new ArrayList<>();
            for (int i = 1; i <= rsMetadata.getColumnCount(); i++) {
                availableColumns.add(rsMetadata.getColumnName(i).toLowerCase());
            }
            
            // Map columns
            for (ColumnMetadata column : metadata.getColumns().values()) {
                String columnName = column.getColumnName().toLowerCase();
                
                if (availableColumns.contains(columnName)) {
                    Object value = getColumnValue(rs, columnName, column.getFieldType());
                    column.setValue(entity, value);
                }
            }
            
            return entity;
            
        } catch (Exception e) {
            throw new HeliosException("Failed to map ResultSet to entity " + metadata.getEntityClass().getName(), e);
        }
    }
    
    /**
     * Get typed value from ResultSet column.
     */
    private Object getColumnValue(ResultSet rs, String columnName, Class<?> fieldType) throws SQLException {
        Object value = rs.getObject(columnName);
        
        if (value == null) {
            return null;
        }
        
        // Handle type conversions
        if (fieldType == String.class) {
            return rs.getString(columnName);
        } else if (fieldType == Long.class || fieldType == long.class) {
            return rs.getLong(columnName);
        } else if (fieldType == Integer.class || fieldType == int.class) {
            return rs.getInt(columnName);
        } else if (fieldType == Boolean.class || fieldType == boolean.class) {
            return rs.getBoolean(columnName);
        } else if (fieldType == Double.class || fieldType == double.class) {
            return rs.getDouble(columnName);
        } else if (fieldType == Float.class || fieldType == float.class) {
            return rs.getFloat(columnName);
        } else if (fieldType == LocalDateTime.class) {
            java.sql.Timestamp timestamp = rs.getTimestamp(columnName);
            return timestamp != null ? timestamp.toLocalDateTime() : null;
        } else if (fieldType == OffsetDateTime.class) {
            java.sql.Timestamp timestamp = rs.getTimestamp(columnName);
            return timestamp != null ? OffsetDateTime.of(timestamp.toLocalDateTime(), java.time.ZoneOffset.UTC) : null;
        } else if (fieldType == java.util.Date.class) {
            return rs.getTimestamp(columnName);
        } else if (fieldType == java.sql.Date.class) {
            return rs.getDate(columnName);
        } else if (fieldType == java.sql.Time.class) {
            return rs.getTime(columnName);
        } else if (fieldType == java.sql.Timestamp.class) {
            return rs.getTimestamp(columnName);
        } else if (fieldType.isEnum()) {
            String enumValue = rs.getString(columnName);
            return enumValue != null ? Enum.valueOf((Class<Enum>) fieldType, enumValue) : null;
        }
        
        // Default: return the raw value
        return value;
    }
}
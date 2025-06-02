package fr.nassime.helios.mapping;

import fr.nassime.helios.exception.HeliosException;
import fr.nassime.helios.util.ReflectionUtils;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ResultSetMapper {

    public <T> List<T> mapToList(ResultSet resultSet, Class<T> entityClass) {
        try {
            List<T> entities = new ArrayList<>();
            Map<String, Field> columnFieldMap = ReflectionUtils.createColumnFieldMap(entityClass);

            while (resultSet.next()) {
                T entity = mapToEntity(resultSet, entityClass, columnFieldMap);
                entities.add(entity);
            }

            return entities;
        } catch (SQLException e) {
            throw new HeliosException("Error mapping ResultSet to a list of entities", e);
        }
    }

    public <T> T mapToEntity(ResultSet resultSet, Class<T> entityClass) {
        try {
            if (resultSet.next()) {
                Map<String, Field> columnFieldMap = ReflectionUtils.createColumnFieldMap(entityClass);
                return mapToEntity(resultSet, entityClass, columnFieldMap);
            }
            return null;
        } catch (SQLException e) {
            throw new HeliosException("Error mapping ResultSet to an entity", e);
        }
    }

    private <T> T mapToEntity(ResultSet resultSet, Class<T> entityClass, Map<String, Field> columnFieldMap) throws SQLException {
        T entity = ReflectionUtils.newInstance(entityClass);
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnName(i);
            Field field = columnFieldMap.get(columnName);

            if (field != null) {
                Object value = getValueFromResultSet(resultSet, i, field.getType());
                ReflectionUtils.setFieldValue(entity, field, value);
            }
        }

        return entity;
    }

    private Object getValueFromResultSet(ResultSet resultSet, int columnIndex, Class<?> targetType) throws SQLException {
        Object value = resultSet.getObject(columnIndex);

        if (value == null) {
            return null;
        }

        // Conversions spécifiques
        if (targetType == LocalDateTime.class && value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime();
        } else if (targetType == LocalDate.class && value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate();
        } else if (targetType.isEnum() && value instanceof String) {
            return Enum.valueOf((Class<Enum>) targetType, (String) value);
        } else if (targetType.isEnum() && value instanceof Number) {
            return targetType.getEnumConstants()[((Number) value).intValue()];
        }

        return value;
    }

    public Map<String, Object> mapToMap(ResultSet resultSet) {
        try {
            Map<String, Object> result = new HashMap<>();
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            if (resultSet.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = resultSet.getObject(i);
                    result.put(columnName, value);
                }
            }

            return result;
        } catch (SQLException e) {
            throw new HeliosException("Error mapping ResultSet to a Map", e);
        }
    }

    public List<Map<String, Object>> mapToMapList(ResultSet resultSet) {
        try {
            List<Map<String, Object>> resultList = new ArrayList<>();
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (resultSet.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = resultSet.getObject(i);
                    row.put(columnName, value);
                }
                resultList.add(row);
            }

            return resultList;
        } catch (SQLException e) {
            throw new HeliosException("Error mapping ResultSet to a Map list", e);
        }
    }
}

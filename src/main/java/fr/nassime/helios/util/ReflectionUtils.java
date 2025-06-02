package fr.nassime.helios.util;

import fr.nassime.helios.annotation.Column;
import fr.nassime.helios.annotation.Id;
import fr.nassime.helios.annotation.Table;
import fr.nassime.helios.exception.HeliosException;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ReflectionUtils {

    public static String getTableName(Class<?> entityClass) {
        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        if (tableAnnotation == null) {
            throw new HeliosException("Entity class " + entityClass.getName() + " is not annotated with @Table");
        }

        String tableName = tableAnnotation.name();
        if (tableName.isEmpty()) {
            tableName = entityClass.getSimpleName().toLowerCase();
        }

        String schema = tableAnnotation.schema();
        if (!schema.isEmpty()) {
            tableName = schema + "." + tableName;
        }

        return tableName;
    }

    public static List<Field> getColumnFields(Class<?> etityClass) {
        return Arrays.stream(etityClass.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Column.class) || field.isAnnotationPresent(Id.class))
                .peek(field -> field.setAccessible(true))
                .toList();
    }

    public static Field getIdField(Class<?> etityClass) {
        return Arrays.stream(etityClass.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Id.class))
                .peek(field -> field.setAccessible(true))
                .findFirst()
                .orElseThrow(() -> new HeliosException("Entity class " + etityClass.getName() + " does not have an @Id field"));
    }

    public static String getColumnName(Field field) {
        if (field.isAnnotationPresent(Column.class)) {
            Column column = field.getAnnotation(Column.class);
            return column.name().isEmpty() ? field.getName() : column.name();
        } else if (field.isAnnotationPresent(Id.class)) {
            return field.getName();
        }
        return field.getName();
    }

    public static Map<String, Field> createColumnFieldMap(Class<?> entityClass) {
        Map<String, Field> columnFieldMap = new HashMap<>();
        getColumnFields(entityClass).forEach(field -> {
            String columnName = getColumnName(field);
            columnFieldMap.put(columnName, field);
        });
        return columnFieldMap;
    }

    public static <T> T newInstance(Class<T> entityClass) {
        try {
            return entityClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            log.error("Failed to create a new instance of class {}", entityClass.getName(), e);
            throw new HeliosException("Failed to create a new instance of class " + entityClass.getName(), e);
        }
    }

    public static void setFieldValue(Object object, Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(object, value);
        } catch (IllegalAccessException e) {
            log.error("Failed to set value for field {} in class {}", field.getName(), object.getClass().getName(), e);
            throw new HeliosException("Failed to set value for field " + field.getName() + " in class " + object.getClass().getName(), e);
        }
    }

    public static Object getFieldValue(Object object, Field field) {
        try {
            field.setAccessible(true);
            return field.get(object);
        } catch (IllegalAccessException e) {
            log.error("Failed to get value for field {} in class {}", field.getName(), object.getClass().getName(), e);
            throw new HeliosException("Failed to get value for field " + field.getName() + " in class " + object.getClass().getName(), e);
        }
    }


}

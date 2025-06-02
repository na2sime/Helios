package fr.nassime.helios.relation;

import fr.nassime.helios.HeliosORM;
import fr.nassime.helios.exception.HeliosException;
import fr.nassime.helios.mapping.EntityMapper;
import fr.nassime.helios.query.SelectBuilder;
import fr.nassime.helios.util.ReflectionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
public class RelationLoader {
    private final HeliosORM orm;

    public <T> void loadRelation(T entity, RelationInfo relationInfo) {
        try {
            switch (relationInfo.getType()) {
                case ONE_TO_ONE:
                    loadOneToOne(entity, relationInfo);
                    break;
                case ONE_TO_MANY:
                    loadOneToMany(entity, relationInfo);
                    break;
                case MANY_TO_ONE:
                    loadManyToOne(entity, relationInfo);
                    break;
                case MANY_TO_MANY:
                    loadManyToMany(entity, relationInfo);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported relationship type: " + relationInfo.getType());
            }
        } catch (Exception e) {
            log.error("Error loading relationship: {}", relationInfo.getField().getName(), e);
            throw new HeliosException("Error loading relationship: " + relationInfo.getField().getName(), e);
        }
    }

    private <T> void loadOneToOne(T entity, RelationInfo relationInfo) {
        if (relationInfo.getMappedBy().isEmpty()) {
            // Côté propriétaire de la relation
            EntityMapper<?> sourceMapper = orm.getEntityMapper(entity.getClass());
            Field joinField = findFieldForJoinColumn(entity.getClass(), relationInfo.getJoinColumn());
            Object foreignKeyValue = ReflectionUtils.getFieldValue(entity, joinField);

            if (foreignKeyValue != null) {
                Optional<?> relatedEntity = orm.findById(relationInfo.getTargetEntityClass(), foreignKeyValue);
                relatedEntity.ifPresent(related ->
                        ReflectionUtils.setFieldValue(entity, relationInfo.getField(), related));
            }
        } else {
            // Côté inverse de la relation
            EntityMapper<?> sourceMapper = orm.getEntityMapper(entity.getClass()); // Initialize sourceMapper
            EntityMapper<?> targetMapper = orm.getEntityMapper(relationInfo.getTargetEntityClass());
            Object primaryKeyValue = ReflectionUtils.getFieldValue(entity, sourceMapper.getIdField());

            if (primaryKeyValue != null) {
                Map<String, Object> conditions = new HashMap<>();
                conditions.put(relationInfo.getMappedBy(), primaryKeyValue);

                List<?> relatedEntities = orm.findByConditions(relationInfo.getTargetEntityClass(), conditions);
                if (!relatedEntities.isEmpty()) {
                    ReflectionUtils.setFieldValue(entity, relationInfo.getField(), relatedEntities.get(0));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void loadOneToMany(T entity, RelationInfo relationInfo) {
        EntityMapper<?> sourceMapper = orm.getEntityMapper(entity.getClass());
        Object primaryKeyValue = ReflectionUtils.getFieldValue(entity, sourceMapper.getIdField());

        if (primaryKeyValue != null) {
            EntityMapper<?> targetMapper = orm.getEntityMapper(relationInfo.getTargetEntityClass());
            Field mappedByField = findFieldByName(relationInfo.getTargetEntityClass(), relationInfo.getMappedBy());
            String joinColumnName = ReflectionUtils.getColumnName(mappedByField);

            Map<String, Object> conditions = new HashMap<>();
            conditions.put(joinColumnName, primaryKeyValue);

            List<?> relatedEntities = orm.findByConditions(relationInfo.getTargetEntityClass(), conditions);

            Collection<Object> collection;
            if (relationInfo.getField().getType().isAssignableFrom(Set.class)) {
                collection = new HashSet<>(relatedEntities);
            } else {
                collection = new ArrayList<>(relatedEntities);
            }

            ReflectionUtils.setFieldValue(entity, relationInfo.getField(), collection);
        }

    }

    private <T> void loadManyToOne(T entity, RelationInfo relationInfo) {
        String joinColumnName = relationInfo.getJoinColumn();
        if (joinColumnName.isEmpty()) {
            joinColumnName = relationInfo.getField().getName() + "_id";
        }

        Field joinField = findFieldForJoinColumn(entity.getClass(), joinColumnName);
        Object foreignKeyValue = ReflectionUtils.getFieldValue(entity, joinField);

        if (foreignKeyValue != null) {
            Optional<?> relatedEntity = orm.findById(relationInfo.getTargetEntityClass(), foreignKeyValue);
            relatedEntity.ifPresent(related ->
                    ReflectionUtils.setFieldValue(entity, relationInfo.getField(), related));
        }

    }

    @SuppressWarnings("unchecked")
    private <T> void loadManyToMany(T entity, RelationInfo relationInfo) {
        EntityMapper<?> sourceMapper = orm.getEntityMapper(entity.getClass());
        Object primaryKeyValue = ReflectionUtils.getFieldValue(entity, sourceMapper.getIdField());

        if (primaryKeyValue != null) {
            List<?> relatedEntities = orm.executeInTransaction(connection -> {
                try {
                    String targetTable = orm.getEntityMapper(relationInfo.getTargetEntityClass()).getTableName();
                    String targetIdColumn = orm.getEntityMapper(relationInfo.getTargetEntityClass()).getIdColumnName();

                    SelectBuilder selectBuilder = SelectBuilder.create()
                            .columns("t.*")
                            .from(targetTable + " t")
                            .innerJoin(relationInfo.getJoinTable() + " j",
                                    "t." + targetIdColumn + " = j." + relationInfo.getInverseJoinColumn())
                            .where(Collections.singletonMap("j." + relationInfo.getJoinColumn(), primaryKeyValue));

                    try (PreparedStatement statement = selectBuilder.prepareStatement(connection);
                         ResultSet resultSet = statement.executeQuery()) {

                        return orm.getResultSetMapper().mapToList(resultSet, relationInfo.getTargetEntityClass());
                    }
                } catch (SQLException e) {
                    throw new HeliosException("Error whyle loading relation ManyToMany", e);
                }
            });

            Collection<Object> collection;
            if (relationInfo.getField().getType().isAssignableFrom(Set.class)) {
                collection = new HashSet<>(relatedEntities);
            } else {
                collection = new ArrayList<>(relatedEntities);
            }

            ReflectionUtils.setFieldValue(entity, relationInfo.getField(), collection);
        }

    }

    private Field findFieldForJoinColumn(Class<?> entityClass, String joinColumnName) {
        for (Field field : ReflectionUtils.getColumnFields(entityClass)) {
            String columnName = ReflectionUtils.getColumnName(field);
            if (columnName.equals(joinColumnName)) {
                return field;
            }
        }
        throw new HeliosException("Join column not found: " + joinColumnName + " in " + entityClass.getName());
    }

    private Field findFieldByName(Class<?> entityClass, String fieldName) {
        try {
            Field field = entityClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new HeliosException("Field not found: " + fieldName + " in " + entityClass.getName());
        }
    }


    public static Class<?> getTargetEntityFromCollection(Field field) {
        if (field.getGenericType() instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) field.getGenericType();
            return (Class<?>) paramType.getActualTypeArguments()[0];
        }
        throw new HeliosException("Unable to determine target entity type for collection " + field.getName());
    }
}

package fr.nassime.helios.mapping;

import fr.nassime.helios.annotation.*;
import fr.nassime.helios.relation.RelationInfo;
import fr.nassime.helios.relation.RelationType;
import fr.nassime.helios.util.ReflectionUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class EntityMapper<T> {
    @Getter
    private final Class<T> entityClass;
    @Getter
    private final String tableName;
    @Getter
    private final Field idField;
    private final List<Field> columnFields;
    private final Map<String, Field> columnFieldMap;

    @Getter
    private final List<RelationInfo> relations;

    public EntityMapper(Class<T> entityClass) {
        this.entityClass = entityClass;
        this.tableName = ReflectionUtils.getTableName(entityClass);
        this.idField = ReflectionUtils.getIdField(entityClass);
        this.columnFields = ReflectionUtils.getColumnFields(entityClass);
        this.columnFieldMap = ReflectionUtils.createColumnFieldMap(entityClass);
        this.relations = findRelations(entityClass);
    }

    private List<RelationInfo> findRelations(Class<?> entityClass) {
        List<RelationInfo> relationInfos = new ArrayList<>();

        for (Field field : entityClass.getDeclaredFields()) {
            field.setAccessible(true);

            if (field.isAnnotationPresent(OneToOne.class)) {
                OneToOne annotation = field.getAnnotation(OneToOne.class);

                String joinColumn = annotation.joinColumn();
                if (joinColumn.isEmpty() && annotation.mappedBy().isEmpty()) {
                    joinColumn = field.getName() + "_id";
                }

                RelationInfo relationInfo = RelationInfo.builder()
                        .type(RelationType.ONE_TO_ONE)
                        .field(field)
                        .targetEntityClass(field.getType())
                        .joinColumn(joinColumn)
                        .mappedBy(annotation.mappedBy())
                        .fetchType(annotation.fetch())
                        .cascade(annotation.cascade())
                        .collection(false)
                        .build();

                relationInfos.add(relationInfo);
            } else if (field.isAnnotationPresent(OneToMany.class)) {
                OneToMany annotation = field.getAnnotation(OneToMany.class);

                RelationInfo relationInfo = RelationInfo.builder()
                        .type(RelationType.ONE_TO_MANY)
                        .field(field)
                        .targetEntityClass(annotation.targetEntity())
                        .mappedBy(annotation.mappedBy())
                        .fetchType(annotation.fetch())
                        .cascade(annotation.cascade())
                        .orphanRemoval(annotation.orphanRemoval())
                        .collection(true)
                        .build();

                relationInfos.add(relationInfo);
            } else if (field.isAnnotationPresent(ManyToOne.class)) {
                ManyToOne annotation = field.getAnnotation(ManyToOne.class);

                String joinColumn = annotation.joinColumn();
                if (joinColumn.isEmpty()) {
                    joinColumn = field.getName() + "_id";
                }

                RelationInfo relationInfo = RelationInfo.builder()
                        .type(RelationType.MANY_TO_ONE)
                        .field(field)
                        .targetEntityClass(field.getType())
                        .joinColumn(joinColumn)
                        .fetchType(annotation.fetch())
                        .cascade(annotation.cascade())
                        .collection(false)
                        .build();

                relationInfos.add(relationInfo);
            } else if (field.isAnnotationPresent(ManyToMany.class)) {
                ManyToMany annotation = field.getAnnotation(ManyToMany.class);

                RelationInfo relationInfo = RelationInfo.builder()
                        .type(RelationType.MANY_TO_MANY)
                        .field(field)
                        .targetEntityClass(annotation.targetEntity())
                        .joinTable(annotation.joinTable())
                        .joinColumn(annotation.joinColumn())
                        .inverseJoinColumn(annotation.inverseJoinColumn())
                        .fetchType(annotation.fetch())
                        .cascade(annotation.cascade())
                        .collection(true)
                        .build();

                relationInfos.add(relationInfo);
            }
        }

        return relationInfos;
    }

    public Map<String, Object> toColumnValues(T entity, boolean includeId) {
        Map<String, Object> values = new HashMap<>();

        for (Field field : columnFields) {
            // Ignorer le champ ID si nécessaire
            if (!includeId && field.equals(idField)) {
                continue;
            }

            // Ignorer les champs non insertables ou non updatables selon le contexte
            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);
                boolean isInsert = !includeId; // Si includeId est false, c'est une opération INSERT
                if ((isInsert && !column.insertable()) || (!isInsert && !column.updatable())) {
                    continue;
                }
            }

            String columnName = ReflectionUtils.getColumnName(field);
            Object value = ReflectionUtils.getFieldValue(entity, field);
            values.put(columnName, value);
        }

        return values;
    }

    public Object getIdValue(T entity) {
        return ReflectionUtils.getFieldValue(entity, idField);
    }

    public void setIdValue(T entity, Object idValue) {
        ReflectionUtils.setFieldValue(entity, idField, idValue);
    }

    public String getIdColumnName() {
        return ReflectionUtils.getColumnName(idField);
    }

    public List<String> getColumnNames(boolean includeId) {
        return columnFields.stream()
                .filter(field -> includeId || !field.equals(idField))
                .map(ReflectionUtils::getColumnName)
                .toList();
    }

    public boolean isIdGenerated() {
        Id idAnnotation = idField.getAnnotation(Id.class);
        return idAnnotation.generated();
    }

    public List<RelationInfo> getEagerRelations() {
        return relations.stream()
                .filter(relation -> relation.getFetchType() == Relation.FetchType.EAGER)
                .toList();
    }

    public List<RelationInfo> getLazyRelations() {
        return relations.stream()
                .filter(relation -> relation.getFetchType() == Relation.FetchType.LAZY)
                .toList();
    }

    public RelationInfo getRelationByFieldName(String fieldName) {
        return relations.stream()
                .filter(relation -> relation.getField().getName().equals(fieldName))
                .findFirst()
                .orElse(null);
    }
}
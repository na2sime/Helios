package fr.nassime.helios;

import fr.nassime.helios.connection.ConnectionManager;
import fr.nassime.helios.connection.DataSourceConfig;
import fr.nassime.helios.exception.HeliosException;
import fr.nassime.helios.mapping.EntityMapper;
import fr.nassime.helios.mapping.ResultSetMapper;
import fr.nassime.helios.query.DeleteBuilder;
import fr.nassime.helios.query.InsertBuilder;
import fr.nassime.helios.query.SelectBuilder;
import fr.nassime.helios.query.UpdateBuilder;
import fr.nassime.helios.relation.RelationInfo;
import fr.nassime.helios.relation.RelationLoader;
import fr.nassime.helios.transaction.TransactionManager;
import fr.nassime.helios.util.ReflectionUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public class HeliosORM implements AutoCloseable {
    @Getter
    private final ConnectionManager connectionManager;
    private final TransactionManager transactionManager;
    @Getter
    private final ResultSetMapper resultSetMapper;
    private final Map<Class<?>, EntityMapper<?>> entityMappers;
    private final RelationLoader relationLoader;

    private HeliosORM(DataSourceConfig config) {
        this.connectionManager = new ConnectionManager(config);
        this.transactionManager = new TransactionManager();
        this.resultSetMapper = new ResultSetMapper();
        this.entityMappers = new ConcurrentHashMap<>();
        this.relationLoader = new RelationLoader(this);
        log.info("Helios ORM successfully initialized");
    }

    public static HeliosORM create(DataSourceConfig config) {
        return new HeliosORM(config);
    }

    public Connection getConnection() {
        try {
            return connectionManager.getConnection();
        } catch (SQLException e) {
            throw new HeliosException("Unable to obtain a database fr.nassime.helios.connection", e);
        }
    }

    public void close() {
        connectionManager.close();
    }

    public <T> T executeInTransaction(Function<Connection, T> operation) {
        try (Connection connection = getConnection()) {
            return transactionManager.executeInTransaction(connection, operation);
        } catch (SQLException e) {
            throw new HeliosException("Error during fr.nassime.helios.transaction execution", e);
        }
    }

    public void executeInTransactionWithoutResult(Consumer<Connection> operation) {
        try (Connection connection = getConnection()) {
            transactionManager.executeInTransactionWithoutResult(connection, operation);
        } catch (SQLException e) {
            throw new HeliosException("Error during fr.nassime.helios.transaction execution", e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> EntityMapper<T> getEntityMapper(Class<T> entityClass) {
        return (EntityMapper<T>) entityMappers.computeIfAbsent(entityClass, EntityMapper::new);
    }

    public <T, ID> Optional<T> findById(Class<T> entityClass, ID id) {
        EntityMapper<T> mapper = getEntityMapper(entityClass);
        String tableName = mapper.getTableName();
        String idColumn = mapper.getIdColumnName();

        return executeInTransaction(connection -> {
            SelectBuilder selectBuilder = SelectBuilder.create()
                    .columns()
                    .from(tableName)
                    .where(Collections.singletonMap(idColumn, id));

            try (PreparedStatement statement = selectBuilder.prepareStatement(connection);
                 ResultSet resultSet = statement.executeQuery()) {

                T entity = resultSetMapper.mapToEntity(resultSet, entityClass);

                if (entity != null) {
                    // Load EAGER relations
                    loadEagerRelations(entity);
                }

                return Optional.ofNullable(entity);
            } catch (SQLException e) {
                throw new HeliosException("Error while finding entity by ID", e);
            }
        });
    }

    public <T> List<T> findAll(Class<T> entityClass) {
        EntityMapper<T> mapper = getEntityMapper(entityClass);
        String tableName = mapper.getTableName();

        return executeInTransaction(connection -> {
            SelectBuilder selectBuilder = SelectBuilder.create()
                    .columns()
                    .from(tableName);

            try (PreparedStatement statement = selectBuilder.prepareStatement(connection);
                 ResultSet resultSet = statement.executeQuery()) {

                List<T> entities = resultSetMapper.mapToList(resultSet, entityClass);

                for (T entity : entities) {
                    loadEagerRelations(entity);
                }

                return entities;
            } catch (SQLException e) {
                throw new HeliosException("Error while finding all entities", e);
            }
        });
    }

    public <T> List<T> findByConditions(Class<T> entityClass, Map<String, Object> conditions) {
        EntityMapper<T> mapper = getEntityMapper(entityClass);
        String tableName = mapper.getTableName();

        return executeInTransaction(connection -> {
            SelectBuilder selectBuilder = SelectBuilder.create()
                    .columns()
                    .from(tableName)
                    .where(conditions);

            try (PreparedStatement statement = selectBuilder.prepareStatement(connection);
                 ResultSet resultSet = statement.executeQuery()) {

                List<T> entities = resultSetMapper.mapToList(resultSet, entityClass);

                // Load EAGER relations for each entity
                for (T entity : entities) {
                    loadEagerRelations(entity);
                }

                return entities;
            } catch (SQLException e) {
                throw new HeliosException("Error while finding entities by conditions", e);
            }
        });
    }

    public <T> void loadRelation(T entity, String fieldName) {
        EntityMapper<T> mapper = getEntityMapper((Class<T>) entity.getClass());
        RelationInfo relationInfo = mapper.getRelationByFieldName(fieldName);

        if (relationInfo == null) {
            throw new HeliosException("Relation not found: " + fieldName);
        }

        relationLoader.loadRelation(entity, relationInfo);
    }

    public <T> void loadEagerRelations(T entity) {
        EntityMapper<T> mapper = getEntityMapper((Class<T>) entity.getClass());
        List<RelationInfo> eagerRelations = mapper.getEagerRelations();

        for (RelationInfo relationInfo : eagerRelations) {
            relationLoader.loadRelation(entity, relationInfo);
        }
    }

    public <T> T save(T entity) {
        @SuppressWarnings("unchecked")
        Class<T> entityClass = (Class<T>) entity.getClass();
        EntityMapper<T> mapper = getEntityMapper(entityClass);

        Object idValue = mapper.getIdValue(entity);

        if (idValue == null || (idValue instanceof Number && ((Number) idValue).longValue() == 0)) {
            // Insert
            entity = insert(entity);
        } else {
            // Update
            entity = update(entity);
        }

        // Save cascading relations
        saveRelationsInCascade(entity, mapper);

        return entity;
    }

    private <T> void saveRelationsInCascade(T entity, EntityMapper<T> mapper) {
        List<RelationInfo> cascadeRelations = mapper.getRelations().stream()
                .filter(RelationInfo::isCascade)
                .toList();

        for (RelationInfo relationInfo : cascadeRelations) {
            Object relatedValue = ReflectionUtils.getFieldValue(entity, relationInfo.getField());

            if (relatedValue == null) {
                continue;
            }

            if (relationInfo.isCollection()) {
                Collection<?> collection = (Collection<?>) relatedValue;
                for (Object relatedEntity : collection) {
                    save(relatedEntity);
                }
            } else {
                save(relatedValue);
            }
        }
    }

    public <T> T insert(T entity) {
        @SuppressWarnings("unchecked")
        Class<T> entityClass = (Class<T>) entity.getClass();
        EntityMapper<T> mapper = getEntityMapper(entityClass);

        String tableName = mapper.getTableName();
        Map<String, Object> columnValues = mapper.toColumnValues(entity, false);

        return executeInTransaction(connection -> {
            InsertBuilder insertBuilder = InsertBuilder.create()
                    .into(tableName)
                    .values(columnValues);

            if (mapper.isIdGenerated()) {
                insertBuilder.returning(mapper.getIdColumnName());
            }

            try (PreparedStatement statement = mapper.isIdGenerated()
                    ? insertBuilder.prepareStatement(connection)
                    : insertBuilder.prepareStatementWithGeneratedKeys(connection)) {

                if (mapper.isIdGenerated()) {
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            Object generatedId = resultSet.getObject(1);
                            mapper.setIdValue(entity, generatedId);
                        }
                    }
                } else {
                    statement.executeUpdate();
                    try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            Object generatedId = generatedKeys.getObject(1);
                            mapper.setIdValue(entity, generatedId);
                        }
                    }
                }

                return entity;
            } catch (SQLException e) {
                throw new HeliosException("Error during entity insertion", e);
            }
        });
    }

    public <T> T update(T entity) {
        @SuppressWarnings("unchecked")
        Class<T> entityClass = (Class<T>) entity.getClass();
        EntityMapper<T> mapper = getEntityMapper(entityClass);

        String tableName = mapper.getTableName();
        String idColumn = mapper.getIdColumnName();
        Object idValue = mapper.getIdValue(entity);

        Map<String, Object> columnValues = mapper.toColumnValues(entity, true);
        columnValues.remove(idColumn); // Do not update the ID

        return executeInTransaction(connection -> {
            Map<String, Object> whereConditions = new HashMap<>();
            whereConditions.put(idColumn, idValue);

            UpdateBuilder updateBuilder = UpdateBuilder.create()
                    .table(tableName)
                    .set(columnValues)
                    .where(whereConditions);

            try (PreparedStatement statement = updateBuilder.prepareStatement(connection)) {
                int rowsAffected = statement.executeUpdate();
                if (rowsAffected == 0) {
                    throw new HeliosException("No rows affected during entity update with ID: " + idValue);
                }
                return entity;
            } catch (SQLException e) {
                throw new HeliosException("Error during entity update", e);
            }
        });
    }

    public <T> boolean delete(T entity) {
        @SuppressWarnings("unchecked")
        Class<T> entityClass = (Class<T>) entity.getClass();
        EntityMapper<T> mapper = getEntityMapper(entityClass);

        // Delete orphaned relations if necessary
        deleteOrphanedRelations(entity, mapper);

        String tableName = mapper.getTableName();
        String idColumn = mapper.getIdColumnName();
        Object idValue = mapper.getIdValue(entity);

        return executeInTransaction(connection -> {
            Map<String, Object> whereConditions = new HashMap<>();
            whereConditions.put(idColumn, idValue);

            DeleteBuilder deleteBuilder = DeleteBuilder.create()
                    .from(tableName)
                    .where(whereConditions);

            try (PreparedStatement statement = deleteBuilder.prepareStatement(connection)) {
                int rowsAffected = statement.executeUpdate();
                return rowsAffected > 0;
            } catch (SQLException e) {
                throw new HeliosException("Error during entity deletion", e);
            }
        });
    }

    private <T> void deleteOrphanedRelations(T entity, EntityMapper<T> mapper) {
        List<RelationInfo> orphanRemovalRelations = mapper.getRelations().stream()
                .filter(RelationInfo::isOrphanRemoval)
                .toList();

        for (RelationInfo relationInfo : orphanRemovalRelations) {
            if (relationInfo.getType() == fr.nassime.helios.relation.RelationType.ONE_TO_MANY) {
                loadRelation(entity, relationInfo.getField().getName());
                Collection<?> collection = (Collection<?>) ReflectionUtils.getFieldValue(entity, relationInfo.getField());

                if (collection != null) {
                    for (Object relatedEntity : collection) {
                        delete(relatedEntity);
                    }
                }
            }
        }
    }

    public int executeUpdate(String sql, Object... params) {
        return executeInTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    statement.setObject(i + 1, params[i]);
                }
                return statement.executeUpdate();
            } catch (SQLException e) {
                throw new HeliosException("Error during fr.nassime.helios.query execution", e);
            }
        });
    }

    public <T> List<T> executeQuery(String sql, Class<T> entityClass, Object... params) {
        return executeInTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    statement.setObject(i + 1, params[i]);
                }

                try (ResultSet resultSet = statement.executeQuery()) {
                    List<T> entities = resultSetMapper.mapToList(resultSet, entityClass);

                    // Load EAGER relations for each entity
                    for (T entity : entities) {
                        loadEagerRelations(entity);
                    }

                    return entities;
                }
            } catch (SQLException e) {
                throw new HeliosException("Error during fr.nassime.helios.query execution", e);
            }
        });
    }

    public List<Map<String, Object>> executeQueryForMaps(String sql, Object... params) {
        return executeInTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    statement.setObject(i + 1, params[i]);
                }

                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSetMapper.mapToMapList(resultSet);
                }
            } catch (SQLException e) {
                throw new HeliosException("Error during fr.nassime.helios.query execution", e);
            }
        });
    }
}
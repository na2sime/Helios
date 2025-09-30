package fr.nassime.helios.api;

import fr.nassime.helios.api.annotations.Persistable;
import fr.nassime.helios.api.config.MultiDatabaseConfig;
import fr.nassime.helios.api.exception.HeliosException;
import fr.nassime.helios.api.query.Query;
import fr.nassime.helios.api.transaction.Transaction;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Multi-database session that routes operations to the appropriate database
 * based on the entity's @Persistable annotation.
 */
@Slf4j
class MultiDatabaseSession implements HeliosSession {

    private final Map<String, HeliosSessionFactory> sessionFactories;
    private final Map<String, HeliosSession> sessions = new HashMap<>();
    private final MultiDatabaseConfig config;
    private boolean closed = false;

    MultiDatabaseSession(Map<String, HeliosSessionFactory> sessionFactories, MultiDatabaseConfig config) {
        this.sessionFactories = sessionFactories;
        this.config = config;
    }

    @Override
    public <T> T save(T entity) {
        return getSessionForEntity(entity.getClass()).save(entity);
    }

    @Override
    public <T, ID> Optional<T> findById(Class<T> entityClass, ID id) {
        return getSessionForEntity(entityClass).findById(entityClass, id);
    }

    @Override
    public <T> List<T> findAll(Class<T> entityClass) {
        return getSessionForEntity(entityClass).findAll(entityClass);
    }

    @Override
    public <T> boolean delete(T entity) {
        return getSessionForEntity(entity.getClass()).delete(entity);
    }

    @Override
    public <T> Query<T> createQuery(Class<T> entityClass) {
        return getSessionForEntity(entityClass).createQuery(entityClass);
    }

    @Override
    public <T> List<T> executeNativeQuery(String query, Class<T> resultClass, Object... parameters) {
        return getSessionForEntity(resultClass).executeNativeQuery(query, resultClass, parameters);
    }

    @Override
    public int executeUpdate(String query, Object... parameters) {
        throw new UnsupportedOperationException("executeUpdate requires explicit database selection. Use openSession(databaseName)");
    }

    @Override
    public <T> void loadRelation(T entity, String relationName) {
        getSessionForEntity(entity.getClass()).loadRelation(entity, relationName);
    }

    @Override
    public Transaction beginTransaction() {
        throw new UnsupportedOperationException("Multi-database transactions not yet supported. Use openSession(databaseName) for single-database transactions");
    }

    @Override
    public <T> T executeInTransaction(Function<HeliosSession, T> operation) {
        throw new UnsupportedOperationException("Multi-database transactions not yet supported. Use openSession(databaseName) for single-database transactions");
    }

    @Override
    public void executeInTransaction(Consumer<HeliosSession> operation) {
        throw new UnsupportedOperationException("Multi-database transactions not yet supported. Use openSession(databaseName) for single-database transactions");
    }

    @Override
    public void flush() {
        for (HeliosSession session : sessions.values()) {
            session.flush();
        }
    }

    @Override
    public void clear() {
        for (HeliosSession session : sessions.values()) {
            session.clear();
        }
    }

    @Override
    public void close() {
        if (!closed) {
            for (HeliosSession session : sessions.values()) {
                try {
                    session.close();
                } catch (Exception e) {
                    log.error("Error closing session", e);
                }
            }
            sessions.clear();
            closed = true;
        }
    }

    /**
     * Get or create a session for the appropriate database based on entity annotation
     */
    private HeliosSession getSessionForEntity(Class<?> entityClass) {
        String databaseName = getDatabaseNameForEntity(entityClass);

        // Get or create session for this database
        return sessions.computeIfAbsent(databaseName, name -> {
            HeliosSessionFactory factory = sessionFactories.get(name);
            if (factory == null) {
                throw new HeliosException("Database '" + name + "' not configured for entity " + entityClass.getName());
            }
            log.debug("Opening session for database '{}'", name);
            return factory.openSession();
        });
    }

    /**
     * Determine which database to use for this entity
     */
    private String getDatabaseNameForEntity(Class<?> entityClass) {
        Persistable annotation = entityClass.getAnnotation(Persistable.class);

        if (annotation == null) {
            throw new HeliosException("Entity " + entityClass.getName() + " is not annotated with @Persistable");
        }

        // Check if entity specifies a database
        String database = annotation.database();
        if (database != null && !database.isEmpty()) {
            if (!config.hasDatabase(database)) {
                throw new HeliosException("Database '" + database + "' specified in @Persistable for " +
                        entityClass.getName() + " is not configured");
            }
            return database;
        }

        // Use default database
        return config.getDefaultDatabase().getName();
    }
}
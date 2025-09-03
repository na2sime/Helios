package fr.nassime.helios.api.repository;

import fr.nassime.helios.api.HeliosSession;
import fr.nassime.helios.api.query.Query;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Base implementation of the Repository interface.
 * Provides common functionality for all repositories.
 *
 * @param <T>  The entity type
 * @param <ID> The ID type of the entity
 */
public class BaseRepository<T, ID> implements Repository<T, ID> {
    
    protected final HeliosSession session;
    protected final Class<T> entityClass;
    
    @SuppressWarnings("unchecked")
    public BaseRepository(HeliosSession session) {
        this.session = session;
        this.entityClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass())
                .getActualTypeArguments()[0];
    }
    
    public BaseRepository(HeliosSession session, Class<T> entityClass) {
        this.session = session;
        this.entityClass = entityClass;
    }
    
    @Override
    public Optional<T> findById(ID id) {
        return session.findById(entityClass, id);
    }
    
    @Override
    public List<T> findAll() {
        return session.findAll(entityClass);
    }
    
    @Override
    public T save(T entity) {
        return session.save(entity);
    }
    
    @Override
    public List<T> saveAll(Iterable<T> entities) {
        List<T> savedEntities = new ArrayList<>();
        for (T entity : entities) {
            savedEntities.add(session.save(entity));
        }
        return savedEntities;
    }
    
    @Override
    public boolean deleteById(ID id) {
        Optional<T> entity = findById(id);
        if (entity.isPresent()) {
            return session.delete(entity.get());
        }
        return false;
    }
    
    @Override
    public boolean delete(T entity) {
        return session.delete(entity);
    }
    
    @Override
    public void deleteAll(Iterable<T> entities) {
        for (T entity : entities) {
            session.delete(entity);
        }
    }
    
    @Override
    public void deleteAll() {
        List<T> allEntities = findAll();
        deleteAll(allEntities);
    }
    
    @Override
    public boolean existsById(ID id) {
        return findById(id).isPresent();
    }
    
    @Override
    public long count() {
        return findAll().size(); // Basic implementation, can be optimized
    }
    
    @Override
    public Query<T> createQuery() {
        return session.createQuery(entityClass);
    }
    
    @Override
    public List<T> findByField(String fieldName, Object value) {
        return createQuery()
                .where(fieldName, value)
                .getResultList();
    }
    
    @Override
    public Optional<T> findFirstByField(String fieldName, Object value) {
        List<T> results = createQuery()
                .where(fieldName, value)
                .limit(1)
                .getResultList();
        
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    @Override
    public Class<T> getEntityClass() {
        return entityClass;
    }
    
    /**
     * Get the Helios session used by this repository.
     *
     * @return the Helios session
     */
    protected HeliosSession getSession() {
        return session;
    }
}
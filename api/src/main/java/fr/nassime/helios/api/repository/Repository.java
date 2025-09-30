package fr.nassime.helios.api.repository;

import fr.nassime.helios.api.query.Query;

import java.util.List;
import java.util.Optional;

/**
 * Generic repository interface providing common CRUD operations for entities.
 * This interface follows the Repository pattern to simplify data access operations.
 *
 * @param <T>  The entity type
 * @param <ID> The ID type of the entity
 */
public interface Repository<T, ID> {
    
    /**
     * Find an entity by its ID.
     *
     * @param id the ID of the entity to find
     * @return an Optional containing the entity if found, empty otherwise
     */
    Optional<T> findById(ID id);
    
    /**
     * Find all entities.
     *
     * @return a list of all entities
     */
    List<T> findAll();
    
    /**
     * Save an entity (insert or update).
     *
     * @param entity the entity to save
     * @return the saved entity
     */
    T save(T entity);
    
    /**
     * Save multiple entities in a batch operation.
     *
     * @param entities the entities to save
     * @return the list of saved entities
     */
    List<T> saveAll(Iterable<T> entities);
    
    /**
     * Delete an entity by its ID.
     *
     * @param id the ID of the entity to delete
     * @return true if the entity was deleted, false otherwise
     */
    boolean deleteById(ID id);
    
    /**
     * Delete an entity.
     *
     * @param entity the entity to delete
     * @return true if the entity was deleted, false otherwise
     */
    boolean delete(T entity);
    
    /**
     * Delete multiple entities.
     *
     * @param entities the entities to delete
     */
    void deleteAll(Iterable<T> entities);
    
    /**
     * Delete all entities.
     */
    void deleteAll();
    
    /**
     * Check if an entity exists by its ID.
     *
     * @param id the ID to check
     * @return true if the entity exists, false otherwise
     */
    boolean existsById(ID id);
    
    /**
     * Count the number of entities.
     *
     * @return the number of entities
     */
    long count();
    
    /**
     * Create a query for this entity type.
     *
     * @return a Query instance for building custom queries
     */
    Query<T> createQuery();
    
    /**
     * Find entities by a field value.
     *
     * @param fieldName the name of the field to search by
     * @param value     the value to search for
     * @return a list of matching entities
     */
    List<T> findByField(String fieldName, Object value);
    
    /**
     * Find the first entity by a field value.
     *
     * @param fieldName the name of the field to search by
     * @param value     the value to search for
     * @return an Optional containing the first matching entity if found
     */
    Optional<T> findFirstByField(String fieldName, Object value);
    
    /**
     * Get the entity class this repository manages.
     *
     * @return the entity class
     */
    Class<T> getEntityClass();
}
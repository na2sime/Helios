package fr.nassime.helios.api.repository;

import fr.nassime.helios.api.HeliosSession;

/**
 * Factory for creating repository instances.
 * Provides a convenient way to create repositories for entities.
 */
public class RepositoryFactory {
    
    private final HeliosSession session;
    
    public RepositoryFactory(HeliosSession session) {
        this.session = session;
    }
    
    /**
     * Create a repository for the specified entity class.
     *
     * @param entityClass the entity class
     * @param idClass     the ID class
     * @param <T>         the entity type
     * @param <ID>        the ID type
     * @return a repository instance
     */
    public <T, ID> Repository<T, ID> getRepository(Class<T> entityClass, Class<ID> idClass) {
        return new BaseRepository<>(session, entityClass);
    }
    
    /**
     * Create a repository for the specified entity class.
     * Uses Object as the ID type (less type-safe but more flexible).
     *
     * @param entityClass the entity class
     * @param <T>         the entity type
     * @return a repository instance
     */
    @SuppressWarnings("unchecked")
    public <T> Repository<T, Object> getRepository(Class<T> entityClass) {
        return new BaseRepository<>(session, entityClass);
    }
    
    /**
     * Create a custom repository instance.
     *
     * @param repositoryClass the repository implementation class
     * @param <R>             the repository type
     * @return a repository instance
     * @throws RuntimeException if the repository cannot be instantiated
     */
    public <R extends Repository<?, ?>> R getCustomRepository(Class<R> repositoryClass) {
        try {
            // Try constructor with HeliosSession parameter
            return repositoryClass.getConstructor(HeliosSession.class).newInstance(session);
        } catch (Exception e) {
            throw new RuntimeException("Could not instantiate repository: " + repositoryClass.getName(), e);
        }
    }
    
    /**
     * Get the underlying Helios session.
     *
     * @return the Helios session
     */
    public HeliosSession getSession() {
        return session;
    }
}
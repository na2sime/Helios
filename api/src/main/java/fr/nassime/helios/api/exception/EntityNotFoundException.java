package fr.nassime.helios.api.exception;

/**
 * Exception thrown when an entity is not found.
 */
public class EntityNotFoundException extends HeliosException {
    
    public EntityNotFoundException(String message) {
        super(message);
    }
    
    public EntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public EntityNotFoundException(Class<?> entityClass, Object id) {
        super("Entity " + entityClass.getSimpleName() + " with id " + id + " not found");
    }
}
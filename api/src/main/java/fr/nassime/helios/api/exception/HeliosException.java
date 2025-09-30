package fr.nassime.helios.api.exception;

/**
 * Base exception for all Helios ORM exceptions.
 */
public class HeliosException extends RuntimeException {
    
    public HeliosException(String message) {
        super(message);
    }
    
    public HeliosException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public HeliosException(Throwable cause) {
        super(cause);
    }
}
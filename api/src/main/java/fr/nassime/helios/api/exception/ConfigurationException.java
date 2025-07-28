package fr.nassime.helios.api.exception;

/**
 * Exception thrown for configuration-related errors.
 */
public class ConfigurationException extends HeliosException {
    
    public ConfigurationException(String message) {
        super(message);
    }
    
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
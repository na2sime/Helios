package fr.nassime.helios.api.exception;

/**
 * Exception thrown for transaction-related errors.
 */
public class TransactionException extends HeliosException {
    
    public TransactionException(String message) {
        super(message);
    }
    
    public TransactionException(String message, Throwable cause) {
        super(message, cause);
    }
}
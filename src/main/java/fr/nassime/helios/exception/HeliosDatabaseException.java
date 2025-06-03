package fr.nassime.helios.exception;

public class HeliosDatabaseException extends RuntimeException {
    public HeliosDatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
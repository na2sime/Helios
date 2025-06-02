package fr.nassime.helios.exception;

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

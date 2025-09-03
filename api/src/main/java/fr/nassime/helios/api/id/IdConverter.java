package fr.nassime.helios.api.id;

/**
 * Pluggable interface for converting entity IDs between different formats
 * required by various storage backends.
 * 
 * This interface allows Helios to seamlessly handle different ID types
 * across SQL databases (Long, UUID) and NoSQL databases (String, ObjectId).
 * 
 * @param <T> The Java type of the ID
 * @param <S> The storage-specific type of the ID
 */
public interface IdConverter<T, S> {
    
    /**
     * Convert a Java ID to the format expected by the storage backend.
     * 
     * @param javaId the ID in Java format (can be null)
     * @return the ID in storage format, or null if input is null
     * @throws IllegalArgumentException if the conversion is invalid
     */
    S toStorageFormat(T javaId);
    
    /**
     * Convert a storage ID to the Java format expected by the application.
     * 
     * @param storageId the ID in storage format (can be null)
     * @return the ID in Java format, or null if input is null
     * @throws IllegalArgumentException if the conversion is invalid
     */
    T fromStorageFormat(S storageId);
    
    /**
     * Get the Java class type this converter handles.
     * 
     * @return the Java ID type class
     */
    Class<T> getJavaType();
    
    /**
     * Get the storage class type this converter produces.
     * 
     * @return the storage ID type class
     */
    Class<S> getStorageType();
    
    /**
     * Check if the given Java ID is valid for this converter.
     * 
     * @param javaId the Java ID to validate
     * @return true if the ID is valid for conversion
     */
    default boolean isValidJavaId(T javaId) {
        return javaId != null;
    }
    
    /**
     * Check if the given storage ID is valid for this converter.
     * 
     * @param storageId the storage ID to validate
     * @return true if the ID is valid for conversion
     */
    default boolean isValidStorageId(S storageId) {
        return storageId != null;
    }
    
    /**
     * Generate a new ID in Java format.
     * This is optional and may not be supported by all converters.
     * 
     * @return a new ID, or null if generation is not supported
     */
    default T generateId() {
        return null;
    }
}
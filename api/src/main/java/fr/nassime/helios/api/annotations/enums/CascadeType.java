package fr.nassime.helios.api.annotations.enums;

/**
 * Defines the types of cascade operations.
 */
public enum CascadeType {
    
    /**
     * Cascade all operations.
     */
    ALL,
    
    /**
     * Cascade persist operations.
     */
    PERSIST,
    
    /**
     * Cascade merge operations.
     */
    MERGE,
    
    /**
     * Cascade remove operations.
     */
    REMOVE,
    
    /**
     * Cascade refresh operations.
     */
    REFRESH,
    
    /**
     * Cascade detach operations.
     */
    DETACH
}
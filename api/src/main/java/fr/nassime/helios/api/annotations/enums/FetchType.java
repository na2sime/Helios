package fr.nassime.helios.api.annotations.enums;

/**
 * Defines strategies for fetching related entities.
 */
public enum FetchType {
    
    /**
     * Fetch eagerly (immediately when the parent entity is loaded).
     */
    EAGER,
    
    /**
     * Fetch lazily (only when accessed).
     */
    LAZY
}
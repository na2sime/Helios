package fr.nassime.helios.api.annotations.enums;

/**
 * Enumeration of relationship types between entities.
 */
public enum RelationType {
    /**
     * Many entities of this type can be related to one entity of the target type.
     */
    MANY_TO_ONE,
    
    /**
     * One entity of this type can be related to many entities of the target type.
     */
    ONE_TO_MANY,
    
    /**
     * One entity of this type can be related to one entity of the target type.
     */
    ONE_TO_ONE,
    
    /**
     * Many entities of this type can be related to many entities of the target type.
     */
    MANY_TO_MANY
}
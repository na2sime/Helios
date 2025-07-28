package fr.nassime.helios.api.annotations.enums;

/**
 * Defines the types of primary key generation strategies.
 */
public enum GenerationType {
    
    /**
     * Auto-select strategy based on the database provider.
     */
    AUTO,
    
    /**
     * Use an identity column (PostgreSQL SERIAL, MongoDB ObjectId).
     */
    IDENTITY,
    
    /**
     * Use a database sequence.
     */
    SEQUENCE,
    
    /**
     * Use a table to generate unique values.
     */
    TABLE,
    
    /**
     * Application is responsible for assigning primary keys.
     */
    ASSIGNED,
    
    /**
     * Use UUID generation.
     */
    UUID
}
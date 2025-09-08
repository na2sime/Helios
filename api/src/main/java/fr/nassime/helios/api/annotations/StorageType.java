package fr.nassime.helios.api.annotations;

/**
 * Defines the storage type for hybrid entity fields.
 */
public enum StorageType {
    /**
     * Store in relational database (PostgreSQL, MariaDB, etc.).
     * Best for structured data, ACID transactions, complex queries.
     */
    RELATIONAL,
    
    /**
     * Store in document database (MongoDB, etc.).
     * Best for flexible schema, nested data, rapid iteration.
     */
    DOCUMENT,
    
    /**
     * Store in both databases for redundancy and performance.
     * Requires careful synchronization strategy.
     */
    BOTH
}
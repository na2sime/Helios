package fr.nassime.helios.api.annotations.enums;

/**
 * Enumeration defining the persistence strategy for entities.
 * This determines which storage backend should be used.
 */
public enum PersistenceType {

    /**
     * Automatically determine the persistence type based on available providers
     * and configuration. This is the default and recommended approach.
     */
    AUTO,

    /**
     * Force SQL-based persistence (PostgreSQL, MariaDB, etc.).
     * Suitable for relational data with complex relationships and ACID requirements.
     */
    SQL,

    /**
     * Force document-based persistence (MongoDB, etc.).
     * Suitable for semi-structured data and horizontal scaling requirements.
     */
    DOCUMENT
}
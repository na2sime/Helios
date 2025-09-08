package fr.nassime.helios.api.annotations.enums;

/**
 * Defines how data is synchronized between relational and document stores in hybrid entities.
 */
public enum SynchronizationStrategy {
    /**
     * Document store contains a foreign key reference to the relational entity.
     * Relational data is the source of truth, document data is linked.
     * 
     * MongoDB document: { "_id": ObjectId, "user_id": 123, "messages": [...] }
     * PostgreSQL row: { "id": 123, "email": "user@example.com" }
     */
    LINKED,
    
    /**
     * Relational entity ID is embedded within the document.
     * Both stores maintain their own identity but share a common identifier.
     * 
     * MongoDB document: { "_id": ObjectId, "relational_id": 123, "data": {...} }
     * PostgreSQL row: { "id": 123, "structured_data": "..." }
     */
    EMBEDDED,
    
    /**
     * Both stores maintain synchronized copies of shared data.
     * Changes in one store are automatically propagated to the other.
     * Provides high availability but requires conflict resolution.
     */
    UNIFIED,
    
    /**
     * No automatic synchronization. Manual sync required.
     * Provides maximum control but requires explicit management.
     */
    MANUAL
}
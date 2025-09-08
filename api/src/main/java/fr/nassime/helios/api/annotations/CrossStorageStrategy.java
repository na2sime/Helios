package fr.nassime.helios.api.annotations;

/**
 * Strategy for managing relationships across different storage types.
 */
public enum CrossStorageStrategy {
    
    /**
     * Store only the ID reference in the source storage.
     * Target entities are loaded on-demand via separate queries.
     * 
     * Best for: Large collections, infrequent access
     * Storage overhead: Minimal (only IDs)
     * Query performance: Slower (multiple queries)
     * 
     * Example: User.messages -> only stores message IDs in user document
     */
    REFERENCE,
    
    /**
     * Embed complete target entities within the source entity.
     * All related data is stored together for fast access.
     * 
     * Best for: Small collections, frequent access, denormalization
     * Storage overhead: High (full object duplication) 
     * Query performance: Fastest (single query)
     * 
     * Example: Order.items -> full item objects stored in order document
     */
    EMBED,
    
    /**
     * Maintain synchronized copies in both storages.
     * Provides redundancy and optimal performance for both access patterns.
     * 
     * Best for: Critical data, mixed access patterns
     * Storage overhead: Highest (full duplication)
     * Query performance: Optimal (native queries in both stores)
     * 
     * Example: User.profile -> exists in both SQL and MongoDB
     */
    DUPLICATE,
    
    /**
     * Dynamically choose strategy based on data size and access patterns.
     * Small collections are embedded, large ones use references.
     * 
     * Best for: Unknown access patterns, adaptive systems
     * Storage overhead: Variable
     * Query performance: Adaptive
     * 
     * Example: Smart embedding for < 10 items, referencing for more
     */
    ADAPTIVE,
    
    /**
     * Create a denormalized view optimized for queries.
     * Maintains a query-optimized representation alongside the canonical data.
     * 
     * Best for: Complex analytics, reporting, search
     * Storage overhead: Medium (optimized views)
     * Query performance: Excellent for specific queries
     * 
     * Example: User.orderHistory -> optimized summary for dashboards
     */
    MATERIALIZED_VIEW
}
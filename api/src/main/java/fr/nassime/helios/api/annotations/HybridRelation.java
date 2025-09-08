package fr.nassime.helios.api.annotations;

import fr.nassime.helios.api.annotations.enums.FetchType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enhanced relation annotation for hybrid entities.
 * Combines traditional JPA relationships with hybrid storage capabilities.
 * 
 * This annotation can be used alongside @OneToMany, @ManyToOne, etc.
 * to provide hybrid-specific behavior.
 * 
 * Example:
 * <pre>
 * {@code
 * @OneToMany(mappedBy = "user")
 * @HybridRelation(
 *     storage = StorageType.DOCUMENT,
 *     strategy = CrossStorageStrategy.EMBED,
 *     partitionBy = "created_date"
 * )
 * private List<UserActivity> activities;
 * }
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HybridRelation {
    
    /**
     * Primary storage type for this relation.
     */
    StorageType storage() default StorageType.RELATIONAL;
    
    /**
     * Cross-storage strategy when relation spans multiple storage types.
     */
    CrossStorageStrategy strategy() default CrossStorageStrategy.REFERENCE;
    
    /**
     * Whether to synchronize this relation across hybrid storages.
     */
    boolean sync() default true;
    
    /**
     * Fetch strategy optimized for hybrid environments.
     */
    FetchType fetch() default FetchType.LAZY;
    
    /**
     * Partition field for large collections (useful for time-series data).
     * Enables efficient querying and archival of large datasets.
     */
    String partitionBy() default "";
    
    /**
     * Maximum number of items to load in a single batch.
     * Helps prevent memory issues with large collections.
     */
    int batchSize() default 100;
    
    /**
     * Whether to maintain a materialized view for complex queries.
     */
    boolean materialized() default false;
    
    /**
     * Custom query hint for relation loading optimization.
     * Can contain storage-specific optimizations.
     */
    String queryHint() default "";
}
package fr.nassime.helios.api.annotations;

import fr.nassime.helios.api.annotations.enums.FetchType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a relationship that spans across different storage types.
 * This revolutionary annotation enables relations between SQL and NoSQL entities.
 * 
 * Example:
 * <pre>
 * {@code
 * // User in PostgreSQL, Messages in MongoDB
 * @OneToMany
 * @CrossStorageRelation(
 *     targetStorage = StorageType.DOCUMENT,
 *     foreignKey = "user_id",
 *     strategy = CrossStorageStrategy.REFERENCE
 * )
 * private List<Message> messages;
 * }
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CrossStorageRelation {
    
    /**
     * Storage type where the target entity is stored.
     * If not specified, will be auto-detected from target entity annotations.
     */
    StorageType targetStorage() default StorageType.RELATIONAL;
    
    /**
     * Storage type where the source entity is stored.
     * Usually auto-detected from the declaring entity.
     */
    StorageType sourceStorage() default StorageType.RELATIONAL;
    
    /**
     * Strategy for managing the cross-storage relationship.
     */
    CrossStorageStrategy strategy() default CrossStorageStrategy.REFERENCE;
    
    /**
     * Foreign key field name in the target storage.
     * For MongoDB, this becomes a field in the document.
     * For SQL, this is the foreign key column.
     */
    String foreignKey() default "";
    
    /**
     * Whether to maintain referential integrity across storages.
     * When enabled, deleting source entity will cascade to target.
     */
    boolean maintainIntegrity() default true;
    
    /**
     * Fetch strategy for cross-storage loading.
     */
    FetchType fetch() default FetchType.LAZY;
    
    /**
     * Whether to cache the relationship data for performance.
     */
    boolean cached() default false;
    
    /**
     * Maximum cache TTL in seconds (if cached = true).
     */
    int cacheTtl() default 300; // 5 minutes
}
package fr.nassime.helios.api.annotations;

import fr.nassime.helios.api.annotations.enums.SynchronizationStrategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a hybrid entity that can store data across both relational and document databases.
 * This annotation extends @Persistable by adding hybrid-specific configuration.
 * 
 * Usage: Always use @Persistable(type = PersistenceType.HYBRID) along with @HybridEntity
 * 
 * Example:
 * <pre>
 * {@code
 * @Persistable(name = "users", type = PersistenceType.HYBRID)
 * @HybridEntity(
 *     relationalTable = "users",
 *     documentCollection = "user_profiles", 
 *     strategy = SynchronizationStrategy.LINKED
 * )
 * public class User {
 *     @Id private Long id;
 *     @HybridField(storage = StorageType.RELATIONAL) private String email;
 *     @HybridField(storage = StorageType.DOCUMENT) private List<Message> messages;
 * }
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface HybridEntity {
    
    /**
     * Relational database table name.
     * Should match @Persistable.name() for consistency.
     */
    String relationalTable();
    
    /**
     * Document database collection name.
     */
    String documentCollection();
    
    /**
     * Schema for relational table.
     */
    String relationalSchema() default "";
    
    /**
     * Database name for document collection.
     */
    String documentDatabase() default "";
    
    /**
     * Strategy for synchronizing data between relational and document stores.
     */
    SynchronizationStrategy strategy() default SynchronizationStrategy.LINKED;
    
    /**
     * Whether to enable automatic synchronization between stores.
     */
    boolean autoSync() default true;
    
    /**
     * Primary storage for the entity ID.
     */
    StorageType primaryStorage() default StorageType.RELATIONAL;
}
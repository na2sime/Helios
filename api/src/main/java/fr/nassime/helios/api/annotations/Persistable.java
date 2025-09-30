package fr.nassime.helios.api.annotations;

import fr.nassime.helios.api.annotations.enums.PersistenceType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Unified annotation for marking classes as persistable entities across different storage backends.
 * This annotation replaces the specific @Entity and @Document annotations, providing a common
 * interface for both SQL and NoSQL databases.
 *
 * <p>Usage examples:</p>
 * <pre>
 * // Simple entity with auto-detection
 * {@code @Persistable(name = "users")}
 * public class User { }
 *
 * // Entity with explicit type
 * {@code @Persistable(name = "users", type = PersistenceType.SQL)}
 * public class User { }
 *
 * // Multi-database configuration: route to specific database
 * {@code @Persistable(name = "users", database = "main-pg")}
 * public class User { }
 *
 * {@code @Persistable(name = "events", database = "analytics")}
 * public class AnalyticsEvent { }
 * </pre>
 *
 * @author Helios ORM Team
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Persistable {

    /**
     * The name of the table/collection to store entities.
     * If not specified, the class name will be used (converted to appropriate case).
     *
     * @return the table or collection name
     */
    String name() default "";

    /**
     * The type of persistence strategy to use.
     * AUTO will determine the appropriate strategy based on the available providers.
     *
     * @return the persistence type
     */
    PersistenceType type() default PersistenceType.AUTO;

    /**
     * Schema name for SQL databases. Ignored for NoSQL databases.
     * Note: Schema support may vary by database provider.
     *
     * @return the schema name
     */
    String schema() default "";

    /**
     * Database identifier for multi-database configurations.
     *
     * <p>When using {@link fr.nassime.helios.api.Helios#configure()}, specify which
     * configured database this entity should use:</p>
     * <pre>
     * Helios.configure()
     *     .postgres("main", ...)
     *     .mongo("analytics", ...)
     *     .build();
     *
     * {@code @Persistable(name = "users", database = "main")}
     * class User { }
     *
     * {@code @Persistable(name = "events", database = "analytics")}
     * class Event { }
     * </pre>
     *
     * <p>If not specified, the default database will be used.</p>
     * <p>For MongoDB single-database configurations, this can also specify the database name.</p>
     *
     * @return the database identifier
     */
    String database() default "";

    /**
     * Optional catalog name for SQL databases.
     *
     * @return the catalog name
     */
    String catalog() default "";

    /**
     * Whether to enable caching for this entity.
     *
     * @return true if caching should be enabled
     */
    boolean cacheable() default false;

    /**
     * Custom configuration properties as key-value pairs.
     * Useful for provider-specific settings.
     *
     * @return array of property strings in "key=value" format
     */
    String[] properties() default {};
}
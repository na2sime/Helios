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
 * @author Claude Code
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Persistable {
    
    /**
     * The name of the table/collection to store entities.
     * If not specified, the class name will be used (converted to appropriate case).
     */
    String name() default "";
    
    /**
     * The type of persistence strategy to use.
     * AUTO will determine the appropriate strategy based on the available providers.
     */
    PersistenceType type() default PersistenceType.AUTO;
    
    /**
     * Schema name for SQL databases. Ignored for NoSQL databases.
     */
    String schema() default "";
    
    /**
     * Database name for NoSQL databases like MongoDB. Ignored for SQL databases.
     */
    String database() default "";
    
    /**
     * Optional catalog name for SQL databases.
     */
    String catalog() default "";
    
    /**
     * Whether to enable caching for this entity.
     */
    boolean cacheable() default false;
    
    /**
     * Custom configuration properties as key-value pairs.
     * Useful for provider-specific settings.
     */
    String[] properties() default {};
}
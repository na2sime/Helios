package fr.nassime.helios.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies a foreign key column for entity relationships.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JoinColumn {
    
    /**
     * The name of the foreign key column.
     */
    String name() default "";
    
    /**
     * The name of the referenced column in the target table.
     */
    String referencedColumnName() default "";
    
    /**
     * Whether the foreign key column allows null values.
     */
    boolean nullable() default true;
    
    /**
     * Whether the foreign key column should be unique.
     */
    boolean unique() default false;
    
    /**
     * Whether this column should be included in inserts.
     */
    boolean insertable() default true;
    
    /**
     * Whether this column should be included in updates.
     */
    boolean updatable() default true;
}
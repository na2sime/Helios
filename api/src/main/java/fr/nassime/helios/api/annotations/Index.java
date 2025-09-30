package fr.nassime.helios.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to define database index on a field or class.
 * Can be used on fields to create single field indexes or on classes 
 * to create compound indexes.
 */
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Index {
    
    /**
     * The name of the index. If not specified, a default name will be generated.
     */
    String name() default "";
    
    /**
     * Fields to include in the index (for compound indexes on class level).
     * For single field indexes on field level, this is ignored.
     */
    String[] fields() default {};
    
    /**
     * Whether this is a unique index.
     */
    boolean unique() default false;
    
    /**
     * Whether this is a sparse index (only index documents that have the indexed field).
     */
    boolean sparse() default false;
    
    /**
     * Direction of the index (1 for ascending, -1 for descending).
     * For compound indexes, specify direction for each field.
     */
    int[] directions() default {1};
    
    /**
     * Background index creation (for performance).
     */
    boolean background() default true;
}
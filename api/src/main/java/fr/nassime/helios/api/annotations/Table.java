package fr.nassime.helios.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the primary table for the annotated entity.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Table {
    
    /**
     * The name of the table.
     */
    String name() default "";
    
    /**
     * The catalog of the table.
     */
    String catalog() default "";
    
    /**
     * The schema of the table.
     */
    String schema() default "";
}
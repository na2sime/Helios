package fr.nassime.helios.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as an entity that can be persisted to the database.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Entity {
    
    /**
     * The name of the entity. Defaults to the class name.
     */
    String name() default "";
    
    /**
     * The table/collection name. Defaults to the entity name.
     */
    String table() default "";
}
package fr.nassime.helios.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Maps a field to a MongoDB document field.
 * MongoDB equivalent of @Column for relational databases.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Field {
    
    /**
     * The name of the MongoDB field. 
     * Defaults to the Java field name.
     */
    String name() default "";
    
    /**
     * Whether this field should be indexed in MongoDB.
     */
    boolean indexed() default false;
    
    /**
     * Whether this field value should be unique.
     */
    boolean unique() default false;
    
    /**
     * Whether this field is required (cannot be null).
     */
    boolean required() default false;
}
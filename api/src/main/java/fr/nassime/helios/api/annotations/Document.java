package fr.nassime.helios.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the MongoDB collection for the annotated entity.
 * MongoDB equivalent of @Table for relational databases.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Document {
    
    /**
     * The name of the MongoDB collection.
     * Defaults to the class name in lowercase.
     */
    String collection() default "";
    
    /**
     * The name of the MongoDB database.
     * If not specified, uses the default database from configuration.
     */
    String database() default "";
}
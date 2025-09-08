package fr.nassime.helios.api.annotations;

import fr.nassime.helios.api.annotations.enums.PersistenceType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Legacy annotation for MongoDB documents.
 * This annotation is deprecated in favor of @Persistable(type = PersistenceType.DOCUMENT).
 * 
 * @deprecated Use @Persistable(type = PersistenceType.DOCUMENT) instead.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Deprecated(since = "2.0.0", forRemoval = true)
public @interface Document {
    
    /**
     * The name of the MongoDB collection.
     * If not specified, the class name will be used.
     */
    String collection() default "";
    
    /**
     * The database name.
     */
    String database() default "";
}
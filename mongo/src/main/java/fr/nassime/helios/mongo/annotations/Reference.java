package fr.nassime.helios.mongo.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MongoDB-specific annotation to mark fields that contain references to other documents.
 * This is used for storing foreign keys that reference other collections.
 * 
 * Example usage:
 * <pre>
 * &#64;Reference
 * &#64;Field(name = "department_id")
 * private String departmentId; // Reference to Department document
 * 
 * &#64;ManyToOne
 * private Department department; // Will be loaded using departmentId
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Reference {
    
    /**
     * The target collection name.
     * If not specified, inferred from the field name or relation annotation.
     */
    String collection() default "";
    
    /**
     * Whether this reference is required.
     * If true, saves will fail if the reference is null.
     */
    boolean required() default false;
}
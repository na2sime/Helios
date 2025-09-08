package fr.nassime.helios.api.annotations;

import fr.nassime.helios.api.annotations.enums.FetchType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as hybrid, stored in a specific storage type within a HybridEntity.
 * This allows fine-grained control over where each field is persisted.
 * 
 * Example:
 * <pre>
 * {@code
 * @HybridField(storage = StorageType.DOCUMENT, fetch = FetchType.LAZY)
 * private List<Message> messages;
 * 
 * @HybridField(storage = StorageType.RELATIONAL)
 * private String email;
 * }
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HybridField {
    
    /**
     * Storage type for this field.
     */
    StorageType storage();
    
    /**
     * Fetch strategy for this field.
     */
    FetchType fetch() default FetchType.EAGER;
    
    /**
     * Field name in the target storage.
     * If empty, uses the field name.
     */
    String name() default "";
    
    /**
     * Whether this field should be included in synchronization operations.
     */
    boolean sync() default true;
    
    /**
     * Whether this field is required for entity validity.
     */
    boolean required() default false;
}
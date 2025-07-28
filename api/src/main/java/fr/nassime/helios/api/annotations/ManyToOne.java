package fr.nassime.helios.api.annotations;

import fr.nassime.helios.api.annotations.enums.CascadeType;
import fr.nassime.helios.api.annotations.enums.FetchType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a many-to-one relationship between entities.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ManyToOne {
    
    /**
     * The target entity class.
     */
    Class<?> targetEntity() default void.class;
    
    /**
     * The cascade operations to apply.
     */
    CascadeType[] cascade() default {};
    
    /**
     * The fetch strategy.
     */
    FetchType fetch() default FetchType.EAGER;
    
    /**
     * Whether the relationship is optional.
     */
    boolean optional() default true;
}
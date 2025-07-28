package fr.nassime.helios.api.annotations;

import fr.nassime.helios.api.annotations.enums.GenerationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as the primary key of an entity.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Id {
    
    /**
     * The generation strategy for the ID.
     */
    GenerationType strategy() default GenerationType.AUTO;
    
    /**
     * The name of the generator (for SEQUENCE or TABLE strategy).
     */
    String generator() default "";
}
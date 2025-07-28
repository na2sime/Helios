package fr.nassime.helios.api.annotations;

import fr.nassime.helios.api.annotations.enums.GenerationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides for the specification of generation strategies for the values of primary keys.
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface GeneratedValue {
    
    /**
     * The primary key generation strategy that the persistence provider must use to generate the annotated entity primary key.
     */
    GenerationType strategy() default GenerationType.AUTO;
    
    /**
     * The name of the primary key generator to use as specified in the generator element.
     */
    String generator() default "";
}
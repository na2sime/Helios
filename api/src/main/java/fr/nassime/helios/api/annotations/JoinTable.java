package fr.nassime.helios.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the mapping for many-to-many relationships via a join table.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JoinTable {
    
    /**
     * The name of the join table.
     */
    String name() default "";
    
    /**
     * The schema of the join table.
     */
    String schema() default "";
    
    /**
     * The catalog of the join table.
     */
    String catalog() default "";
    
    /**
     * The foreign key columns of the join table that reference the primary table.
     */
    JoinColumn[] joinColumns() default {};
    
    /**
     * The foreign key columns of the join table that reference the inverse table.
     */
    JoinColumn[] inverseJoinColumns() default {};
}
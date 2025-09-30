package fr.nassime.helios.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Maps a field to a database column.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {
    
    /**
     * The name of the column. Defaults to the field name.
     */
    String name() default "";
    
    /**
     * Whether the column allows null values.
     */
    boolean nullable() default true;
    
    /**
     * Whether the column value should be unique.
     */
    boolean unique() default false;
    
    /**
     * The length of the column (for string types).
     */
    int length() default 255;
    
    /**
     * The precision for decimal types.
     */
    int precision() default 0;
    
    /**
     * The scale for decimal types.
     */
    int scale() default 0;
    
    /**
     * Whether this column should be included in inserts.
     */
    boolean insertable() default true;
    
    /**
     * Whether this column should be included in updates.
     */
    boolean updatable() default true;
}
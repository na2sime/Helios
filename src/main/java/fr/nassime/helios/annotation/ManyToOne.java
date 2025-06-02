package fr.nassime.helios.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface ManyToOne {
    String joinColumn() default "";

    Relation.FetchType fetch() default Relation.FetchType.EAGER;

    boolean cascade() default false;
}


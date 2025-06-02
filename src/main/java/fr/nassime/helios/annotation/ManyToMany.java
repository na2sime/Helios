package fr.nassime.helios.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface ManyToMany {
    Class<?> targetEntity();

    String joinTable();

    String joinColumn();

    String inverseJoinColumn();

    Relation.FetchType fetch() default Relation.FetchType.LAZY;

    boolean cascade() default false;
}


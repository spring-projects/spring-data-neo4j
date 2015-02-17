package org.springframework.data.neo4j.annotation;

import org.springframework.data.annotation.QueryAnnotation;

import java.lang.annotation.*;

/**
 * Annotation to declare finder queries directly on repository methods.
 *
 * @author Mark Angrish
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@QueryAnnotation
@Documented
public @interface Query {

    static final String CLASS = "org.springframework.data.neo4j.annotation.Query";
    static final String VALUE = "value";

    /**
     * Defines the Cypher query to be executed when the annotated method is called.
     */
    String value() default "";
}

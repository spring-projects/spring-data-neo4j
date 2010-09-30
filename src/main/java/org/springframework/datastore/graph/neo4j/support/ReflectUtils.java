package org.springframework.datastore.graph.neo4j.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;

/**
 * @author Michael Hunger
 * @since 30.09.2010
 */
public class ReflectUtils {
    private ReflectUtils() {
    }

    public static <A extends Annotation> A getAnnotation(final AnnotatedElement target, final Class<A> annotationClass) {
        return target.isAnnotationPresent(annotationClass) ? target.getAnnotation(annotationClass) : null;
    }
}

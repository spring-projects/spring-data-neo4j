package org.springframework.data.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;

/**
 * Annotated fields and entities will be indexed and available for retrieval using an indexing API.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.FIELD, ElementType.TYPE})
public @interface Indexed {
    /**
     * Name of the index to use.
     */
    String indexName() default "";

    boolean fulltext() default false;

    String fieldName() default "";

    static class Name {
        public static String getDefault(Field field) {
            return get(field.getDeclaringClass());
        }

        public static String get(Class<?> type) {
            return getIndexName(type, type.getSimpleName());
        }

        private static String getIndexName(AnnotatedElement element, String defaultIndexName) {
            Indexed indexed = element.getAnnotation(Indexed.class);
            if (indexed == null || indexed.indexName() == null || indexed.indexName().isEmpty()) {
                return defaultIndexName;
            }
            return indexed.indexName();
        }

        public static String get(Field field) {
            return getIndexName(field, getDefault(field));
        }
    }
}

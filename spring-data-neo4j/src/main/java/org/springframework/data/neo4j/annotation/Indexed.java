/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.neo4j.annotation;

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
@Target(value = {ElementType.FIELD, ElementType.TYPE,ElementType.METHOD})
public @interface Indexed {
    /**
     * Name of the index to use.
     */
    String indexName() default "";

    org.springframework.data.neo4j.support.index.IndexType indexType() default org.springframework.data.neo4j.support.index.IndexType.LABEL;

    String fieldName() default "";

    /**
     * Indicates whether to apply a unique constraint on this property, defaults to false.
     */
    boolean unique() default false;

    boolean numeric() default false;

    /**
     * Only applicable when indexType is LABEL and unique=true, indicates how to handle attempts to save
     * entities where this unique property already exists, defaults to false. When set to false, default entity
     * saving behaviour resorts to merge type behaviour whilst when set to true, results in an exception being
     * thrown when attempting to save another entity where the same unique indexed property already exists under
     * a different node id.
     */
    boolean failOnDuplicate() default false;

    // FQN is a fix for javac compiler bug http://bugs.sun.com/view_bug.do?bug_id=6512707
    org.springframework.data.neo4j.annotation.Indexed.Level level() default org.springframework.data.neo4j.annotation.Indexed.Level.CLASS;

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

        public static String get(Level level, Class<?> type, String providedIndexName, Class<?> instanceType) {
            if (providedIndexName!=null) return providedIndexName;
            switch (level) {
                case GLOBAL: return "nodes";
                case CLASS: return get(type);
                case INSTANCE: return get(instanceType);
                default : return get(type);
            }
        }
    }

    enum Level { @Deprecated GLOBAL, CLASS, INSTANCE}
}

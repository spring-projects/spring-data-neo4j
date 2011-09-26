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

package org.springframework.data.neo4j.mapping;

import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.neo4j.annotation.Indexed;

import javax.persistence.Id;
import java.lang.annotation.Annotation;
import java.util.Collection;

/**
 * Interface for Neo4J specific {@link PersistentProperty}s. Declares additional metadata to lookup relationship
 * information.
 * 
 * @author Oliver Gierke
 */
public interface Neo4JPersistentProperty extends PersistentProperty<Neo4JPersistentProperty> {

    /**
     * Returns whether the property represents a relationship. If this returns {@literal true}, clients can expect
     * {@link #getRelationshipInfo()} to return a non-{@literal null} value.
     * 
     * @return
     */
    boolean isRelationship();

    /**
     * Returns the {@link RelationshipInfo} for the given property if it is a relationship or {@literal null} otherwise.
     * 
     * @see #isRelationship()
     * @return
     */
    RelationshipInfo getRelationshipInfo();

    boolean isIndexed();

    Neo4JPersistentPropertyImpl.IndexInfo getIndexInfo();

    String getNeo4jPropertyName();

    boolean isSimpleValueField();

    boolean isSerializableField(final ConversionService conversionService);

    boolean isDeserializableField(final ConversionService conversionService);

    boolean isNeo4jPropertyType();

    boolean isSyntheticField();

    Collection<? extends Annotation> getAnnotations();

    <T extends Annotation> T getAnnotation(Class<T> annotationType);

    <T extends Annotation> boolean isAnnotationPresent(Class<T> annotationType);

    void setValue(Object entity, Object newValue) throws IllegalAccessException;

    Object getValue(final Object entity) throws IllegalAccessException;
}

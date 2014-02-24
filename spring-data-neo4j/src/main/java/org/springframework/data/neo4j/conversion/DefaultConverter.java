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

package org.springframework.data.neo4j.conversion;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.springframework.data.neo4j.mapping.MappingPolicy;
import org.springframework.data.neo4j.support.path.NodePath;
import org.springframework.data.neo4j.support.path.RelationshipPath;

import java.util.Iterator;
import java.util.Map;

/**
 * @author mh
 * @since 28.06.11
 */
public class DefaultConverter<T,R> implements ResultConverter<T,R> {
    @SuppressWarnings("unchecked")
    public R convert(Object value, Class type) {
        return convert(value,type, null);
    }
    public R convert(Object value, Class type, MappingPolicy mappingPolicy) {
        if (type == null) return (R) value;
        if (value == null || type.isInstance(value)) return (R) value;
        Object singleValue = extractValue(value);
        if (singleValue == null || type.isInstance(singleValue)) return (R) singleValue;
        final Class<?> sourceType = singleValue.getClass();
        Object result = doConvert(singleValue, sourceType, type,mappingPolicy);
        if (result == null)
            throw new RuntimeException("Cannot automatically convert " + sourceType + " to " + type + " please use a custom converter");
        return (R) result;
    }

    protected Object extractValue(Object value) {
        if (value instanceof Path) return value; // todo is this sensible to do?
        if (value instanceof Map) return extractSingle(((Map)value).values());
        if (value instanceof Iterable) return extractSingle((Iterable)value);
        return value;
    }

    private Object extractSingle(Iterable values) {
        final Iterator it = values.iterator();
        if (!it.hasNext()) throw new RuntimeException("Cannot extract single value from empty Iterable.");
        final Object result = it.next();
        if (it.hasNext()) throw new RuntimeException("Cannot extract single value from Iterable with more than one elements.");
        return result;
    }

    @SuppressWarnings("unchecked")
    protected Object doConvert(Object value, Class<?> sourceType, Class type, MappingPolicy mappingPolicy) {
        if (Node.class.isAssignableFrom(type)) {
            return toNode(value, sourceType);
        }
        if (Relationship.class.isAssignableFrom(type)) {
            return toRelationship(value, sourceType);
        }
        if (Path.class.isAssignableFrom(type)) {
            return toPath(value, sourceType);
        }
        if (type.isEnum()) {
            return Enum.valueOf(type, value.toString());
        }
        return null;
    }

    public Path toPath(Object value, Class<?> sourceType) {
        if (Node.class.isAssignableFrom(sourceType)) return new NodePath((Node) value);
        if (Relationship.class.isAssignableFrom(sourceType)) return new RelationshipPath((Relationship) value);
        if (Path.class.isAssignableFrom(sourceType)) return (Path) value;
        return null;
    }

    public Relationship toRelationship(Object value, Class<?> sourceType) {
        if (Relationship.class.isAssignableFrom(sourceType)) return ((Relationship) value);
        if (Path.class.isAssignableFrom(sourceType)) return ((Path) value).lastRelationship();
        if (Node.class.isAssignableFrom(sourceType)) return ((Node) value).getRelationships().iterator().next();
        return null;
    }

    public Node toNode(Object value, Class<?> sourceType) {
        if (Node.class.isAssignableFrom(sourceType)) return (Node)value;
        if (Path.class.isAssignableFrom(sourceType)) return ((Path) value).endNode();
        if (Relationship.class.isAssignableFrom(sourceType)) return ((Relationship) value).getEndNode();
        return null;
    }
}

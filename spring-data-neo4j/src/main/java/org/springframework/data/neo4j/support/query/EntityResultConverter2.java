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

package org.springframework.data.neo4j.support.query;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.neo4j.core.TypeRepresentationStrategy;
import org.springframework.data.neo4j.conversion.ResultConverter;
import org.springframework.data.neo4j.support.GraphDatabaseContext;

/**
 * @author mh
 * @since 28.06.11
 */
public class EntityResultConverter2<R> implements ResultConverter<Object,R> {
    private final TypeRepresentationStrategy nodeTypeRepresentationStrategy;
    private final TypeRepresentationStrategy relationshipTypeRepresentationStrategy;
    private final ConversionService conversionService;

    public EntityResultConverter2(GraphDatabaseContext ctx) {
        this.conversionService = ctx.getConversionService();
        this.nodeTypeRepresentationStrategy = ctx.getNodeTypeRepresentationStrategy();
        relationshipTypeRepresentationStrategy = ctx.getRelationshipTypeRepresentationStrategy();
    }

    public R convert(Object value, Class<R> type) {
        if (type == null) return (R) convertValue(value);
        if (type.isInstance(value)) return type.cast(value);
        if (value instanceof Node) {
            return (R) nodeTypeRepresentationStrategy.createEntity((Node) value, type);
        }
        if (value instanceof Relationship) {
            return (R) relationshipTypeRepresentationStrategy.createEntity((Relationship) value, type);
        }
        return conversionService.convert(value, type);
    }

    private Object convertValue(Object value) {
        if (value instanceof Node) {
            return nodeTypeRepresentationStrategy.createEntity((Node) value);
        }
        if (value instanceof Relationship) {
            return relationshipTypeRepresentationStrategy.createEntity((Relationship) value);
        }
        return value;
    }
}

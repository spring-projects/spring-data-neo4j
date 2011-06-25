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

package org.springframework.data.graph.neo4j.support.query;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.graph.core.TypeRepresentationStrategy;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;

import java.util.Map;

/**
 * @author mh
 * @since 10.06.11
 *        todo limits
 */
public class QueryExecutor implements QueryResultConverter {
    private final TypeRepresentationStrategy nodeTypeRepresentationStrategy;
    private final TypeRepresentationStrategy relationshipTypeRepresentationStrategy;
    private final ConversionService conversionService;
    private final QueryEngine queryEngine;

    public QueryExecutor(GraphDatabaseContext ctx) {
        this.nodeTypeRepresentationStrategy = ctx.getNodeTypeRepresentationStrategy();
        relationshipTypeRepresentationStrategy = ctx.getRelationshipTypeRepresentationStrategy();
        conversionService = ctx.getConversionService();
        queryEngine = new EmbeddedQueryEngine(ctx.getGraphDatabaseService(), this);
    }

    public Iterable<Map<String, Object>> query(String statement) {
        return queryEngine.query(statement);
    }

    public <T> Iterable<T> query(String statement, Class<T> type) {
        return queryEngine.query(statement, type);
    }

    public <T> T queryForObject(String statement, Class<T> type) {
        return queryEngine.queryForObject(statement,type);
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

    public <T> T convertValue(Object value, Class<T> type) {
        if (type == null) return (T) convertValue(value);
        if (type.isInstance(value)) return type.cast(value);
        if (value instanceof Node) {
            return (T) nodeTypeRepresentationStrategy.createEntity((Node) value, type);
        }
        if (value instanceof Relationship) {
            return (T) relationshipTypeRepresentationStrategy.createEntity((Relationship) value, type);
        }
        return conversionService.convert(value, type);
    }
}
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

import org.neo4j.cypher.SyntaxError;
import org.neo4j.cypher.commands.Query;
import org.neo4j.cypher.javacompat.CypherParser;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.collection.IterableWrapper;
import org.springframework.core.convert.ConversionService;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.data.graph.core.TypeRepresentationStrategy;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author mh
 * @since 10.06.11
 *        todo limits
 */
public class QueryExecutor {
    private final TypeRepresentationStrategy nodeTypeRepresentationStrategy;
    private final ExecutionEngine executionEngine;
    private final TypeRepresentationStrategy relationshipTypeRepresentationStrategy;
    private final ConversionService conversionService;

    public QueryExecutor(GraphDatabaseContext ctx) {
        this.nodeTypeRepresentationStrategy = ctx.getNodeTypeRepresentationStrategy();
        relationshipTypeRepresentationStrategy = ctx.getRelationshipTypeRepresentationStrategy();
        conversionService = ctx.getConversionService();
        this.executionEngine = new ExecutionEngine(ctx.getGraphDatabaseService());
    }

    public Iterable<Map<String, Object>> query(String statement) {
        try {
            ExecutionResult result = parseAndExecuteQuery(statement);
            return convertResult(result);
        } catch (Exception e) {
            throw new InvalidDataAccessResourceUsageException("Error executing statement " + statement, e);
        }
    }

    public <T> Iterable<T> query(String statement, Class<T> type) {
        try {
            ExecutionResult result = parseAndExecuteQuery(statement);
            return convertResult(result, type);
        } catch (Exception e) {
            throw new InvalidDataAccessResourceUsageException("Error executing statement " + statement + " for type " + type, e);
        }
    }

    public <T> T queryForObject(String statement, Class<T> type) {
        try {
            ExecutionResult result = parseAndExecuteQuery(statement);
            final Iterable<T> convertedResult = convertResult(result, type);
            return extractSingleResult(convertedResult);
        } catch (Exception e) {
            throw new InvalidDataAccessResourceUsageException("Error executing statement " + statement + " for type " + type, e);
        }
    }

    private <T> T extractSingleResult(Iterable<T> convertedResult) {
        final Iterator<T> it = convertedResult.iterator();
        if (!it.hasNext()) throw new InvalidDataAccessResourceUsageException("Expected single result, got none");
        T value = it.hasNext() ? it.next() : null;
        if (it.hasNext())
            throw new InvalidDataAccessResourceUsageException("Expected single result, got more than one");
        return value;
    }

    private <T> Iterable<T> convertResult(ExecutionResult result, final Class<T> type) {
        final List<String> columns = result.columns();
        if (columns.size() != 1)
            throw new InvalidDataAccessResourceUsageException("Expected single column of results, got " + columns);
        final String column = columns.get(0);
        return new IterableWrapper<T, Map<String, Object>>(result) {
            @Override
            protected T underlyingObjectToObject(Map<String, Object> row) {
                return convertValue(row.get(column), type);
            }
        };
    }

    private Iterable<Map<String, Object>> convertResult(Iterable<Map<String, Object>> result) {
        return new IterableWrapper<Map<String, Object>, Map<String, Object>>(result) {
            @Override
            protected Map<String, Object> underlyingObjectToObject(Map<String, Object> row) {
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    Object value = convertValue(entry.getValue());
                    if (value != entry.getValue()) {
                        entry.setValue(value);
                    }
                }
                return row;
            }
        };
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
    private <T> T convertValue(Object value,Class<T> type) {
        if (value instanceof Node) {
            return (T) nodeTypeRepresentationStrategy.createEntity((Node) value,type);
        }
        if (value instanceof Relationship) {
            return (T) relationshipTypeRepresentationStrategy.createEntity((Relationship) value,type);
        }
        return conversionService.convert(value,type);
    }

    private ExecutionResult parseAndExecuteQuery(String statement) {
        try {
            CypherParser parser = new CypherParser();
            Query query = parser.parse(statement);
            return executionEngine.execute(query);
        } catch (SyntaxError syntaxError) {
            throw new InvalidDataAccessResourceUsageException("Error executing statement " + statement, syntaxError);
        }
    }
}
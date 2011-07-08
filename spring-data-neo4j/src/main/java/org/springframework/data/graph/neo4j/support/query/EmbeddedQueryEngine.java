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

import org.neo4j.cypher.commands.Query;
import org.neo4j.cypher.javacompat.CypherParser;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.helpers.collection.IterableWrapper;
import org.springframework.dao.InvalidDataAccessResourceUsageException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class EmbeddedQueryEngine implements QueryEngine {

    public EmbeddedQueryEngine(GraphDatabaseService graphDatabaseService) {
        this(graphDatabaseService, QueryResultConverter.NO_OP_QUERY_RESULT_CONVERTER);
    }

    final ExecutionEngine executionEngine;
    private QueryResultConverter resultConverter;

    public EmbeddedQueryEngine(GraphDatabaseService graphDatabaseService, QueryResultConverter resultConverter) {
        this.resultConverter = resultConverter != null ? resultConverter : QueryResultConverter.NO_OP_QUERY_RESULT_CONVERTER;
        this.executionEngine = new ExecutionEngine(graphDatabaseService);
    }

    @Override
    public Iterable<Map<String, Object>> query(String statement) {
        try {
            ExecutionResult result = parseAndExecuteQuery(statement);
            return convertResult(result);
        } catch (Exception e) {
            throw new InvalidDataAccessResourceUsageException("Error executing statement " + statement, e);
        }
    }

    @Override
    public <T> Iterable<T> query(String statement, Class<T> type) {
        try {
            ExecutionResult result = parseAndExecuteQuery(statement);
            return convertResult(result, type);
        } catch (Exception e) {
            throw new InvalidDataAccessResourceUsageException("Error executing statement " + statement + " for type " + type, e);
        }
    }

    @Override
    public <T> T queryForObject(String statement, Class<T> type) {
        try {
            ExecutionResult result = parseAndExecuteQuery(statement);
            final Iterable<T> convertedResult = convertResult(result, type);
            return extractSingleResult(convertedResult);
        } catch (Exception e) {
            throw new InvalidDataAccessResourceUsageException("Error executing statement " + statement + " for type " + type, e);
        }
    }

    private ExecutionResult parseAndExecuteQuery(String statement) {
        try {
            CypherParser parser = new CypherParser();
            Query query = parser.parse(statement);
            return executionEngine.execute(query);
        } catch(Exception e) {
            throw new InvalidDataAccessResourceUsageException("Error executing statement " + statement, e);
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
                return resultConverter.convertValue(row.get(column), type);
            }
        };
    }

    private Iterable<Map<String, Object>> convertResult(Iterable<Map<String, Object>> result) {
        return new IterableWrapper<Map<String, Object>, Map<String, Object>>(result) {
            @Override
            protected Map<String, Object> underlyingObjectToObject(Map<String, Object> row) {
                Map<String,Object> newRow=new HashMap<String,Object>(row); // todo performance
                for (Map.Entry<String, Object> entry : newRow.entrySet()) {
                    Object value = resultConverter.convertValue(entry.getValue(),null);
                    if (value != entry.getValue()) {
                        entry.setValue(value);
                    }
                }
                return row;
            }
        };
    }

}
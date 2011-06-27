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

package org.springframework.data.graph.neo4j.rest.support;

import com.sun.jersey.api.client.ClientResponse;
import org.neo4j.helpers.collection.IterableWrapper;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.data.graph.neo4j.support.query.QueryEngine;
import org.springframework.data.graph.neo4j.support.query.QueryResultConverter;

import java.util.*;
import org.springframework.data.graph.neo4j.support.query.QueryEngine;

import java.util.Map;

/**
 * @author mh
 * @since 22.06.11
 */
public class RestQueryEngine implements QueryEngine {
    private final RestRequest restRequest;
    private final RestGraphDatabase restGraphDatabase;
    private final QueryResultConverter resultConverter;

    public RestQueryEngine(RestGraphDatabase restGraphDatabase, QueryResultConverter resultConverter) {
        this.restGraphDatabase = restGraphDatabase;
        this.resultConverter = resultConverter;
        this.restRequest = restGraphDatabase.getRestRequest();
    }

    @Override
    public Iterable<Map<String, Object>> query(String statement) {
        return executeStatement(statement).getData();
    }

    private RestQueryResult executeStatement(String statement) {
        final ClientResponse response = restRequest.get("ext/CypherPlugin/graphdb/execute_query", JsonHelper.createJsonFrom(Collections.singletonMap("query", statement)));
        return new RestQueryResult(restRequest.toMap(response));
    }

    class RestQueryResult {
        List<String> columns;
        List<Map<String,Object>> data;

        public RestQueryResult(Map<?, ?> result) {
            columns= (List<String>) result.get("columns");
            extractData(result);
        }

        private void extractData(Map<?, ?> result) {
            List<List<?>> rows= (List<List<?>>) result.get("data");
            data=new ArrayList<Map<String, Object>>(rows.size());
            for (List<?> row : rows) {
                data.add(mapRow(row));
            }
        }

        private Map<String, Object> mapRow(List<?> row) {
            int columnCount=columns.size();
            Map<String,Object> newRow=new HashMap<String, Object>(columnCount);
            for (int i = 0; i < columnCount; i++) {
                final Object value = row.get(i);
                newRow.put(columns.get(i), convertValue(value));
            }
            return newRow;
        }

        private Object convertValue(Object value) {
            final Object representationValue = convertFromRepresentation(value);
            return resultConverter.convertValue(representationValue, null);
        }

        private Object convertFromRepresentation(Object value) {
            if (value instanceof Map) {
                RestEntity restEntity = createRestEntity((Map) value);
                if (restEntity!=null) return restEntity;
            }
            return value;
        }

        private RestEntity createRestEntity(Map data) {
            final String uri = (String) data.get("self");
            if (uri == null || uri.isEmpty()) return null;
            if (uri.contains("/node/")) {
                return new RestNode(data,restGraphDatabase);
            }
            if (uri.contains("/relationship/")) {
                return new RestRelationship(data,restGraphDatabase);
            }
            return null;
        }

        public List<Map<String, Object>> getData() {
            return data;
        }

        public List<String> getColumns() {
            return columns;
        }

        public <T> T getSingleValue(Class<T> type) {
            if (data.size()==0) throw new InvalidDataAccessResourceUsageException("Expected single result, got none");
            if (data.size()!=1) throw new InvalidDataAccessResourceUsageException("Expected single result, got more than one");
            return getSingleColumn(type).iterator().next();
        }

        public <T> Iterable<T> getSingleColumn(final Class<T> type) {
            if (columns.size()==0) throw new InvalidDataAccessResourceUsageException("Expected single column, got none");
            if (columns.size()!=1) throw new InvalidDataAccessResourceUsageException("Expected single column, got more than one");
            final String firstColumn = columns.get(0);
            return new IterableWrapper<T, Map<String,Object>>(data) {
                @Override
                protected T underlyingObjectToObject(Map<String,Object> row) {
                    return resultConverter.convertValue(row.get(firstColumn),type);
                }
            };
        }
    }

    @Override
    public <T> Iterable<T> query(String statement, Class<T> type) {
        final RestQueryResult restQueryResult = executeStatement(statement);
        return restQueryResult.getSingleColumn(type);
    }

    @Override
    public <T> T queryForObject(String statement, Class<T> type) {
        return executeStatement(statement).getSingleValue(type);
    }
}

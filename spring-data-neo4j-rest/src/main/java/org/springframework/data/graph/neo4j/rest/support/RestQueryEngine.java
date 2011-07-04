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


import org.springframework.data.graph.neo4j.conversion.*;
import org.springframework.data.graph.neo4j.support.query.QueryEngine;

import java.util.*;

/**
 * @author mh
 * @since 22.06.11
 */
public class RestQueryEngine implements QueryEngine {
    private final RestRequest restRequest;
    private final RestGraphDatabase restGraphDatabase;
    private final ResultConverter resultConverter;

    public RestQueryEngine(RestGraphDatabase restGraphDatabase) {
        this(restGraphDatabase,null);
    }
    public RestQueryEngine(RestGraphDatabase restGraphDatabase, ResultConverter resultConverter) {
        this.restGraphDatabase = restGraphDatabase;
        this.resultConverter = resultConverter!=null ? resultConverter : new DefaultConverter();
        this.restRequest = restGraphDatabase.getRestRequest();
    }

    @Override
    public QueryResult<Map<String, Object>> query(String statement) {
        return executeStatement(statement);
    }

    private RestQueryResult executeStatement(String statement) {
        final RequestResult requestResult = restRequest.get("ext/CypherPlugin/graphdb/execute_query", JsonHelper.createJsonFrom(Collections.singletonMap("query", statement)));
        return new RestQueryResult(restRequest.toMap(requestResult),restGraphDatabase,resultConverter);
    }

    static class RestQueryResult implements QueryResult<Map<String,Object>> {
        QueryResultBuilder<Map<String,Object>> result;
        private final RestGraphDatabase restGraphDatabase;


        @Override
        public <R> ConvertedResult<R> to(Class<R> type) {
            return result.to(type);
        }

        @Override
        public <R> ConvertedResult<R> to(Class<R> type, ResultConverter<Map<String, Object>, R> converter) {
            return result.to(type,converter);
        }

        @Override
        public Iterator<Map<String, Object>> iterator() {
            return result.iterator();
        }

        public RestQueryResult(Map<?, ?> result, RestGraphDatabase restGraphDatabase, ResultConverter resultConverter) {
            this.restGraphDatabase = restGraphDatabase;
            List<String> columns= (List<String>) result.get("columns");
            final List<Map<String, Object>> data = extractData(result, columns);
            this.result=new QueryResultBuilder<Map<String,Object>>(data, resultConverter);
        }

        private List<Map<String, Object>> extractData(Map<?, ?> restResult, List<String> columns) {
            List<List<?>> rows= (List<List<?>>) restResult.get("data");
            List<Map<String,Object>> result=new ArrayList<Map<String, Object>>(rows.size());
            for (List<?> row : rows) {
                result.add(mapRow(columns,row));
            }
            return result;
        }

        private Map<String, Object> mapRow(List<String> columns, List<?> row) {
            int columnCount=columns.size();
            Map<String,Object> newRow=new HashMap<String, Object>(columnCount);
            for (int i = 0; i < columnCount; i++) {
                final Object value = row.get(i);
                newRow.put(columns.get(i), convertFromRepresentation(value));
            }
            return newRow;
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
    }
}

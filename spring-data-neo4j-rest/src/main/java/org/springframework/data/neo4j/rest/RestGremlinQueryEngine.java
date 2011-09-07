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

package org.springframework.data.neo4j.rest;


import org.neo4j.helpers.collection.IterableWrapper;
import org.neo4j.helpers.collection.MapUtil;
import org.springframework.data.neo4j.conversion.*;
import org.springframework.data.neo4j.support.query.QueryEngine;

import java.util.*;

/**
 * @author mh
 * @since 22.06.11
 */
public class RestGremlinQueryEngine implements QueryEngine<Object> {
    private final RestRequest restRequest;
    private final RestGraphDatabase restGraphDatabase;
    private final ResultConverter resultConverter;

    public RestGremlinQueryEngine(RestGraphDatabase restGraphDatabase) {
        this(restGraphDatabase,null);
    }
    public RestGremlinQueryEngine(RestGraphDatabase restGraphDatabase, ResultConverter resultConverter) {
        this.restGraphDatabase = restGraphDatabase;
        this.resultConverter = resultConverter!=null ? resultConverter : new DefaultConverter();
        this.restRequest = restGraphDatabase.getRestRequest();
    }

    @Override
    public QueryResult<Object> query(String statement, Map<String, Object> params) {
        final String paramsString = JsonHelper.createJsonFrom(params == null ? Collections.emptyMap() : params);
        final String data = JsonHelper.createJsonFrom(MapUtil.map("script", statement,"params",paramsString));
        final RequestResult requestResult = restRequest.get("ext/GremlinPlugin/graphdb/execute_script", data);
        final Object result = JsonHelper.readJson(requestResult.getEntity());
        if (requestResult.getStatus() == 500) {
            return handleError(result);
        } else {
            return new RestQueryResult(result,restGraphDatabase,resultConverter);
        }
    }

    private QueryResult<Object> handleError(Object result) {
        if (result instanceof Map) {
            Map<?, ?> mapResult = (Map<?, ?>) result;
            if (RestResultException.isExceptionResult(mapResult)) {
                throw new RestResultException(mapResult);
            }
        }
        throw new RestResultException(Collections.singletonMap("exception", result.toString()));
    }

    static class RestQueryResult<T> implements QueryResult<T> {
        QueryResultBuilder<T> result;
        private final RestGraphDatabase restGraphDatabase;


        @Override
        public <R> ConvertedResult<R> to(Class<R> type) {
            return result.to(type);
        }

        @Override
        public <R> ConvertedResult<R> to(Class<R> type, ResultConverter<T, R> converter) {
            return result.to(type,converter);
        }

        @Override
        public void handle(Handler<T> handler) {
            result.handle(handler);
        }

        @Override
        public Iterator<T> iterator() {
            return result.iterator();
        }

        public RestQueryResult(Object result, RestGraphDatabase restGraphDatabase, ResultConverter resultConverter) {
            this.restGraphDatabase = restGraphDatabase;
            final Iterable<T> convertedResult = convertRestResult(result);
            this.result=new QueryResultBuilder<T>(convertedResult, resultConverter);
        }

        private Iterable<T> convertRestResult(Object result) {
            final RestEntityExtractor restEntityExtractor = new RestEntityExtractor(restGraphDatabase);
            if (result instanceof Map) {
                Map<?,?> mapResult= (Map<?, ?>) result;
                if (RestResultException.isExceptionResult(mapResult)) {
                    throw new RestResultException(mapResult);
                }
                if (isTableResult(mapResult)) {
                    return (Iterable<T>) new RestTableResultExtractor(restEntityExtractor).extract(mapResult);
                }
            }
            if (result instanceof Iterable) {
                return new IterableWrapper<T,Object>((Iterable<Object>)result) {
                    @Override
                    protected T underlyingObjectToObject(Object value) {
                        return (T) restEntityExtractor.convertFromRepresentation(value);
                    }
                };
            }
            return Collections.singletonList((T) restEntityExtractor.convertFromRepresentation(result));
        }

        public static boolean isTableResult(Map<?, ?> mapResult) {
            return mapResult.containsKey("columns") && mapResult.containsKey("data");
        }
    }
}

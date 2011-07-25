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


import org.springframework.data.neo4j.conversion.*;
import org.springframework.data.neo4j.support.query.QueryEngine;

import java.util.*;

/**
 * @author mh
 * @since 22.06.11
 */
public class RestCypherQueryEngine implements QueryEngine<Map<String,Object>> {
    private final RestRequest restRequest;
    private final RestGraphDatabase restGraphDatabase;
    private final ResultConverter resultConverter;

    public RestCypherQueryEngine(RestGraphDatabase restGraphDatabase) {
        this(restGraphDatabase,null);
    }
    public RestCypherQueryEngine(RestGraphDatabase restGraphDatabase, ResultConverter resultConverter) {
        this.restGraphDatabase = restGraphDatabase;
        this.resultConverter = resultConverter!=null ? resultConverter : new DefaultConverter();
        this.restRequest = restGraphDatabase.getRestRequest();
    }

    @Override
    public QueryResult<Map<String, Object>> query(String statement, Map<String, Object> params) {
        return executeStatement(statement);
    }

    private RestQueryResult executeStatement(String statement) {
        final RequestResult requestResult = restRequest.get("ext/CypherPlugin/graphdb/execute_query", JsonHelper.createJsonFrom(Collections.singletonMap("query", statement)));
        return new RestQueryResult(restRequest.toMap(requestResult),restGraphDatabase,resultConverter);
    }

    static class RestQueryResult implements QueryResult<Map<String,Object>> {
        QueryResultBuilder<Map<String,Object>> result;

        @Override
        public <R> ConvertedResult<R> to(Class<R> type) {
            return result.to(type);
        }

        @Override
        public <R> ConvertedResult<R> to(Class<R> type, ResultConverter<Map<String, Object>, R> converter) {
            return result.to(type,converter);
        }

        @Override
        public void handle(Handler<Map<String, Object>> handler) {
            result.handle(handler);
        }

        @Override
        public Iterator<Map<String, Object>> iterator() {
            return result.iterator();
        }

        public RestQueryResult(Map<?, ?> result, RestGraphDatabase restGraphDatabase, ResultConverter resultConverter) {
            final RestTableResultExtractor extractor = new RestTableResultExtractor(new RestEntityExtractor(restGraphDatabase));
            final List<Map<String, Object>> data = extractor.extract(result);
            this.result=new QueryResultBuilder<Map<String,Object>>(data, resultConverter);
        }
    }
}

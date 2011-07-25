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

import org.neo4j.graphdb.GraphDatabaseService;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.data.neo4j.conversion.DefaultConverter;
import org.springframework.data.neo4j.conversion.QueryResult;
import org.springframework.data.neo4j.conversion.QueryResultBuilder;
import org.springframework.data.neo4j.conversion.ResultConverter;

import java.util.Map;

public class GremlinQueryEngine implements QueryEngine<Object> {

    private final GremlinExecutor gremlinExecutor;
    private final ResultConverter resultConverter;

    public GremlinQueryEngine(GraphDatabaseService graphDatabaseService) {
        this(graphDatabaseService, new DefaultConverter());
    }


    public GremlinQueryEngine(GraphDatabaseService graphDatabaseService, ResultConverter resultConverter) {
        this.resultConverter = resultConverter != null ? resultConverter : new DefaultConverter();
        this.gremlinExecutor = new GremlinExecutor(graphDatabaseService);
    }

    @Override
    public QueryResult<Object> query(String statement, Map<String, Object> params) {
        try {
            Iterable<Object> result = gremlinExecutor.query(statement, params);
            return new QueryResultBuilder<Object>(result,resultConverter);
        } catch (Exception e) {
            throw new InvalidDataAccessResourceUsageException("Error executing statement " + statement, e);
        }
    }
}
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

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.data.neo4j.conversion.DefaultConverter;
import org.springframework.data.neo4j.conversion.QueryResultBuilder;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.conversion.ResultConverter;

import java.util.Map;

public class CypherQueryEngineImpl implements CypherQueryEngine {
    private final static Logger log = LoggerFactory.getLogger(CypherQueryEngineImpl.class);

    private final ExecutionEngine executionEngine;
    private ResultConverter resultConverter;
    private final QueryParameterConverter queryParameterConverter = new QueryParameterConverter();

    public CypherQueryEngineImpl(GraphDatabaseService graphDatabaseService, ResultConverter resultConverter) {
        this.resultConverter = resultConverter != null ? resultConverter : new DefaultConverter();
        this.executionEngine = new ExecutionEngine(graphDatabaseService);
    }

    @Override
    public ResultConverter getResultConverter() {
        return resultConverter;
    }

    @Override
    public void setResultConverter(ResultConverter resultConverter) {
        this.resultConverter = resultConverter;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Result<Map<String, Object>> query(String statement, Map<String, Object> params) {
        try {
            ExecutionResult result = parseAndExecuteQuery(statement,params);
            return new QueryResultBuilder<Map<String,Object>>(result,resultConverter);
        } catch (Exception e) {
            throw new InvalidDataAccessResourceUsageException("Error executing statement " + statement, e);
        }
    }

    private ExecutionResult parseAndExecuteQuery(String statement, Map<String, Object> params) {
        try {
            final Map<String, Object> queryParams = queryParams(params);
            if (log.isDebugEnabled()) log.debug(String.format("Executing cypher query: %s params %s",statement,queryParams));

            return executionEngine.execute(statement, queryParams);
        } catch(Exception e) {
            throw new InvalidDataAccessResourceUsageException("Error executing statement " + statement, e);
        }
    }

    private Map<String, Object> queryParams(Map<String, Object> params) {
        return queryParameterConverter.convert(params);
    }
}

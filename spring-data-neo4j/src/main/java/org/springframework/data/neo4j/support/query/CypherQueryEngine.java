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

import org.neo4j.cypher.commands.Query;
import org.neo4j.cypher.javacompat.CypherParser;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.data.neo4j.conversion.DefaultConverter;
import org.springframework.data.neo4j.conversion.QueryResult;
import org.springframework.data.neo4j.conversion.QueryResultBuilder;
import org.springframework.data.neo4j.conversion.ResultConverter;

import java.util.Collections;
import java.util.Map;

public class CypherQueryEngine implements QueryEngine<Map<String,Object>> {

    final ExecutionEngine executionEngine;
    private ResultConverter resultConverter;

    public CypherQueryEngine(GraphDatabaseService graphDatabaseService) {
        this(graphDatabaseService, new DefaultConverter());
    }


    public CypherQueryEngine(GraphDatabaseService graphDatabaseService, ResultConverter resultConverter) {
        this.resultConverter = resultConverter != null ? resultConverter : new DefaultConverter();
        this.executionEngine = new ExecutionEngine(graphDatabaseService);
    }

    @SuppressWarnings("unchecked")
    @Override
    public QueryResult<Map<String, Object>> query(String statement, Map<String, Object> params) {
        try {
            ExecutionResult result = parseAndExecuteQuery(statement,params);
            return new QueryResultBuilder<Map<String,Object>>(result,resultConverter);
        } catch (Exception e) {
            throw new InvalidDataAccessResourceUsageException("Error executing statement " + statement, e);
        }
    }

    private ExecutionResult parseAndExecuteQuery(String statement, Map<String, Object> params) {
        try {
            CypherParser parser = new CypherParser();
            Query query = parser.parse(statement);
            return executionEngine.execute(query,params==null ? Collections.<String,Object>emptyMap() : params);
        } catch(Exception e) {
            throw new InvalidDataAccessResourceUsageException("Error executing statement " + statement, e);
        }
    }
}
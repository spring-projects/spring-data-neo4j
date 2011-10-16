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

import org.springframework.data.neo4j.annotation.QueryType;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.support.GraphDatabaseContext;
import org.springframework.util.ClassUtils;

import java.util.Map;

/**
 * @author mh
 * @since 10.06.11
 *        todo limits
 */
public class CypherQueryExecutor implements QueryOperations<Map<String,Object>> {
    private final QueryEngine<Map<String,Object>> queryEngine;

    public CypherQueryExecutor(GraphDatabaseContext ctx) {
        if (ClassUtils.isPresent("org.neo4j.cypher.javacompat.ExecutionEngine",getClass().getClassLoader())) {
            queryEngine = ctx.queryEngineFor(QueryType.Cypher);
        } else {
            queryEngine = new QueryEngine<Map<String, Object>>() {
                @Override
                public Result<Map<String, Object>> query(String statement, Map<String, Object> params) {
                    throw new IllegalStateException("Cypher is not available, please add it to your dependencies to execute: "+statement);
                }
            };
        }
    }

    public Iterable<Map<String, Object>> queryForList(String statement, Map<String,Object> params) {
        return queryEngine.query(statement, params);
    }

    public <T> Iterable<T> query(String statement, Class<T> type, Map<String,Object> params) {
        return queryEngine.query(statement, params).to(type);
    }

    public <T> T queryForObject(String statement, Class<T> type, Map<String,Object> params) {
        return (T) queryEngine.query(statement, params).to(type).single();
    }
}
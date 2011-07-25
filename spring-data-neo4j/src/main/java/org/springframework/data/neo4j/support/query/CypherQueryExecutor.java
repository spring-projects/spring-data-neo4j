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

import org.springframework.data.neo4j.support.GraphDatabaseContext;
import org.springframework.data.neo4j.support.conversion.EntityResultConverter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mh
 * @since 10.06.11
 *        todo limits
 */
public class CypherQueryExecutor implements QueryOperations<Map<String,Object>> {
    private final CypherQueryEngine queryEngine;

    public CypherQueryExecutor(GraphDatabaseContext ctx) {
        EntityResultConverter converter = new EntityResultConverter(ctx);
        queryEngine = new CypherQueryEngine(ctx.getGraphDatabaseService(), converter);
    }

    public Iterable<Map<String, Object>> queryForList(String statement, Map<String,Object>...params) {
        return queryEngine.query(statement,mergeParams(params));
    }

    public <T> Iterable<T> query(String statement, Class<T> type, Map<String,Object>...params) {
        return queryEngine.query(statement,mergeParams(params)).to(type);
    }

    public <T> T queryForObject(String statement, Class<T> type, Map<String,Object>...params) {
        return (T) queryEngine.query(statement,mergeParams(params)).to(type).single();
    }
    private Map<String,Object> mergeParams(Map<String,Object>...params) {
        if (params==null || params.length==0) return Collections.emptyMap();
        Map<String,Object> result=new HashMap<String, Object>();
        for (Map<String, Object> map : params) {
            result.putAll(map);
        }
        return result;
    }
}
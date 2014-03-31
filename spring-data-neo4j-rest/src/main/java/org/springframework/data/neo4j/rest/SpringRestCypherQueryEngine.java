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


import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.query.RestCypherQueryEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.conversion.QueryResultBuilder;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.conversion.ResultConverter;
import org.springframework.data.neo4j.support.query.CypherQueryEngine;
import org.springframework.data.neo4j.support.query.QueryEngine;

import java.util.Map;


public class SpringRestCypherQueryEngine implements CypherQueryEngine {

    public static final Logger log = LoggerFactory.getLogger(SpringRestCypherQueryEngine.class);

    private final RestCypherQueryEngine restCypherQueryEngine;
    private ResultConverter resultConverter;

    public SpringRestCypherQueryEngine(RestAPI restAPI, ResultConverter resultConverter) {
        this.resultConverter = resultConverter;
        this.restCypherQueryEngine = new RestCypherQueryEngine(restAPI, new SpringResultConverter(resultConverter));
    }

    @Override
    public Result<Map<String,Object>> query(String statement, Map<String, Object> params) {
        if (log.isDebugEnabled()) log.debug(String.format("Executing remote cypher query: %s params %s",statement,params));

        return new QueryResultBuilder<Map<String, Object>>(restCypherQueryEngine.query(statement, params), resultConverter);
    }

    public ResultConverter getResultConverter() {
        return resultConverter;
    }

    public void setResultConverter(ResultConverter resultConverter) {
        this.resultConverter = resultConverter;
    }

    private static class SpringResultConverter implements org.neo4j.rest.graphdb.util.ResultConverter {
        private final ResultConverter resultConverter;

        public SpringResultConverter(ResultConverter resultConverter) {
            this.resultConverter = resultConverter;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object convert(Object value, Class target) {
            return resultConverter.convert(value,target);
        }
    }
}

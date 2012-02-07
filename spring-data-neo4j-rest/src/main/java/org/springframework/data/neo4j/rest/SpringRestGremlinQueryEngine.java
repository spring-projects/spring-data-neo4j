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


import org.neo4j.rest.graphdb.query.RestGremlinQueryEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.support.query.QueryEngine;

import java.util.Map;


public class SpringRestGremlinQueryEngine implements QueryEngine<Object> {

    public static final Logger log = LoggerFactory.getLogger(SpringRestGremlinQueryEngine.class);

    private final RestGremlinQueryEngine restGremlinQueryEngine;

    public SpringRestGremlinQueryEngine(RestGremlinQueryEngine restGremlinQueryEngine) {
        this.restGremlinQueryEngine = restGremlinQueryEngine;
    }

    @Override
    public SpringRestResult<Object> query(String statement, Map<String, Object> params) {
        if (log.isDebugEnabled()) log.debug(String.format("Executing remote gremlin query: %s params %s",statement,params));

        return new SpringRestResult<Object>(restGremlinQueryEngine.query(statement, params));
    }

}

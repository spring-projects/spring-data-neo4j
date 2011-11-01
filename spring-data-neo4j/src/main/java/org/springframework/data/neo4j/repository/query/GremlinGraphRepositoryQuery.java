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
package org.springframework.data.neo4j.repository.query;

import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.annotation.QueryType;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.query.QueryEngine;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.ParameterAccessor;

import java.util.Map;

/**
* @author mh
* @since 31.10.11
*/
class GremlinGraphRepositoryQuery extends GraphRepositoryQuery {

    private QueryEngine queryEngine;

    public GremlinGraphRepositoryQuery(GraphQueryMethod queryMethod, final Neo4jTemplate template) {
        super(queryMethod, template);
    }

    protected QueryEngine getQueryEngine() {
        if (this.queryEngine !=null) return queryEngine;
        this.queryEngine = getTemplate().queryEngineFor(QueryType.Gremlin);
        return this.queryEngine;
    }
}

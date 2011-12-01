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
package org.springframework.data.neo4j.repository;

import java.util.Map;

import org.neo4j.cypherdsl.Execute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.conversion.EndResult;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class NodeCypherDslGraphRepository<T> extends NodeGraphRepository<T>implements CypherDslRepository<T> {

    public NodeCypherDslGraphRepository(final Class<T> clazz, final Neo4jTemplate template) {
    	super(clazz, template);
    }
	
    @Override
    public Page<T> query(Execute query, Map<String, Object> params, Pageable page) {
        return CypherDslQueryUtil.query(template, clazz, query, params, page);
    }

    @Override
    public EndResult<T> query(Execute query, Map<String, Object> params) {
        return CypherDslQueryUtil.query(template, clazz, query, params);
    }
}

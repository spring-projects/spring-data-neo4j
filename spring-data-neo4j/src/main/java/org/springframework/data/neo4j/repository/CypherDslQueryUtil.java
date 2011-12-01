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
import org.neo4j.cypherdsl.Skip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.annotation.QueryType;
import org.springframework.data.neo4j.conversion.EndResult;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;

@Repository
public final class CypherDslQueryUtil {
	private CypherDslQueryUtil() {
	}
	
    @SuppressWarnings("unchecked")
    public static <T> Page<T> query(Neo4jTemplate template, Class<T> clazz, Execute query, Map<String, Object> params, Pageable page) {
        final Execute limitedQuery = ((Skip)query).skip(page.getOffset()).limit(page.getPageSize());
        return template.queryEngineFor(QueryType.Cypher).query(limitedQuery.toString(), params).to(clazz).as(Page.class);
    }

    public static <T> EndResult<T> query(Neo4jTemplate template, Class<T> clazz, Execute query, Map<String, Object> params) {
        return template.queryEngineFor(QueryType.Cypher).query(query.toString(), params).to(clazz);
    }
}

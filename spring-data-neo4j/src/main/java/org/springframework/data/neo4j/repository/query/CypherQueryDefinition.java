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
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.Parameter;

import java.util.Map;

/**
 * Interface to abstract Cypher query creation.
 * 
 *
 * @author Oliver Gierke
 */
interface CypherQueryDefinition extends ParameterResolver {

    /**
     * Returns a Cypher query without adding any sort or pagination.
     * 
     * @return
     */
    String toQueryString();

    /**
     * Returns a Cypher query adding the given {@link Sort}.
     * 
     * @param sort
     * @return
     */
    String toQueryString(Sort sort);

    /**
     * Returns a Cypher query restricting the result to the given {@link Pageable} and applying the {@link Sort}
     * contained in it.
     * 
     * @param pageable
     * @return
     */
    String toQueryString(Pageable pageable);
}

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

import org.neo4j.cypherdsl.grammar.Execute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * @author mh
 * @since 11.11.11
 */
public interface CypherDslRepository<T> {
    @Transactional
    Page<T> query(Execute query, Map<String, Object> params, Pageable page);
    @Transactional
    Page<T> query(Execute query, Execute countQuery, Map<String, Object> params, Pageable page);
    @Transactional
    Result<T> query(Execute query, Map<String, Object> params);
}

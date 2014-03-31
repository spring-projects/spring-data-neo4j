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

import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * CRUD interface for graph repositories, used as base repository for crud operations.
 * 
 * @author Michael Hunger
 * @author Oliver Gierke
 */
@NoRepositoryBean
public interface CRUDRepository<T> extends PagingAndSortingRepository<T, Long> {


    /**
     * uses the configured TypeRepresentationStrategy to load all entities, might return a large result
     * @return all entities of the given type
     * NOTE: please close the iterable if it is not fully looped through
     */
    Result<T> findAll();
    

    /**
     * finder that takes the provided sorting into account
     * NOTE: the sorting is not yet implemented
     * @param sort
     * @return all elements of the repository type, sorted according to the sort
     * NOTE: please close the iterable if it is not fully looped through
     */
    Result<T> findAll(Sort sort);


    Class getStoredJavaType(Object entity);

    
    Result<T> query(String query, Map<String, Object> params);
}

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

package org.springframework.data.graph.neo4j.repository;

import org.neo4j.graphdb.PropertyContainer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.graph.core.GraphBacked;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;

/**
 * @author mh
 * @since 28.03.11
 */
@NoRepositoryBean
public interface CRUDGraphRepository<S extends PropertyContainer, T extends GraphBacked<S>> extends PagingAndSortingRepository<T, Long> {

    @Transactional
    T save(T entity);


    @Transactional
    Iterable<T> save(Iterable<? extends T> entities);


    T findOne(Long id);


    boolean exists(Long id);


    Iterable<T> findAll();


    Long count();


    @Transactional
    void delete(T entity);


    @Transactional
    void delete(Iterable<? extends T> entities);


    @Transactional
    void deleteAll();


    Iterable<T> findAll(Sort sort);


    Page<T> findAll(Pageable pageable);

}
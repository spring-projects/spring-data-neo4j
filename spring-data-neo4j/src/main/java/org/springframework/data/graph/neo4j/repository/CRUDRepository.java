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

import org.neo4j.helpers.collection.ClosableIterable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.graph.core.GraphBacked;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD interface for graph repositories, used as base repository for crud operations
 */
@NoRepositoryBean
public interface CRUDRepository<T extends GraphBacked<?>> extends PagingAndSortingRepository<T, Long> {

    /**
     * persists an entity by forwarding to entity.persist()
     * @param entity to be persisted
     * @return the saved entity (being the same reference as the parameter)
     */
    @Transactional
    T save(T entity);


    /**
     * persists the provided entities by forwarding to their entity.persist() methods
     * @param entities to be persisted
     * @return the input iterable
     */
    @Transactional
    Iterable<T> save(Iterable<? extends T> entities);


    /**
     * @param id of the node or relationship-entity
     * @return found instance or null
     */
    T findOne(Long id);


    /**
     * @param id
     * @return true if the entity with this id exists
     */
    boolean exists(Long id);


    /**
     * uses the configured TypeRepresentationStrategy to load all entities, might return a large result
     * @return all entities of the given type
     * NOTE: please close the iterable if it is not fully looped through
     */
    ClosableIterable<T> findAll();


    /**
     * uses the configured TypeRepresentationStrategy, depending on the strategy this number might be an
     * approximation
     * @return number of entities of this type in the graph
     */
    Long count();


    /**
     * deletes the given entity by calling its entity.remove() method
     * @param entity to delete
     */
    @Transactional
    void delete(T entity);


    /**
     * deletes the given entities by calling their entity.remove() methods
     * @param entities to delete
     */
    @Transactional
    void delete(Iterable<? extends T> entities);


    /**
     * removes all entities of this type, use with care
     */
    @Transactional
    void deleteAll();


    /**
     * finder that takes the provided sorting into account
     * NOTE: the sorting is not yet implemented
     * @param sort
     * @return all elements of the repository type, sorted according to the sort
     * NOTE: please close the iterable if it is not fully looped through
     */
    ClosableIterable<T> findAll(Sort sort);


    /**
     * finder that takes the provided sorting and paging into account
     * NOTE: the sorting is not yet implemented
     *
     * @param pageable
     * @return all elements of the repository type, sorted according to the sort
     * NOTE: please close the iterable if it is not fully looped through
     */
    Page<T> findAll(Pageable pageable);

}
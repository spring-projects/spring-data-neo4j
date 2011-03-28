package org.springframework.data.graph.neo4j.repository;

import org.neo4j.graphdb.PropertyContainer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.graph.core.GraphBacked;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;

/**
 * @author mh
 * @since 28.03.11
 */
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
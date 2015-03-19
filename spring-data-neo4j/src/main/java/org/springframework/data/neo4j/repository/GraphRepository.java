/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * @author Vince Bickers
 */
@NoRepositoryBean
public interface GraphRepository<T> extends CrudRepository<T, Long> {

    <S extends T> S save(S s, int depth);

    <S extends T> Iterable<S> save(Iterable<S> entities, int depth);

    T findOne(Long id, int depth);

    Iterable<T> findAll(int depth);

    Iterable<T> findAll(Iterable<Long> ids, int depth);

    //Iterable<T> findByProperty(String propertyName, Object propertyValue);

    //Iterable<T> findByProperty(String propertyName, Object propertyValue, int depth);
}

/*
 * Copyright (c)  [2011-2018] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.examples.movies.repo;

import java.io.Serializable;
import java.util.Collection;

import org.springframework.data.neo4j.examples.movies.domain.Person;
import org.springframework.data.neo4j.repository.Neo4jRepository;

/**
 * @author Luanne Misquitta
 * @author Michael J. Simons
 */
public interface PersonRepository<T extends Person, ID extends Serializable> extends Neo4jRepository<T, ID> {

	Collection<T> findByName(String name);

	Collection<T> findByNameIgnoreCase(String name);
}

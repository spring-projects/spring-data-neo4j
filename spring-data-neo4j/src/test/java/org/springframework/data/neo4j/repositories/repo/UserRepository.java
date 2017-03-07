/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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

package org.springframework.data.neo4j.repositories.repo;

import java.util.List;

import org.springframework.data.neo4j.repositories.domain.User;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Michal Bachman
 * @author Vince Bickers
 */
@Repository
public interface UserRepository extends Neo4jRepository<User, Long> {

	/*
	 * @see DATAGRAPH-813
	 */
	Long deleteByName(String name); // return a count of deleted objects by name

	/*
	 * @see DATAGRAPH-813
	 */
	List<Long> removeByName(String name); // remove users by name and return an iterable of the removed users' ids

	/*
	 * @see DATAGRAPH-813
	 */
	Long countByName(String name); // return a count of objects with name

}

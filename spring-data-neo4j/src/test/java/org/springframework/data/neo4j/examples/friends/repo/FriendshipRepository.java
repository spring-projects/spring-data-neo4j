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

package org.springframework.data.neo4j.examples.friends.repo;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.examples.friends.domain.Friendship;
import org.springframework.data.neo4j.examples.friends.domain.Person;
import org.springframework.data.neo4j.repository.GraphRepository;

/**
 * @author Luanne Misquitta
 */
public interface FriendshipRepository extends GraphRepository<Friendship> {

	@Query("MATCH (person1)-[rel:IS_FRIEND]->(person2) WHERE ID(person1)={0} AND ID(person2)={1} return rel")
	Friendship getFriendship(Person person1, Person person2);

}

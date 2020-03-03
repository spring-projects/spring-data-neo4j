/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.examples.friends.repo;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.examples.friends.domain.Friendship;
import org.springframework.data.neo4j.examples.friends.domain.Person;
import org.springframework.data.neo4j.repository.Neo4jRepository;

/**
 * @author Luanne Misquitta
 */
public interface FriendshipRepository extends Neo4jRepository<Friendship, Long> {

	@Query("MATCH (person1)-[rel:IS_FRIEND]->(person2) WHERE ID(person1)=$0 AND ID(person2)=$1 return rel")
	Friendship getFriendship(Person person1, Person person2);

}

/*
 * Copyright 2011-2022 the original author or authors.
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
package org.springframework.data.neo4j.integration.lite;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

/**
 * @author Michael J. Simons
 */
public interface SomeDomainRepository extends Neo4jRepository<SomeDomainObject, UUID> {

	/**
	 * @return Mapping arbitrary, ungrouped results into a dto
	 */
	// language=cypher
	@Query("UNWIND range(1,10) AS x RETURN randomUUID() AS resyncId, tointeger(x*rand()*10)+1 AS counter ORDER BY counter")
	Collection<MyDTO> getAllFlat();

	/**
	 * @return Mapping a single ungrouped result
	 */
	// language=cypher
	@Query("RETURN randomUUID() AS resyncId, 4711 AS counter")
	Optional<MyDTO> getOneFlat();

	/**
	 * @return Mapping a dto plus known domain objects
	 */
	// language=cypher
	@Query("""
			MATCH (u:User {login:'michael'}) -[r:OWNS] -> (s:SomeDomainObject)
			WITH u, collect(r) AS r, collect(s) AS ownedObjects
			RETURN
				u{.*, __internalNeo4jId__: id(u), r, ownedObjects} AS user,
				randomUUID() AS resyncId, 4711 AS counter,u
			""")
	Collection<MyDTO> getNestedStuff();

	/**
	 * @return Mapping nested dtos
	 */
	// language=cypher
	@Query("RETURN 'av' AS outer, {inner: 'bv'} AS nested")
	Optional<A> getOneNestedDTO();
}

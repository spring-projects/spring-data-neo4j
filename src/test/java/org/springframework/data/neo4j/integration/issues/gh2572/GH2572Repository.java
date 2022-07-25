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
package org.springframework.data.neo4j.integration.issues.gh2572;

import java.util.List;
import java.util.Optional;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

/**
 * @author Michael J. Simons
 */
public interface GH2572Repository extends Neo4jRepository<GH2572Child, String> {

	@Query("MATCH(person:GH2572Parent {id: $id}) "
		   + "OPTIONAL MATCH (person)<-[:IS_PET]-(dog:GH2572Child) "
		   + "RETURN dog")
	List<GH2572Child> getDogsForPerson(String id);

	@Query("MATCH(person:GH2572Parent {id: $id}) "
		   + "OPTIONAL MATCH (person)<-[:IS_PET]-(dog:GH2572Child) "
		   + "RETURN dog ORDER BY dog.name ASC LIMIT 1")
	Optional<GH2572Child> findOneDogForPerson(String id);

	@Query("MATCH(person:GH2572Parent {id: $id}) "
		   + "OPTIONAL MATCH (person)<-[:IS_PET]-(dog:GH2572Child) "
		   + "RETURN dog ORDER BY dog.name ASC LIMIT 1")
	GH2572Child getOneDogForPerson(String id);
}

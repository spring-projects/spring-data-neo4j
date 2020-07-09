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
package org.springframework.data.neo4j.documentation.repositories.projection;

import java.util.Collection;
import java.util.List;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.repository.Neo4jRepository;

/**
 * @author Gerrit Meier
 */
// tag::projection.entity[]
@Node
public class Person {

	@Id @GeneratedValue private Long id;
	String firstName;
	String lastName;

	@Relationship("LIVES_AT") Address address;

	@Node
	static class Address {
		@Id @GeneratedValue private Long id;
		private String zipCode;
		private String city;
		private String street;
	}

}
// end::projection.entity[]

// tag::projection.repository[]
interface PersonRepository extends Neo4jRepository<Person, Long> {

	// tag::projection.repository.concrete[]
	List<Person> findByLastName(String lastName);
	// end::projection.repository.concrete[]

	// tag::projection.repository.interface[]
	List<NamesOnly> findByFirstName(String firstName);
	// end::projection.repository.interface[]
}
// end::projection.repository[]

// tag::projection.dynamic-projection-repository[]
interface DynamicProjectionPersonRepository extends Neo4jRepository<Person, Long> {

	<T> Collection<T> findByFirstName(String firstName, Class<T> type);
}
// end::projection.dynamic-projection-repository[]

// tag::projection.dynamic-projection-usage[]
class DynamicProjectionService {

	void someMethod(DynamicProjectionPersonRepository people) {
		Collection<Person> daves = people.findByFirstName("Dave", Person.class);

		Collection<NamesOnly> davesWithNameOnly = people.findByFirstName("Dave", NamesOnly.class);
	}
}
// end::projection.dynamic-projection-usage[]

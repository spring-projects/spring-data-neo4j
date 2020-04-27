/*
 * Copyright (c) 2019-2020 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.springframework.data.examples.spring_boot;

// tag::faq.template-imperative[]

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.neo4j.springframework.boot.test.autoconfigure.data.DataNeo4jTest;
import org.neo4j.springframework.data.core.Neo4jTemplate;
import org.neo4j.springframework.data.examples.spring_boot.domain.MovieEntity;
import org.neo4j.springframework.data.examples.spring_boot.domain.PersonEntity;
import org.neo4j.springframework.data.examples.spring_boot.domain.Roles;
import org.springframework.beans.factory.annotation.Autowired;

// end::faq.template-imperative[]
/**
 * @author Michael J. Simons
 */
// tag::faq.template-imperative[]
// tag::testing.dataneo4jtest[]
@DataNeo4jTest
public class TemplateExampleTest {
	// end::testing.dataneo4jtest[]
	@Test
	void shouldSaveAndReadEntities(@Autowired Neo4jTemplate neo4jTemplate) {

		MovieEntity movie = new MovieEntity(
			"The Love Bug",
			"A movie that follows the adventures of Herbie, Herbie's driver, "
				+ "Jim Douglas (Dean Jones), and Jim's love interest, "
				+ "Carole Bennett (Michele Lee)");

		movie.getActorsAndRoles().put(new PersonEntity(1931, "Dean Jones"), new Roles(singletonList("Didi")));
		movie.getActorsAndRoles().put(new PersonEntity(1942, "Michele Lee"), new Roles(singletonList("Michi")));

		neo4jTemplate.save(movie);

		Optional<PersonEntity> person = neo4jTemplate
			.findById("Dean Jones", PersonEntity.class);
		assertThat(person).map(PersonEntity::getBorn).hasValue(1931);

		assertThat(neo4jTemplate.count(PersonEntity.class)).isEqualTo(2L);
	}
	// tag::testing.dataneo4jtest[]
}
// end::faq.template-imperative[]
// end::testing.dataneo4jtest[]

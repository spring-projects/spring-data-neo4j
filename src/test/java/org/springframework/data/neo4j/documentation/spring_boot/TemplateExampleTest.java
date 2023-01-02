/*
 * Copyright 2011-2023 the original author or authors.
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
package org.springframework.data.neo4j.documentation.spring_boot;

// tag::faq.template-imperative-pt1[]

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Optional;

// end::faq.template-imperative-pt1[]
import org.junit.jupiter.api.Disabled;
// tag::faq.template-imperative-pt1[]
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.documentation.domain.MovieEntity;
import org.springframework.data.neo4j.documentation.domain.PersonEntity;
import org.springframework.data.neo4j.documentation.domain.Roles;

// end::faq.template-imperative-pt1[]

/**
 * @author Michael J. Simons
 */
@Disabled
// tag::faq.template-imperative-pt2[]
public class TemplateExampleTest {

	@Test
	void shouldSaveAndReadEntities(@Autowired Neo4jTemplate neo4jTemplate) {

		MovieEntity movie = new MovieEntity("The Love Bug",
				"A movie that follows the adventures of Herbie, Herbie's driver, "
						+ "Jim Douglas (Dean Jones), and Jim's love interest, " + "Carole Bennett (Michele Lee)");

		Roles roles1 = new Roles(new PersonEntity(1931, "Dean Jones"), Collections.singletonList("Didi"));
		Roles roles2 = new Roles(new PersonEntity(1942, "Michele Lee"), Collections.singletonList("Michi"));
		movie.getActorsAndRoles().add(roles1);
		movie.getActorsAndRoles().add(roles2);

		neo4jTemplate.save(movie);

		Optional<PersonEntity> person = neo4jTemplate.findById("Dean Jones", PersonEntity.class);
		assertThat(person).map(PersonEntity::getBorn).hasValue(1931);

		assertThat(neo4jTemplate.count(PersonEntity.class)).isEqualTo(2L);
	}
}
// end::faq.template-imperative-pt2[]

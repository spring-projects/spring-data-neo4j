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

// tag::faq.template-reactive-pt1[]

import reactor.test.StepVerifier;

import java.util.Collections;

// end::faq.template-reactive-pt1[]
import org.junit.jupiter.api.Disabled;
// tag::faq.template-reactive-pt1[]
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.ReactiveNeo4jTemplate;
import org.springframework.data.neo4j.documentation.domain.MovieEntity;
import org.springframework.data.neo4j.documentation.domain.PersonEntity;
import org.springframework.data.neo4j.documentation.domain.Roles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// end::faq.template-reactive-pt1[]

/**
 * @author Michael J. Simons
 */
@Disabled
// tag::faq.template-reactive-pt1[]
@Testcontainers
// end::faq.template-reactive-pt1[]
// tag::faq.template-reactive-pt2[]
class ReactiveTemplateExampleTest {

	@Container private static Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>("neo4j:5");

	@DynamicPropertySource
	static void neo4jProperties(DynamicPropertyRegistry registry) {
		registry.add("org.neo4j.driver.uri", neo4jContainer::getBoltUrl);
		registry.add("org.neo4j.driver.authentication.username", () -> "neo4j");
		registry.add("org.neo4j.driver.authentication.password", neo4jContainer::getAdminPassword);
	}

	@Test
	void shouldSaveAndReadEntities(@Autowired ReactiveNeo4jTemplate neo4jTemplate) {

		MovieEntity movie = new MovieEntity("The Love Bug",
				"A movie that follows the adventures of Herbie, Herbie's driver, "
						+ "Jim Douglas (Dean Jones), and Jim's love interest, " + "Carole Bennett (Michele Lee)");

		Roles role1 = new Roles(new PersonEntity(1931, "Dean Jones"), Collections.singletonList("Didi"));
		Roles role2 = new Roles(new PersonEntity(1942, "Michele Lee"), Collections.singletonList("Michi"));
		movie.getActorsAndRoles().add(role1);
		movie.getActorsAndRoles().add(role2);

		StepVerifier.create(neo4jTemplate.save(movie)).expectNextCount(1L).verifyComplete();

		StepVerifier.create(neo4jTemplate.findById("Dean Jones", PersonEntity.class).map(PersonEntity::getBorn))
				.expectNext(1931).verifyComplete();

		StepVerifier.create(neo4jTemplate.count(PersonEntity.class)).expectNext(2L).verifyComplete();
	}
}
// end::faq.template-reactive-pt2[]

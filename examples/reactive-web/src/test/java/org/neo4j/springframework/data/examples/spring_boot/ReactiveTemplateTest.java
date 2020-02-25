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

import reactor.test.StepVerifier;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.driver.springframework.boot.test.autoconfigure.Neo4jTestHarnessAutoConfiguration;
import org.neo4j.springframework.boot.test.autoconfigure.data.AutoConfigureDataNeo4j;
import org.neo4j.springframework.data.core.ReactiveNeo4jTemplate;
import org.neo4j.springframework.data.examples.spring_boot.domain.MovieEntity;
import org.neo4j.springframework.data.examples.spring_boot.domain.PersonEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// tag::faq.template-reactive[]
// end::faq.template-reactive[]
// tag::faq.template-reactive[]
// end::faq.template-reactive[]

/**
 * @author Michael J. Simons
 */
@Testcontainers
@EnabledIfEnvironmentVariable(named = ReactiveTemplateTest.SYS_PROPERTY_NEO4J_VERSION, matches = "4\\.0.*")
@ExtendWith(SpringExtension.class)
@EnableAutoConfiguration(exclude = Neo4jTestHarnessAutoConfiguration.class)
@AutoConfigureDataNeo4j
@ContextConfiguration(initializers = ReactiveTemplateTest.Initializer.class)
	// tag::faq.template-reactive[]
class ReactiveTemplateTest {

	// end::faq.template-reactive[]

	private static final String SYS_PROPERTY_NEO4J_ACCEPT_COMMERCIAL_EDITION = "SDN_RX_NEO4J_ACCEPT_COMMERCIAL_EDITION";
	private static final String SYS_PROPERTY_NEO4J_REPOSITORY = "SDN_RX_NEO4J_REPOSITORY";
	protected static final String SYS_PROPERTY_NEO4J_VERSION = "SDN_RX_NEO4J_VERSION";

	@Container
	private static Neo4jContainer<?> neo4jContainer =
		new Neo4jContainer<>(
			Optional.ofNullable(System.getenv(SYS_PROPERTY_NEO4J_REPOSITORY)).orElse("neo4j") + ":" + Optional
				.ofNullable(System.getenv(SYS_PROPERTY_NEO4J_VERSION)).orElse("4.0.0"))
			.withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT",
				Optional.ofNullable(System.getenv(SYS_PROPERTY_NEO4J_ACCEPT_COMMERCIAL_EDITION)).orElse("no"));

	// tag::faq.template-reactive[]
	@Test
	void shouldSaveAndReadEntities(@Autowired ReactiveNeo4jTemplate neo4jTemplate) {

		MovieEntity movie = new MovieEntity(
			"The Love Bug",
			"A movie that follows the adventures of Herbie, Herbie's driver, "
				+ "Jim Douglas (Dean Jones), and Jim's love interest, "
				+ "Carole Bennett (Michele Lee)");

		movie.getActors().add(new PersonEntity(1931, "Dean Jones"));
		movie.getActors().add(new PersonEntity(1942, "Michele Lee"));

		StepVerifier.create(neo4jTemplate.save(movie))
			.expectNextCount(1L)
			.verifyComplete();

		StepVerifier.create(neo4jTemplate
			.findById("Dean Jones", PersonEntity.class)
			.map(PersonEntity::getBorn)
		)
			.expectNext(1931)
			.verifyComplete();

		StepVerifier.create(neo4jTemplate.count(PersonEntity.class))
			.expectNext(2L)
			.verifyComplete();
	}
	// end::faq.template-reactive[]

	static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
		public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
			TestPropertyValues.of(
				"org.neo4j.driver.uri=" + neo4jContainer.getBoltUrl(),
				"org.neo4j.driver.authentication.username=neo4j",
				"org.neo4j.driver.authentication.password=" + neo4jContainer.getAdminPassword()
			).applyTo(configurableApplicationContext.getEnvironment());
		}
	}
	// tag::faq.template-reactive[]
}
// end::faq.template-reactive[]

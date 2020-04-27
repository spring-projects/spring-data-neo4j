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

// tag::testing.reactivedataneo4jtest[]

import static org.assertj.core.api.Assertions.*;

import reactor.test.StepVerifier;

// end::testing.reactivedataneo4jtest[]
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
// tag::testing.reactivedataneo4jtest[]
import org.junit.jupiter.api.Test;
// end::testing.reactivedataneo4jtest[]
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
// tag::testing.reactivedataneo4jtest[]
import org.neo4j.springframework.boot.test.autoconfigure.data.ReactiveDataNeo4jTest;
import org.neo4j.springframework.data.examples.spring_boot.domain.MovieRepository;
import org.neo4j.springframework.data.examples.spring_boot.domain.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// end::testing.reactivedataneo4jtest[]

/**
 * @author Michael J. Simons
 * @author Gerrit Meier
 */
// tag::testing.reactivedataneo4jtest[]
@Testcontainers
@ReactiveDataNeo4jTest
class RepositoryIT {

	@Container
	private static Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>("neo4j:4.0");

	@DynamicPropertySource
	static void neo4jProperties(DynamicPropertyRegistry registry) {
		registry.add("org.neo4j.driver.uri", neo4jContainer::getBoltUrl);
		registry.add("org.neo4j.driver.authentication.username", () -> "neo4j");
		registry.add("org.neo4j.driver.authentication.password", neo4jContainer::getAdminPassword);
	}

	// end::testing.reactivedataneo4jtest[]
	@BeforeEach
	void setup(@Autowired Driver driver) throws IOException {
		try (BufferedReader moviesReader = new BufferedReader(
			new InputStreamReader(this.getClass().getResourceAsStream("/movies.cypher")));
			Session session = driver.session()) {
			session.run("MATCH (n) DETACH DELETE n");
			String moviesCypher = moviesReader.lines().collect(Collectors.joining(" "));
			session.run(moviesCypher);
		}
	}

	// tag::testing.reactivedataneo4jtest[]
	@Test
	void loadAllPersonsFromGraph(@Autowired PersonRepository personRepository) {
		int expectedPersonCount = 133;
		StepVerifier.create(personRepository.findAll())
			.expectNextCount(expectedPersonCount)
			.verifyComplete();
	}

	// end::testing.reactivedataneo4jtest[]

	@Test
	void findPersonByName(@Autowired PersonRepository personRepository) {
		StepVerifier.create(personRepository.findByName("Tom Hanks"))
			.assertNext(personEntity -> {
				assertThat(personEntity.getBorn()).isEqualTo(1956);
			})
			.verifyComplete();
	}

	@Test
	void findsPersonsWhoActAndDirect(@Autowired PersonRepository personRepository) {
		int expectedActorAndDirectorCount = 5;
		StepVerifier.create(personRepository.getPersonsWhoActAndDirect())
			.expectNextCount(expectedActorAndDirectorCount)
			.verifyComplete();
	}

	@Test
	void findOneMovie(@Autowired MovieRepository movieRepository) {
		StepVerifier.create(movieRepository.findOneByTitle("The Matrix"))
			.assertNext(movie -> {
				assertThat(movie.getTitle()).isEqualTo("The Matrix");
				assertThat(movie.getDescription()).isEqualTo("Welcome to the Real World");
				assertThat(movie.getDirectors()).hasSize(2);
				assertThat(movie.getActorsAndRoles()).hasSize(5);
			})
			.verifyComplete();
	}

	// tag::testing.reactivedataneo4jtest[]
}
// end::testing.reactivedataneo4jtest[]

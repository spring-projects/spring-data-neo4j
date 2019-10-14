/*
 * Copyright (c) 2019 "Neo4j,"
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

import static org.assertj.core.api.Assertions.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.springframework.boot.test.autoconfigure.data.DataNeo4jTest;
import org.neo4j.springframework.data.examples.spring_boot.domain.MovieRepository;
import org.neo4j.springframework.data.examples.spring_boot.domain.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Michael J. Simons
 */
@DataNeo4jTest
class RepositoryIT {

	@Autowired
	private PersonRepository repository;

	@Autowired
	private MovieRepository movieRepository;

	@Autowired
	private Driver driver;

	@BeforeEach
	void setup() throws IOException {
		try (BufferedReader moviesReader = new BufferedReader(
			new InputStreamReader(this.getClass().getResourceAsStream("/movies.cypher")));
			Session session = driver.session()) {
			session.run("MATCH (n) DETACH DELETE n");
			String moviesCypher = moviesReader.lines().collect(Collectors.joining(" "));
			session.run(moviesCypher);
		}
	}

	@Test
	void loadAllPersonsFromGraph() {
		int expectedPersonCount = 133;
		assertThat(repository.count()).isEqualTo(expectedPersonCount);
	}

	@Test
	void findPersonByName() {
		assertThat(repository.findByName("Tom Hanks"))
			.isPresent().get()
			.satisfies(personEntity -> assertThat(personEntity.getBorn()).isEqualTo(1956));
	}

	@Test
	void findsPersonsWhoActAndDirect() {
		int expectedActorAndDirectorCount = 5;
		assertThat(repository.getPersonsWhoActAndDirect()).hasSize(expectedActorAndDirectorCount);
	}

	@Test
	void findOneMovie() {
		assertThat(movieRepository.findOneByTitle("The Matrix"))
			.isPresent().get()
			.satisfies(movie -> {
				assertThat(movie.getTitle()).isEqualTo("The Matrix");
				assertThat(movie.getDescription()).isEqualTo("Welcome to the Real World");
				assertThat(movie.getDirectors()).hasSize(2);
				assertThat(movie.getActors()).hasSize(5);
			});
	}
}

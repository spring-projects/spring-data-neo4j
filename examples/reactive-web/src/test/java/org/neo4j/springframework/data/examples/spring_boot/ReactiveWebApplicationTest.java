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

import static org.assertj.core.api.Assertions.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.springframework.data.examples.spring_boot.domain.MovieEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @author Gerrit Meier
 */
@SpringBootTest
@AutoConfigureWebTestClient
@Testcontainers
public class ReactiveWebApplicationTest {

	@Autowired
	private Driver driver;

	@Autowired
	private WebTestClient client;

	@Container
	private static Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>("neo4j:4.0");

	@DynamicPropertySource
	static void neo4jProperties(DynamicPropertyRegistry registry) {
		registry.add("org.neo4j.driver.uri", neo4jContainer::getBoltUrl);
		registry.add("org.neo4j.driver.authentication.username", () -> "neo4j");
		registry.add("org.neo4j.driver.authentication.password", neo4jContainer::getAdminPassword);
	}

	@BeforeEach
	void setupData() throws IOException {
		try (BufferedReader moviesReader = new BufferedReader(
			new InputStreamReader(this.getClass().getResourceAsStream("/movies.cypher")));
			Session session = driver.session()) {

			session.writeTransaction(tx -> {
				tx.run("MATCH (n) DETACH DELETE n");
				String moviesCypher = moviesReader.lines().collect(Collectors.joining(" "));
				tx.run(moviesCypher);
				return null;
			});
		}

	}

	@Test
	void listAllMovies() {
		client.get().uri("/movies").exchange()
			.expectStatus().isOk()
			.expectBodyList(MovieEntity.class).hasSize(38);
	}

	@Test
	void movieByTitle() {
		client.get().uri("/movies/by-title?title=The Matrix").exchange()
			.expectStatus().isOk()
			.expectBody(MovieEntity.class)
			.consumeWith(result -> {
				MovieEntity movie = result.getResponseBody();
				assertThat(movie.getTitle()).isEqualTo("The Matrix");
			});
	}

	@Test
	void createMovie() {
		MovieEntity newMovie = new MovieEntity("Aeon Flux", "Reactive is the new cool");
		client.put().uri("/movies").bodyValue(newMovie).exchange()
			.expectStatus().isOk();

		// usually a web test ends here but for completeness we check the database directly
		try (Session session = driver.session()) {
			String tagline = session.run("MATCH (m:Movie{title:'Aeon Flux'}) return m")
				.single()
				.get("m").asNode()
				.get("tagline").asString();

			assertThat(tagline).isEqualTo("Reactive is the new cool");
		}
	}

	@Test
	void deleteMovie() {
		client.delete().uri("/movies/The Matrix") // disclaimer: you should never delete The Matrix
			.exchange()
			.expectStatus().isOk();

		// and another back to back check in the database
		try (Session session = driver.session()) {
			Long movieCount = session.run("MATCH (m:Movie) return count(m) as movieCount")
				.single()
				.get("movieCount").asLong();

			assertThat(movieCount).isEqualTo(37L);
		}
	}

}

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @author Gerrit Meier
 */
@SpringBootTest
@AutoConfigureMockMvc
public class ImperativeWebApplicationTest {

	@Autowired
	private Driver driver;

	@Autowired
	private MockMvc client;

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
	void listAllMovies() throws Exception {

		client.perform(get("/movies"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(38));
	}

	@Test
	void movieByTitle() throws Exception {

		client.perform(get("/movies/by-title?title=The Matrix"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.title").value("The Matrix"));
	}

	@Test
	void createMovie() throws Exception {
		client.perform(put("/movies")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\": \"Aeon Flux\", \"description\": \"Reactive is the new cool\"}"))
			.andExpect(status().isOk());

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
	void deleteMovie() throws Exception {
		client.perform(delete("/movies/The Matrix"))
			.andExpect(status().isOk());

		// and another back to back check in the database
		try (Session session = driver.session()) {
			Long movieCount = session.run("MATCH (m:Movie) return count(m) as movieCount")
				.single()
				.get("movieCount").asLong();

			assertThat(movieCount).isEqualTo(37L);
		}
	}

}

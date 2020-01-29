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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

/**
 * This examples brings up the complete application and starts it in profile {@literal "selection-by-user"}, thus activating
 * {@link Neo4jConfig#databaseSelectionProvider()}.
 * <p>The test mocks a user name {@literal "someMovieEnthusiast"}. We create a database with the same name and load our
 * movie data into it.
 *
 * @author Michael J. Simons
 */
@ContextConfiguration(initializers = TestContainerInitializer.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("selection-by-user")
@AutoConfigureMockMvc
@WithMockUser("someMovieEnthusiast")
public class ExampleUsingDynamicDatabaseNameTest {

	@BeforeAll
	static void createDatabase(@Autowired Driver driver) throws IOException {
		try (Session session = driver.session(SessionConfig.forDatabase("system"))) {
			// Corresponds to the mocked user at the top of this class.
			session.run("CREATE DATABASE someMovieEnthusiast");
		}

		try (BufferedReader moviesReader = new BufferedReader(
			new InputStreamReader(ExampleUsingDynamicDatabaseNameTest.class.getResourceAsStream("/movies.cypher")));
			Session session = driver.session(SessionConfig.forDatabase("someMovieEnthusiast"))) {

			session.run("MATCH (n) DETACH DELETE n");
			String moviesCypher = moviesReader.lines().collect(Collectors.joining(" "));
			session.run(moviesCypher);
		}
	}

	@Test
	void moviesByTitleShouldWork(@Autowired MockMvc mvc) throws Exception {

		mvc.perform(get("/movies/by-title").param("title", "Stand By Me"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("title", equalTo("Stand By Me")))
			.andExpect(jsonPath("description",
				startsWith("For some, it's the last real taste of innocence, and the first real taste of life.")))
			.andDo(MockMvcResultHandlers.print());
	}
}

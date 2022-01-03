/*
 * Copyright 2011-2022 the original author or authors.
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
package org.springframework.data.neo4j.documentation.repositories.custom_queries;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.documentation.domain.MovieEntity;
import org.springframework.data.neo4j.documentation.domain.PersonEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
class CustomQueriesIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	// tag::custom-queries-test[]
	@Test
	void customRepositoryFragmentsShouldWork(
			@Autowired PersonRepository people,
			@Autowired MovieRepository movies
	) {

		PersonEntity meg = people.findById("Meg Ryan").get();
		PersonEntity kevin = people.findById("Kevin Bacon").get();

		List<MovieEntity> moviesBetweenMegAndKevin = movies.
				findMoviesAlongShortestPath(meg, kevin);
		assertThat(moviesBetweenMegAndKevin).isNotEmpty();

		Collection<NonDomainResults.Result> relatedPeople = movies
				.findRelationsToMovie(moviesBetweenMegAndKevin.get(0));
		assertThat(relatedPeople).isNotEmpty();

		assertThat(movies.deleteGraph()).isGreaterThan(0);
		assertThat(movies.findAll()).isEmpty();
		assertThat(people.findAll()).isEmpty();
	}
	// end::custom-queries-test[]

	@BeforeAll
	static void setupData(@Autowired Driver driver) throws IOException {

		try (BufferedReader moviesReader = new BufferedReader(
				new InputStreamReader(CustomQueriesIT.class.getResourceAsStream("/data/movies.cypher")));
				Session session = driver.session()) {
			session.run("MATCH (n) DETACH DELETE n").consume();
			String moviesCypher = moviesReader.lines().collect(Collectors.joining(" "));
			session.run(moviesCypher).consume();
		}
	}

	interface PersonRepository extends Neo4jRepository<PersonEntity, String> {
	}

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends AbstractNeo4jConfig {

		@Bean
		@Override
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Override
		protected Collection<String> getMappingBasePackages() {
			return Collections.singletonList(MovieEntity.class.getPackage().getName());
		}
	}
}

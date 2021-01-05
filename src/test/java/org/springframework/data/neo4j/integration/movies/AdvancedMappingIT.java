/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.neo4j.integration.movies;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 * @soundtrack Body Count - Manslaughter
 */
@Neo4jIntegrationTest
class AdvancedMappingIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@BeforeAll
	static void setupData(@Autowired Driver driver) throws IOException {

		try (BufferedReader moviesReader = new BufferedReader(
				new InputStreamReader(AdvancedMappingIT.class.getClass().getResourceAsStream("/data/movies.cypher")));
				Session session = driver.session()) {
			session.run("MATCH (n) DETACH DELETE n");
			String moviesCypher = moviesReader.lines().collect(Collectors.joining(" "));
			session.run(moviesCypher);
		}
	}

	/**
	 * Here all paths are going into multiple records. Each path will be one record. The elements in the path will be
	 * seen as aggregated on the server side and each of the aggregates will also be aggregated.
	 *
	 * @param template Used for querying
	 */
	@Test // DATAGRAPH-1437
	void multiplePathsShouldWork(@Autowired Neo4jTemplate template) {

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("person1", "Kevin Bacon");
		parameters.put("person2", "Angela Scope");
		String cypherQuery =
				"MATCH allPaths=allShortestPathS((p1:Person {name: $person1})-[*]-(p2:Person {name: $person2}))\n"
				+ "RETURN allPaths";

		List<Person> people = template.findAll(cypherQuery, parameters, Person.class);
		assertThat(people).hasSize(7);
	}

	/**
	 * Here all paths are going into one single record.
	 *
	 * @param template Used for querying
	 */
	@Test // DATAGRAPH-1437
	void multiplePreAggregatedPathsShouldWork(@Autowired Neo4jTemplate template) {

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("person1", "Kevin Bacon");
		parameters.put("person2", "Angela Scope");
		String cypherQuery =
				"MATCH allPaths=allShortestPathS((p1:Person {name: $person1})-[*]-(p2:Person {name: $person2}))\n"
				+ "RETURN collect(allPaths)";

		List<Person> people = template.findAll(cypherQuery, parameters, Person.class);
		assertThat(people).hasSize(7);
	}

	/**
	 * This tests checks whether all nodes that fit a certain class along a path are mapped correctly.
	 *
	 * @param template Used for querying
	 */
	@Test // DATAGRAPH-1437
	void pathMappingWithoutAdditionalInformationShouldWork(@Autowired Neo4jTemplate template) {

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("person1", "Kevin Bacon");
		parameters.put("person2", "Angela Scope");
		parameters.put("requiredMovie", "The Da Vinci Code");
		String cypherQuery =
				"MATCH p=shortestPath((p1:Person {name: $person1})-[*]-(p2:Person {name: $person2}))\n"
				+ "WHERE size([n IN nodes(p) WHERE n.title = $requiredMovie]) > 0\n"
				+ "RETURN p";
		List<Person> people = template.findAll(cypherQuery, parameters, Person.class);

		assertThat(people)
				.hasSize(4)
				.extracting(Person::getName)
				.contains("Kevin Bacon", "Jessica Thompson",
						"Angela Scope"); // Two paths lead there, one with Ron Howard, one with Tom Hanks.
		assertThat(people).element(2).extracting(Person::getReviewed)
				.satisfies(
						movies -> assertThat(movies).extracting(Movie::getTitle).containsExactly("The Da Vinci Code"));
	}

	/**
	 * This tests checks whether all nodes that fit a certain class along a path are mapped correctly and if the
	 * additional joined information is applied as well.
	 *
	 * @param template Used for querying
	 */
	@Test // DATAGRAPH-1437
	void pathMappingWithAdditionalInformationShouldWork(@Autowired Neo4jTemplate template) {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("person1", "Kevin Bacon");
		parameters.put("person2", "Meg Ryan");
		parameters.put("requiredMovie", "The Da Vinci Code");
		String cypherQuery =
				"MATCH p=shortestPath(\n"
				+ "(p1:Person {name: $person1})-[*]-(p2:Person {name: $person2}))\n"
				+ "WITH p, [n in nodes(p) WHERE n:Movie] as mn\n"
				+ "UNWIND mn as m\n"
				+ "MATCH (m) <-[r:DIRECTED]- (d:Person)\n"
				+ "RETURN p, collect(r), collect(d)";
		List<Movie> movies = template.findAll(cypherQuery, parameters, Movie.class);

		assertThat(movies)
				.hasSize(2)
				.allSatisfy(m -> assertThat(m.getDirectors()).isNotEmpty())
				.first()
				.satisfies(m -> assertThat(m.getDirectors()).extracting(Person::getName)
						.containsAnyOf("Ron Howard", "Rob Reiner"));
	}

	@Configuration
	@EnableTransactionManagement
	static class Config extends AbstractNeo4jConfig {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}
	}
}

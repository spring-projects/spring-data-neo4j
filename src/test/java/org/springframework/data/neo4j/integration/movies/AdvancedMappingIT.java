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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
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

	protected static long theMatrixId;

	@BeforeAll
	static void setupData(@Autowired Driver driver) throws IOException {

		try (BufferedReader moviesReader = new BufferedReader(
				new InputStreamReader(AdvancedMappingIT.class.getResourceAsStream("/data/movies.cypher")));
				Session session = driver.session()) {
			session.run("MATCH (n) DETACH DELETE n").consume();
			String moviesCypher = moviesReader.lines().collect(Collectors.joining(" "));
			session.run(moviesCypher).consume();
			session.run("MATCH (l1:Person {name: 'Lilly Wachowski'})\n"
						+ "MATCH (l2:Person {name: 'Lana Wachowski'})\n"
						+ "CREATE (l1) - [s:IS_SIBLING_OF] -> (l2)\n"
						+ "RETURN *").consume();
			session.run("MATCH (m1:Movie {title: 'The Matrix'})\n"
						+ "MATCH (m2:Movie {title: 'The Matrix Reloaded'})\n"
						+ "MATCH (m3:Movie {title: 'The Matrix Revolutions'})\n"
						+ "CREATE (m2) - [:IS_SEQUEL_OF] -> (m1)\n"
						+ "CREATE (m3) - [:IS_SEQUEL_OF] -> (m2)\n"
						+ "RETURN *").consume();
			session.run("MATCH (m1:Movie {title: 'The Matrix'})\n"
						+ "MATCH (m2:Movie {title: 'The Matrix Reloaded'})\n"
						+ "CREATE (p:Person {name: 'Gloria Foster'})\n"
						+ "CREATE (p) -[:ACTED_IN {roles: ['The Oracle']}] -> (m1)\n"
						+ "CREATE (p) -[:ACTED_IN {roles: ['The Oracle']}] -> (m2)\n"
						+ "RETURN *").consume();
			session.run("MATCH (m3:Movie {title: 'The Matrix Revolutions'})\n"
						+ "CREATE (p:Person {name: 'Mary Alice'})\n"
						+ "CREATE (p) -[:ACTED_IN {roles: ['The Oracle']}] -> (m3)\n"
						+ "RETURN *").consume();
		}
	}

	interface MovieProjection {

		String getTitle();

		List<Actor> getActors();
	}

	static class MovieDTO {

		private final String title;

		private final List<Actor> actors;

		MovieDTO(String title, List<Actor> actors) {
			this.title = title;
			this.actors = actors;
		}

		public String getTitle() {
			return title;
		}

		public List<Actor> getActors() {
			return actors;
		}
	}

	interface MovieRepository extends Neo4jRepository<Movie, String> {

		MovieProjection findProjectionByTitle(String title);

		MovieDTO findDTOByTitle(String title);
	}

	@Test // GH-2117
	void bothCyclicAndNonCyclicRelationshipsAreExcludedFromProjections(@Autowired MovieRepository movieRepository) {

		// The movie domain is a good fit for this test
		// as the cyclic dependencies is pretty slow to retrieve from Neo4j
		// this does OOM in most setups.
		MovieProjection projection = movieRepository.findProjectionByTitle("The Matrix");
		assertThat(projection.getTitle()).isNotNull();
		assertThat(projection.getActors()).isNotEmpty();
	}

	@Test // GH-2117
	void bothCyclicAndNonCyclicRelationshipsAreExcludedFromDTOProjections(@Autowired MovieRepository movieRepository) {

		// The movie domain is a good fit for this test
		// as the cyclic dependencies is pretty slow to retrieve from Neo4j
		// this does OOM in most setups.
		MovieDTO dtoProjection = movieRepository.findDTOByTitle("The Matrix");
		assertThat(dtoProjection.getTitle()).isNotNull();
		assertThat(dtoProjection.getActors()).isNotEmpty();
	}

	@Test // GH-2114
	void bothStartAndEndNodeOfPathsMustBeLookedAt(@Autowired Neo4jTemplate template) {

		// @ParameterizedTest does not work together with the parameter resolver for @Autowired
		for (String query : new String[] {
				"MATCH p=()-[:IS_SIBLING_OF]-> () RETURN p",
				"MATCH (s)-[:IS_SIBLING_OF]-> (e) RETURN [s,e]"
		}) {
			List<Person> people = template.findAll(query, Collections.emptyMap(), Person.class);
			assertThat(people)
					.extracting(Person::getName)
					.containsExactlyInAnyOrder("Lilly Wachowski", "Lana Wachowski");
		}
	}

	@Test // GH-2114
	void directionAndTypeLessPathMappingShouldWork(@Autowired Neo4jTemplate template) {

		List<Person> people = template.findAll("MATCH p=(:Person)-[]-(:Person) RETURN p", Collections.emptyMap(), Person.class);
		assertThat(people).hasSize(6);
	}

	@Test // GH-2114
	void mappingOfAPathWithOddNumberOfElementsShouldWorkFromStartToEnd(@Autowired Neo4jTemplate template) {

		Map<String, Movie> movies = template
				.findAll("MATCH p=shortestPath((:Person {name: 'Mary Alice'})-[*]-(:Person {name: 'Emil Eifrem'})) RETURN p", Collections.emptyMap(), Movie.class)
				.stream().collect(Collectors.toMap(Movie::getTitle, Function.identity()));
		assertThat(movies).hasSize(3);

		// This is the actual test for the original issue… When the end node of a segment is not taken into account, Emil is not an actor
		assertThat(movies).hasEntrySatisfying("The Matrix", m -> assertThat(m.getActors()).isNotEmpty());
		assertThat(movies).hasEntrySatisfying("The Matrix Revolutions", m -> assertThat(m.getActors()).isNotEmpty());

		assertThat(movies).hasEntrySatisfying("The Matrix", m -> assertThat(m.getSequel()).isNotNull());
		assertThat(movies).hasEntrySatisfying("The Matrix Reloaded", m -> assertThat(m.getSequel()).isNotNull());
		assertThat(movies).hasEntrySatisfying("The Matrix Revolutions", m -> assertThat(m.getSequel()).isNull());
	}

	@Test // GH-2114
	void mappingOfAPathWithEventNumberOfElementsShouldWorkFromStartToEnd(@Autowired Neo4jTemplate template) {

		Map<String, Movie> movies = template
				.findAll("MATCH p=shortestPath((:Movie {title: 'The Matrix Revolutions'})-[*]-(:Person {name: 'Emil Eifrem'})) RETURN p", Collections.emptyMap(), Movie.class)
				.stream().collect(Collectors.toMap(Movie::getTitle, Function.identity()));
		assertThat(movies).hasSize(3);

		// This is the actual test for the original issue… When the end node of a segment is not taken into account, Emil is not an actor
		assertThat(movies).hasEntrySatisfying("The Matrix", m -> assertThat(m.getActors()).isNotEmpty());
		assertThat(movies).hasEntrySatisfying("The Matrix Revolutions", m -> assertThat(m.getActors()).isEmpty());

		assertThat(movies).hasEntrySatisfying("The Matrix", m -> assertThat(m.getSequel()).isNotNull());
		assertThat(movies).hasEntrySatisfying("The Matrix Reloaded", m -> assertThat(m.getSequel()).isNotNull());
		assertThat(movies).hasEntrySatisfying("The Matrix Revolutions", m -> assertThat(m.getSequel()).isNull());
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
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends AbstractNeo4jConfig {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}
	}
}

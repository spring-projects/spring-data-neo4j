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
package org.springframework.data.neo4j.integration.movies.imperative;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.integration.movies.shared.Actor;
import org.springframework.data.neo4j.integration.movies.shared.CypherUtils;
import org.springframework.data.neo4j.integration.movies.shared.Movie;
import org.springframework.data.neo4j.integration.movies.shared.Organisation;
import org.springframework.data.neo4j.integration.movies.shared.Partner;
import org.springframework.data.neo4j.integration.movies.shared.Person;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 * @soundtrack Body Count - Manslaughter
 */
@Neo4jIntegrationTest
class AdvancedMappingIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@BeforeAll
	static void setupData(@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture) throws IOException {

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			session.run("MATCH (n) DETACH DELETE n").consume();
			CypherUtils.loadCypherFromResource("/data/movies.cypher", session);
			CypherUtils.loadCypherFromResource("/data/orgstructure.cypher", session);
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	interface MovieProjectionWithActorProjection {
		String getTitle();

		List<ActorProjection> getActors();

		interface ActorProjection {
			String getName();
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

		MovieProjectionWithActorProjection findProjectionWithProjectionByTitle(String title);

		@Query("MATCH p=(movie:Movie)<-[r:ACTED_IN]-(n:Person) WHERE movie.title=$title RETURN collect(p)")
		Movie customPathQueryMovieFind(@Param("title") String title);

		@Query("MATCH p=(movie:Movie)<-[r:ACTED_IN]-(n:Person) WHERE movie.title=$title RETURN collect(p)")
		List<Movie> customPathQueryMoviesFind(@Param("title") String title);
	}

	@Test // GH-1906
	void nestedSelfRelationshipsFromCustomQueryShouldWork(@Autowired Neo4jTemplate template) {

		Optional<Partner> optionalPartner = template.findOne(
				"MATCH p=(partner:Partner {code: $partnerCode})-[:CHILD_ORGANISATIONS*0..4]->(org:Organisation) \n"
				+ "UNWIND nodes(p) as node UNWIND relationships(p) as rel\n"
				+ "RETURN partner, collect(distinct node), collect(distinct rel)",
				Collections.singletonMap("partnerCode", "partner-one"), Partner.class);

		assertThat(optionalPartner).hasValueSatisfying(p -> {
			assertThat(p.getName()).isEqualTo("partner one");

			assertThat(p.getOrganisations()).hasSize(1);
			Organisation org1 = p.getOrganisations().get(0);

			assertThat(org1.getCode()).isEqualTo("org-1");
			Map<String, Organisation> org1Childs = org1.getOrganisations().stream()
					.collect(Collectors.toMap(Organisation::getCode, Function.identity()));
			assertThat(org1Childs).hasSize(2);

			assertThat(org1Childs).hasEntrySatisfying("org-2", o -> assertThat(o.getOrganisations()).hasSize(1));
			assertThat(org1Childs).hasEntrySatisfying("org-6", o -> assertThat(o.getOrganisations()).isEmpty());

			Organisation org3 = org1Childs.get("org-2").getOrganisations().get(0);
			assertThat(org3.getCode()).isEqualTo("org-3");

			Map<String, Organisation> org3Childs = org3.getOrganisations().stream()
					.collect(Collectors.toMap(Organisation::getCode, Function.identity()));
			assertThat(org3Childs).containsKeys("org-4", "org-5");
		});
	}

	@Test
	void cyclicMappingShouldReturnResultForFindById(@Autowired MovieRepository repository) {
		Movie movie = repository.findById("The Matrix").get();
		assertThat(movie).isNotNull();
		assertThat(movie.getTitle()).isEqualTo("The Matrix");
		assertThat(movie.getActors()).hasSize(6);
	}

	@Test
	void cyclicMappingShouldReturnResultForFindAllById(@Autowired MovieRepository repository) {
		List<Movie> movies = repository.findAllById(Arrays.asList("The Matrix", "The Matrix Revolutions", "The Matrix Reloaded"));
		assertThat(movies).hasSize(3);
	}

	@Test
	void cyclicMappingShouldReturnResultForFindAll(@Autowired MovieRepository repository) {
		List<Movie> movies = repository.findAll();
		assertThat(movies).hasSize(38);
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

	@Test // GH-2117
	void bothCyclicAndNonCyclicRelationshipsAreExcludedFromProjectionsWithProjections(
			@Autowired MovieRepository movieRepository) {

		// The movie domain is a good fit for this test
		// as the cyclic dependencies is pretty slow to retrieve from Neo4j
		// this does OOM in most setups.
		MovieProjectionWithActorProjection projection = movieRepository
				.findProjectionWithProjectionByTitle("The Matrix");
		assertThat(projection.getTitle()).isNotNull();
		assertThat(projection.getActors()).isNotEmpty();
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

		List<Person> people = template
				.findAll("MATCH p=(:Person)-[]-(:Person) RETURN p", Collections.emptyMap(), Person.class);
		assertThat(people).hasSize(6);
	}

	@Test // GH-2114
	void mappingOfAPathWithOddNumberOfElementsShouldWorkFromStartToEnd(@Autowired Neo4jTemplate template) {

		Map<String, Movie> movies = template
				.findAll(
						"MATCH p=shortestPath((:Person {name: 'Mary Alice'})-[*]-(:Person {name: 'Emil Eifrem'})) RETURN p",
						Collections.emptyMap(), Movie.class)
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
				.findAll(
						"MATCH p=shortestPath((:Movie {title: 'The Matrix Revolutions'})-[*]-(:Person {name: 'Emil Eifrem'})) RETURN p",
						Collections.emptyMap(), Movie.class)
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

	/**
	 * This tests checks if the result of a custom path based query will get mapped correctly to an instance
	 * of the defined type instead of a collection.
	 */
	@Test // DATAGRAPH-2107
	void customPathMappingResultsInScalarResultIfDefined(@Autowired MovieRepository movieRepository) {
		Movie movie = movieRepository.customPathQueryMovieFind("The Matrix Revolutions");

		assertThat(movie).isNotNull();
		assertThat(movie.getActors()).hasSize(5);
	}

	/**
	 * This tests checks if the result of a custom path based query will get mapped correctly to a collection
	 * of the defined type with all the fields hydrated.
	 */
	@Test // DATAGRAPH-2109
	void customPathMappingCollectionResultsInHydratedEntities(@Autowired MovieRepository movieRepository) {
		List<Movie> movies = movieRepository.customPathQueryMoviesFind("The Matrix Revolutions");

		assertThat(movies).hasSize(1);
		assertThat(movies.get(0).getActors()).hasSize(5);
	}

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends AbstractNeo4jConfig {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Override
		public PlatformTransactionManager transactionManager(Driver driver, DatabaseSelectionProvider databaseNameProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new Neo4jTransactionManager(driver, databaseNameProvider, Neo4jBookmarkManager.create(bookmarkCapture));
		}
	}
}

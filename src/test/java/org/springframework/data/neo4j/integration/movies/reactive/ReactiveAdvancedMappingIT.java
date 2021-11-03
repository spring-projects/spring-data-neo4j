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
package org.springframework.data.neo4j.integration.movies.reactive;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractReactiveNeo4jConfig;
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.ReactiveNeo4jTemplate;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;
import org.springframework.data.neo4j.integration.movies.shared.Actor;
import org.springframework.data.neo4j.integration.movies.shared.CypherUtils;
import org.springframework.data.neo4j.integration.movies.shared.Movie;
import org.springframework.data.neo4j.integration.movies.shared.Person;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
@Tag(Neo4jExtension.NEEDS_REACTIVE_SUPPORT)
@Neo4jIntegrationTest
class ReactiveAdvancedMappingIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@BeforeAll
	static void setupData(@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture) throws IOException {

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			session.run("MATCH (n) DETACH DELETE n").consume();
			CypherUtils.loadCypherFromResource("/data/movies.cypher", session);
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	interface MovieProjectionWithActorProjection {
		String getTitle();

		List<MovieProjectionWithActorProjection.ActorProjection> getActors();

		interface ActorProjection {
			List<String> getRoles();

			MovieProjectionWithActorProjection.ActorProjection.PersonProjection getPerson();

			interface PersonProjection {

				String getName();

				List<Movie> getActedIn();
			}
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

	interface MovieWithSequelProjection {
		String getTitle();
		MovieWithSequelProjection getSequel();
	}

	interface MovieWithSequelEntity {
		String getTitle();
		Movie getSequel();
	}

	interface MovieRepository extends ReactiveNeo4jRepository<Movie, String> {

		Mono<MovieProjection> findProjectionByTitle(String title);

		Mono<MovieDTO> findDTOByTitle(String title);

		Mono<MovieProjectionWithActorProjection> findProjectionWithProjectionByTitle(String title);

		@Query("MATCH p=(movie:Movie)<-[r:ACTED_IN]-(n:Person) WHERE movie.title=$title RETURN collect(p)")
		Mono<Movie> customPathQueryMovieFind(@Param("title") String title);

		@Query("MATCH p=(movie:Movie)<-[r:ACTED_IN]-(n:Person) WHERE movie.title=$title RETURN collect(p)")
		Flux<Movie> customPathQueryMoviesFind(@Param("title") String title);

		Mono<MovieWithSequelProjection> findProjectionByTitleAndDescription(String title, String description);

		Mono<MovieWithSequelEntity> findByTitleAndDescription(String title, String description);
	}

	@Test
	void cyclicMappingShouldReturnResultForFindById(@Autowired MovieRepository repository) {
		StepVerifier.create(repository.findById("The Matrix"))
				.assertNext(movie -> {
					assertThat(movie).isNotNull();
					assertThat(movie.getTitle()).isEqualTo("The Matrix");
					assertThat(movie.getActors()).hasSize(6);
				})
		.verifyComplete();
	}

	@Test
	void cyclicMappingShouldReturnResultForFindAllById(@Autowired MovieRepository repository) {
		StepVerifier.create(repository.findAllById(Arrays.asList("The Matrix", "The Matrix Revolutions", "The Matrix Reloaded")))
				.expectNextCount(3)
				.verifyComplete();
	}

	@Test
	void cyclicMappingShouldReturnResultForFindAll(@Autowired MovieRepository repository) {
		StepVerifier.create(repository.findAll())
				.expectNextCount(38)
				.verifyComplete();
	}

	@Test // GH-2117
	void bothCyclicAndNonCyclicRelationshipsAreExcludedFromProjections(@Autowired MovieRepository movieRepository) {

		// The movie domain is a good fit for this test
		// as the cyclic dependencies is pretty slow to retrieve from Neo4j
		// this does OOM in most setups.
		StepVerifier.create(movieRepository.findProjectionByTitle("The Matrix"))
				.assertNext(projection -> {
					assertThat(projection.getTitle()).isNotNull();
					assertThat(projection.getActors()).isNotEmpty();
				})
				.verifyComplete();
	}

	@Test // GH-2117
	void bothCyclicAndNonCyclicRelationshipsAreExcludedFromDTOProjections(@Autowired MovieRepository movieRepository) {

		// The movie domain is a good fit for this test
		// as the cyclic dependencies is pretty slow to retrieve from Neo4j
		// this does OOM in most setups.
		StepVerifier.create(movieRepository.findDTOByTitle("The Matrix"))
				.assertNext(dtoProjection -> {
					assertThat(dtoProjection.getTitle()).isNotNull();
					assertThat(dtoProjection.getActors()).extracting("name")
							.containsExactlyInAnyOrder("Gloria Foster", "Keanu Reeves", "Emil Eifrem", "Laurence Fishburne",
									"Carrie-Anne Moss", "Hugo Weaving");
				})
				.verifyComplete();
	}

	@Test // GH-2117
	void bothCyclicAndNonCyclicRelationshipsAreExcludedFromProjectionsWithProjections(@Autowired MovieRepository movieRepository) {

		// The movie domain is a good fit for this test
		// as the cyclic dependencies is pretty slow to retrieve from Neo4j
		// this does OOM in most setups.
		StepVerifier.create(movieRepository.findProjectionWithProjectionByTitle("The Matrix"))
				.assertNext(projection -> {
					assertThat(projection.getTitle()).isEqualTo("The Matrix");
					assertThat(projection.getActors()).extracting("person").extracting("name")
							.containsExactlyInAnyOrder("Gloria Foster", "Keanu Reeves", "Emil Eifrem", "Laurence Fishburne",
									"Carrie-Anne Moss", "Hugo Weaving");
					assertThat(projection.getActors()).flatExtracting("roles")
							.containsExactlyInAnyOrder("The Oracle", "Morpheus", "Trinity", "Agent Smith", "Emil", "Neo");
				})
				.verifyComplete();
	}

	@Test // GH-2114
	void bothStartAndEndNodeOfPathsMustBeLookedAt(@Autowired ReactiveNeo4jTemplate template) {

		// @ParameterizedTest does not work together with the parameter resolver for @Autowired
		for (String query : new String[] {
				"MATCH p=()-[:IS_SIBLING_OF]-> () RETURN p",
				"MATCH (s)-[:IS_SIBLING_OF]-> (e) RETURN [s,e]"
		}) {
			StepVerifier.create(template.findAll(query, Collections.emptyMap(), Person.class))
					.recordWith(ArrayList::new)
					.expectNextCount(2)
					.consumeRecordedWith(people ->
							assertThat(people).extracting(Person::getName)
								.containsExactlyInAnyOrder("Lilly Wachowski", "Lana Wachowski")
					)
					.verifyComplete();
		}
	}

	@Test // GH-2114
	void directionAndTypeLessPathMappingShouldWork(@Autowired ReactiveNeo4jTemplate template) {

		StepVerifier.create(
				template.findAll("MATCH p=(:Person)-[]-(:Person) RETURN p", Collections.emptyMap(), Person.class))
				.expectNextCount(6)
				.verifyComplete();
	}

	@Test // GH-2114
	void mappingOfAPathWithOddNumberOfElementsShouldWorkFromStartToEnd(@Autowired ReactiveNeo4jTemplate template) {

		StepVerifier.create(template
				.findAll("MATCH p=shortestPath((:Person {name: 'Mary Alice'})-[*]-(:Person {name: 'Emil Eifrem'})) RETURN p", Collections.emptyMap(), Movie.class))
				.recordWith(ArrayList::new)
				.expectNextCount(3)
				.consumeRecordedWith(result -> {
					Map<String, Movie> movies = result.stream().collect(Collectors.toMap(Movie::getTitle, Function.identity()));

					// This is the actual test for the original issue… When the end node of a segment is not taken into account, Emil is not an actor
					assertThat(movies).hasEntrySatisfying("The Matrix", m -> assertThat(m.getActors()).isNotEmpty());
					assertThat(movies).hasEntrySatisfying("The Matrix Revolutions", m -> assertThat(m.getActors()).isNotEmpty());

					assertThat(movies).hasEntrySatisfying("The Matrix", m -> assertThat(m.getSequel()).isNotNull());
					assertThat(movies).hasEntrySatisfying("The Matrix Reloaded", m -> assertThat(m.getSequel()).isNotNull());
					assertThat(movies).hasEntrySatisfying("The Matrix Revolutions", m -> assertThat(m.getSequel()).isNull());
				})
				.verifyComplete();
	}

	@Test // GH-2114
	void mappingOfAPathWithEventNumberOfElementsShouldWorkFromStartToEnd(@Autowired ReactiveNeo4jTemplate template) {

		StepVerifier.create(template
				.findAll("MATCH p=shortestPath((:Movie {title: 'The Matrix Revolutions'})-[*]-(:Person {name: 'Emil Eifrem'})) RETURN p", Collections.emptyMap(), Movie.class))
				.recordWith(ArrayList::new)
				.expectNextCount(3)
				.consumeRecordedWith(result -> {
					Map<String, Movie> movies = result.stream().collect(Collectors.toMap(Movie::getTitle, Function.identity()));

					// This is the actual test for the original issue… When the end node of a segment is not taken into account, Emil is not an actor
					assertThat(movies).hasEntrySatisfying("The Matrix", m -> assertThat(m.getActors()).isNotEmpty());
					assertThat(movies).hasEntrySatisfying("The Matrix Revolutions", m -> assertThat(m.getActors()).isEmpty());

					assertThat(movies).hasEntrySatisfying("The Matrix", m -> assertThat(m.getSequel()).isNotNull());
					assertThat(movies).hasEntrySatisfying("The Matrix Reloaded", m -> assertThat(m.getSequel()).isNotNull());
					assertThat(movies).hasEntrySatisfying("The Matrix Revolutions", m -> assertThat(m.getSequel()).isNull());
				})
				.verifyComplete();
	}

	/**
	 * Here all paths are going into multiple records. Each path will be one record. The elements in the path will be
	 * seen as aggregated on the server side and each of the aggregates will also be aggregated.
	 *
	 * @param template Used for querying
	 */
	@Test // DATAGRAPH-1437
	void multiplePathsShouldWork(@Autowired ReactiveNeo4jTemplate template) {

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("person1", "Kevin Bacon");
		parameters.put("person2", "Angela Scope");
		String cypherQuery =
				"MATCH allPaths=allShortestPathS((p1:Person {name: $person1})-[*]-(p2:Person {name: $person2}))\n"
				+ "RETURN allPaths";

		StepVerifier.create(template.findAll(cypherQuery, parameters, Person.class))
				.expectNextCount(7)
				.verifyComplete();
	}

	/**
	 * Here all paths are going into one single record.
	 *
	 * @param template Used for querying
	 */
	@Test // DATAGRAPH-1437
	void multiplePreAggregatedPathsShouldWork(@Autowired ReactiveNeo4jTemplate template) {

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("person1", "Kevin Bacon");
		parameters.put("person2", "Angela Scope");
		String cypherQuery =
				"MATCH allPaths=allShortestPathS((p1:Person {name: $person1})-[*]-(p2:Person {name: $person2}))\n"
				+ "RETURN collect(allPaths)";

		StepVerifier.create(template.findAll(cypherQuery, parameters, Person.class))
				.expectNextCount(7)
				.verifyComplete();
	}

	/**
	 * This tests checks whether all nodes that fit a certain class along a path are mapped correctly.
	 *
	 * @param template Used for querying
	 */
	@Test // DATAGRAPH-1437
	void pathMappingWithoutAdditionalInformationShouldWork(@Autowired ReactiveNeo4jTemplate template) {

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("person1", "Kevin Bacon");
		parameters.put("person2", "Angela Scope");
		parameters.put("requiredMovie", "The Da Vinci Code");
		String cypherQuery =
				"MATCH p=shortestPath((p1:Person {name: $person1})-[*]-(p2:Person {name: $person2}))\n"
				+ "WHERE size([n IN nodes(p) WHERE n.title = $requiredMovie]) > 0\n"
				+ "RETURN p";
		StepVerifier.create(template.findAll(cypherQuery, parameters, Person.class))
				.recordWith(ArrayList::new)
				.expectNextCount(4)
				.consumeRecordedWith(people -> {
					assertThat(people)
							.hasSize(4)
							.extracting(Person::getName)
							.contains("Kevin Bacon", "Jessica Thompson",
									"Angela Scope"); // Two paths lead there, one with Ron Howard, one with Tom Hanks.
					assertThat(people).element(2).extracting(Person::getReviewed)
							.satisfies(
									movies -> assertThat(movies).extracting(Movie::getTitle).containsExactly("The Da Vinci Code"));
				})
				.verifyComplete();
	}

	/**
	 * This tests checks whether all nodes that fit a certain class along a path are mapped correctly and if the
	 * additional joined information is applied as well.
	 *
	 * @param template Used for querying
	 */
	@Test // DATAGRAPH-1437
	void pathMappingWithAdditionalInformationShouldWork(@Autowired ReactiveNeo4jTemplate template) {
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
		StepVerifier.create(template.findAll(cypherQuery, parameters, Movie.class))
				.recordWith(ArrayList::new)
				.expectNextCount(2)
				.consumeRecordedWith(movies ->
						assertThat(movies)
							.hasSize(2)
							.allSatisfy(m -> assertThat(m.getDirectors()).isNotEmpty())
							.first()
							.satisfies(m -> assertThat(m.getDirectors()).extracting(Person::getName)
									.containsAnyOf("Ron Howard", "Rob Reiner")))
				.verifyComplete();
	}

	/**
	 * This tests checks if the result of a custom path based query will get mapped correctly to an instance
	 * of the defined type instead of a collection.
	 */
	@Test // DATAGRAPH-2107
	void customPathMappingResultsInScalarResultIfDefined(@Autowired MovieRepository movieRepository) {
		StepVerifier.create(movieRepository.customPathQueryMovieFind("The Matrix Revolutions"))
				.assertNext(movie -> {
					assertThat(movie).isNotNull();
					assertThat(movie.getActors()).hasSize(5);
				})
				.verifyComplete();
	}

	/**
	 * This tests checks if the result of a custom path based query will get mapped correctly to a collection
	 * of the defined type with all the fields hydrated.
	 */
	@Test // DATAGRAPH-2109
	void customPathMappingCollectionResultsInHydratedEntities(@Autowired MovieRepository movieRepository) {
		StepVerifier.create(movieRepository.customPathQueryMoviesFind("The Matrix Revolutions"))
				.assertNext(movie -> assertThat(movie.getActors()).hasSize(5))
				.verifyComplete();
	}

	@Test // GH-2117 updated for GH-2320
	void cyclicRelationshipsShouldHydrateCorrectlyProjectionsWithProjections(@Autowired MovieRepository movieRepository) {

		// The movie domain is a good fit for this test
		// as the cyclic dependencies is pretty slow to retrieve from Neo4j
		// this does OOM in most setups.
		StepVerifier.create(movieRepository.findProjectionWithProjectionByTitle("The Matrix"))
				.assertNext(projection -> {
					assertThat(projection.getTitle()).isNotNull();
					assertThat(projection.getActors()).extracting("person").extracting("name")
							.containsExactlyInAnyOrder("Gloria Foster", "Keanu Reeves", "Emil Eifrem", "Laurence Fishburne",
									"Carrie-Anne Moss", "Hugo Weaving");
					assertThat(projection.getActors()).flatExtracting("roles")
							.containsExactlyInAnyOrder("The Oracle", "Morpheus", "Trinity", "Agent Smith", "Emil", "Neo");

					// second level mapping of entity cycle
					assertThat(projection.getActors()).extracting("person")
							.allMatch(person ->
									!((MovieProjectionWithActorProjection.ActorProjection.PersonProjection) person).getActedIn().isEmpty());

					// n+1 level mapping of entity cycle
					assertThat(projection.getActors()).extracting("person").flatExtracting("actedIn").extracting("directors")
							.allMatch(directors -> !((Collection<?>) directors).isEmpty());
				})
				.verifyComplete();
	}

	@Test // GH-2320
	void projectDirectCycleProjectionReference(@Autowired MovieRepository movieRepository) {
		StepVerifier.create(movieRepository.findProjectionByTitleAndDescription("The Matrix",
				"Welcome to the Real World"))
				.assertNext(movie -> {
					assertThat(movie.getSequel().getTitle()).isEqualTo("The Matrix Reloaded");
					assertThat(movie.getSequel().getSequel().getTitle()).isEqualTo("The Matrix Revolutions");
				})
				.verifyComplete();
	}

	@Test // GH-2320
	void projectDirectCycleEntityReference(@Autowired MovieRepository movieRepository) {
		StepVerifier.create(movieRepository.findByTitleAndDescription("The Matrix", "Welcome to the Real World"))
				.assertNext(movie -> {

					Movie firstSequel = movie.getSequel();
					assertThat(firstSequel.getTitle()).isEqualTo("The Matrix Reloaded");
					assertThat(firstSequel.getActors()).isNotEmpty();

					Movie secondSequel = firstSequel.getSequel();
					assertThat(secondSequel.getTitle()).isEqualTo("The Matrix Revolutions");
					assertThat(secondSequel.getActors()).isNotEmpty();
				})
				.verifyComplete();
	}

	@Configuration
	@EnableTransactionManagement
	@EnableReactiveNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends AbstractReactiveNeo4jConfig {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Override
		public ReactiveTransactionManager reactiveTransactionManager(Driver driver, ReactiveDatabaseSelectionProvider databaseSelectionProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new ReactiveNeo4jTransactionManager(driver, databaseSelectionProvider, Neo4jBookmarkManager.create(bookmarkCapture));
		}
	}
}

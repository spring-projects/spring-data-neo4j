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
package org.springframework.data.neo4j.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.hamcrest.MockitoHamcrest;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.driver.Bookmark;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Values;
import org.neo4j.driver.reactive.RxResult;
import org.neo4j.driver.reactive.RxSession;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Michael J. Simons
 */
@ExtendWith(MockitoExtension.class)
class ReactiveNeo4jClientTest {

	@Mock private Driver driver;

	@Mock private TypeSystem typeSystem;

	private ArgumentCaptor<SessionConfig> configArgumentCaptor = ArgumentCaptor.forClass(SessionConfig.class);

	@Mock private RxSession session;

	@Mock private RxResult result;

	@Mock private ResultSummary resultSummary;

	@Mock private Record record1;

	@Mock private Record record2;

	void prepareMocks() {

		when(driver.defaultTypeSystem()).thenReturn(typeSystem);

		when(driver.rxSession(any(SessionConfig.class))).thenReturn(session);

		when(session.lastBookmark()).thenReturn(Mockito.mock(Bookmark.class));
		when(session.close()).thenReturn(Mono.empty());
	}

	@AfterEach
	void verifyNoMoreInteractionsWithMocks() {
		verifyNoMoreInteractions(driver, session, result, resultSummary, record1, record2);
	}

	@Test // GH-2426
	void databaseSelectionShouldWorkBeforeAsUser() {

		assumeThat(Neo4jTransactionUtils.driverSupportsImpersonation()).isTrue();

		prepareMocks();

		when(session.run(anyString(), anyMap())).thenReturn(result);
		when(result.records()).thenReturn(Flux.just(record1, record2));
		when(result.consume()).thenReturn(Mono.just(resultSummary));

		ReactiveNeo4jClient client = ReactiveNeo4jClient.create(driver);

		String cypher = "MATCH (u:User) WHERE u.name =~ $name";
		Mono<Map<String, Object>> firstMatchingUser = client
				.query(cypher)
				.in("bikingDatabase")
				.asUser("aUser")
				.bind("Someone.*")
				.to("name")
				.fetch().first();

		StepVerifier.create(firstMatchingUser).expectNextCount(1L).verifyComplete();

		verifyDatabaseSelection("bikingDatabase");
		verifyUserSelection("aUser");

		Map<String, Object> expectedParameters = new HashMap<>();
		expectedParameters.put("name", "Someone.*");

		verify(session).run(eq(cypher), MockitoHamcrest.argThat(new Neo4jClientTest.MapAssertionMatcher(expectedParameters)));
		verify(result).records();
		verify(result).consume();
		verify(resultSummary).notifications();
		verify(resultSummary).hasPlan();
		verify(record1).asMap();
		verify(session).close();
	}

	@Test // GH-2426
	void databaseSelectionShouldWorkAfterAsUser() {

		assumeThat(Neo4jTransactionUtils.driverSupportsImpersonation()).isTrue();

		prepareMocks();

		when(session.run(anyString(), anyMap())).thenReturn(result);
		when(result.records()).thenReturn(Flux.just(record1, record2));
		when(result.consume()).thenReturn(Mono.just(resultSummary));

		ReactiveNeo4jClient client = ReactiveNeo4jClient.create(driver);

		String cypher = "MATCH (u:User) WHERE u.name =~ $name";
		Mono<Map<String, Object>> firstMatchingUser = client
				.query(cypher)
				.asUser("aUser")
				.in("bikingDatabase")
				.bind("Someone.*")
				.to("name")
				.fetch().first();

		StepVerifier.create(firstMatchingUser).expectNextCount(1L).verifyComplete();

		verifyDatabaseSelection("bikingDatabase");
		verifyUserSelection("aUser");

		Map<String, Object> expectedParameters = new HashMap<>();
		expectedParameters.put("name", "Someone.*");

		verify(session).run(eq(cypher), MockitoHamcrest.argThat(new Neo4jClientTest.MapAssertionMatcher(expectedParameters)));
		verify(result).records();
		verify(result).consume();
		verify(resultSummary).notifications();
		verify(resultSummary).hasPlan();
		verify(record1).asMap();
		verify(session).close();
	}

	@Test // GH-2426
	void userSelectionShouldWork() {

		assumeThat(Neo4jTransactionUtils.driverSupportsImpersonation()).isTrue();

		prepareMocks();

		when(session.run(anyString(), anyMap())).thenReturn(result);
		when(result.records()).thenReturn(Flux.just(record1, record2));
		when(result.consume()).thenReturn(Mono.just(resultSummary));

		ReactiveNeo4jClient client = ReactiveNeo4jClient.create(driver);

		String cypher = "MATCH (u:User) WHERE u.name =~ $name";
		Mono<Map<String, Object>> firstMatchingUser = client
				.query(cypher)
				.asUser("aUser")
				.bind("Someone.*")
				.to("name")
				.fetch().first();

		StepVerifier.create(firstMatchingUser).expectNextCount(1L).verifyComplete();

		verifyDatabaseSelection(null);
		verifyUserSelection("aUser");

		Map<String, Object> expectedParameters = new HashMap<>();
		expectedParameters.put("name", "Someone.*");

		verify(session).run(eq(cypher), MockitoHamcrest.argThat(new Neo4jClientTest.MapAssertionMatcher(expectedParameters)));
		verify(result).records();
		verify(result).consume();
		verify(resultSummary).notifications();
		verify(resultSummary).hasPlan();
		verify(record1).asMap();
		verify(session).close();
	}

	@Test
	@DisplayName("Creation of queries and binding parameters should feel natural")
	void queryCreationShouldFeelGood() {

		prepareMocks();

		when(session.run(anyString(), anyMap())).thenReturn(result);
		when(result.records()).thenReturn(Flux.just(record1, record2));
		when(result.consume()).thenReturn(Mono.just(resultSummary));

		ReactiveNeo4jClient client = ReactiveNeo4jClient.create(driver);

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("bikeName", "M.*");
		parameters.put("location", "Sweden");

		String cypher = """
			MATCH (o:User {name: $name}) - [:OWNS] -> (b:Bike) - [:USED_ON] -> (t:Trip)
			WHERE t.takenOn > $aDate
			AND b.name =~ $bikeName
			AND t.location = $location RETURN b
			""";

		Flux<Map<String, Object>> usedBikes = client.query(cypher).bind("michael").to("name").bindAll(parameters)
				.bind(LocalDate.of(2019, 1, 1)).to("aDate").fetch().all();

		StepVerifier.create(usedBikes).expectNextCount(2L).verifyComplete();

		verifyDatabaseSelection(null);

		Map<String, Object> expectedParameters = new HashMap<>();
		expectedParameters.putAll(parameters);
		expectedParameters.put("name", "michael");
		expectedParameters.put("aDate", LocalDate.of(2019, 1, 1));
		verify(session).run(eq(cypher), MockitoHamcrest.argThat(new Neo4jClientTest.MapAssertionMatcher(expectedParameters)));

		verify(result).records();
		verify(result).consume();
		verify(resultSummary).notifications();
		verify(resultSummary).hasPlan();
		verify(record1).asMap();
		verify(record2).asMap();
		verify(session).close();
	}

	@Test
	void databaseSelectionShouldBePossibleOnlyOnce() {

		prepareMocks();

		when(session.run(anyString(), anyMap())).thenReturn(result);
		when(result.records()).thenReturn(Flux.just(record1, record2));
		when(result.consume()).thenReturn(Mono.just(resultSummary));

		ReactiveNeo4jClient client = ReactiveNeo4jClient.create(driver);

		String cypher = "MATCH (u:User) WHERE u.name =~ $name";
		Mono<Map<String, Object>> firstMatchingUser = client.query(cypher).in("bikingDatabase").bind("Someone.*").to("name")
				.fetch().first();

		StepVerifier.create(firstMatchingUser).expectNextCount(1L).verifyComplete();

		verifyDatabaseSelection("bikingDatabase");

		Map<String, Object> expectedParameters = new HashMap<>();
		expectedParameters.put("name", "Someone.*");

		verify(session).run(eq(cypher), MockitoHamcrest.argThat(new Neo4jClientTest.MapAssertionMatcher(expectedParameters)));
		verify(result).records();
		verify(result).consume();
		verify(resultSummary).notifications();
		verify(resultSummary).hasPlan();
		verify(record1).asMap();
		verify(session).close();
	}

	@Test
	void databaseSelectionShouldPreventIllegalValues() {

		ReactiveNeo4jClient client = ReactiveNeo4jClient.create(driver);

		assertThat(client.query("RETURN 1").in(null)).isNotNull();
		assertThat(client.query("RETURN 1").in("foobar")).isNotNull();

		String[] invalidDatabaseNames = { "", " ", "\t" };
		for (String invalidDatabaseName : invalidDatabaseNames) {
			assertThatIllegalArgumentException()
					.isThrownBy(() -> client.delegateTo(r -> Mono.empty()).in(invalidDatabaseName));
		}

		for (String invalidDatabaseName : invalidDatabaseNames) {
			assertThatIllegalArgumentException().isThrownBy(() -> client.query("RETURN 1").in(invalidDatabaseName));
		}

		verify(driver).defaultTypeSystem();
	}

	@Test // GH-2159
	void databaseSelectionBeanShouldGetRespectedIfExisting() {

		prepareMocks();

		when(session.run(anyString(), anyMap())).thenReturn(result);
		when(result.records()).thenReturn(Flux.just(record1, record2));
		when(result.consume()).thenReturn(Mono.just(resultSummary));

		String databaseName = "customDatabaseSelection";
		String cypher = "RETURN 1";
		ReactiveDatabaseSelectionProvider databaseSelection = ReactiveDatabaseSelectionProvider
				.createStaticDatabaseSelectionProvider(databaseName);


		ReactiveNeo4jClient client = ReactiveNeo4jClient.create(driver, databaseSelection);

		StepVerifier.create(client.query(cypher).fetch().first())
				.expectNextCount(1L)
				.verifyComplete();

		verifyDatabaseSelection(databaseName);

		verify(session).run(eq(cypher), anyMap());
		verify(result).records();
		verify(result).consume();
		verify(resultSummary).notifications();
		verify(resultSummary).hasPlan();
		verify(record1).asMap();
		verify(session).close();
	}

	@Test // GH-2369
	void databaseSelectionShouldBePropagatedToDelegate() {

		prepareMocks();

		String databaseName = "aDatabase";
		ReactiveDatabaseSelectionProvider databaseSelection = ReactiveDatabaseSelectionProvider
				.createStaticDatabaseSelectionProvider(databaseName);
		ReactiveNeo4jClient client = ReactiveNeo4jClient.create(driver, databaseSelection);
		Mono<Integer> singleResult = client.delegateTo(runner -> Mono.just(21)).run();

		StepVerifier.create(singleResult).expectNext(21).verifyComplete();

		verifyDatabaseSelection("aDatabase");

		verify(session).close();
	}

	@Nested
	@DisplayName("Callback handling should feel good")
	class CallbackHandlingShouldFeelGood {

		@Test
		void withDefaultDatabase() {

			prepareMocks();

			ReactiveNeo4jClient client = ReactiveNeo4jClient.create(driver);
			Mono<Integer> singleResult = client.delegateTo(runner -> Mono.just(21)).run();

			StepVerifier.create(singleResult).expectNext(21).verifyComplete();

			verifyDatabaseSelection(null);

			verify(session).close();
		}

		@Test
		void withDatabase() {

			prepareMocks();

			ReactiveNeo4jClient client = ReactiveNeo4jClient.create(driver);
			Mono<Integer> singleResult = client.delegateTo(runner -> Mono.just(21)).in("aDatabase").run();

			StepVerifier.create(singleResult).expectNext(21).verifyComplete();

			verifyDatabaseSelection("aDatabase");

			verify(session).close();
		}
	}

	@Nested
	@DisplayName("Mapping should feel good")
	class MappingShouldFeelGood {

		@Test
		void reading() {

			prepareMocks();

			when(session.run(anyString(), anyMap())).thenReturn(result);
			when(result.records()).thenReturn(Flux.just(record1));
			when(result.consume()).thenReturn(Mono.just(resultSummary));
			when(record1.get("name")).thenReturn(Values.value("michael"));

			ReactiveNeo4jClient client = ReactiveNeo4jClient.create(driver);

			String cypher = "MATCH (o:User {name: $name}) - [:OWNS] -> (b:Bike) RETURN o, collect(b) as bikes";

			Neo4jClientTest.BikeOwnerReader mappingFunction = new Neo4jClientTest.BikeOwnerReader();
			Flux<Neo4jClientTest.BikeOwner> bikeOwners = client.query(cypher).bind("michael").to("name")
					.fetchAs(Neo4jClientTest.BikeOwner.class).mappedBy(mappingFunction).all();

			StepVerifier.create(bikeOwners).expectNextMatches(o -> o.getName().equals("michael")).verifyComplete();

			verifyDatabaseSelection(null);

			Map<String, Object> expectedParameters = new HashMap<>();
			expectedParameters.put("name", "michael");

			verify(session).run(eq(cypher), MockitoHamcrest.argThat(new Neo4jClientTest.MapAssertionMatcher(expectedParameters)));
			verify(result).records();
			verify(resultSummary).notifications();
			verify(resultSummary).hasPlan();
			verify(record1).get("name");
			verify(session).close();
		}

		@Test
		void shouldApplyNullChecksDuringReading() {

			prepareMocks();

			when(session.run(anyString(), anyMap())).thenReturn(result);
			when(result.records()).thenReturn(Flux.just(record1, record2));
			when(result.consume()).thenReturn(Mono.just(resultSummary));
			when(record1.get("name")).thenReturn(Values.value("michael"));

			ReactiveNeo4jClient client = ReactiveNeo4jClient.create(driver);
			Flux<Neo4jClientTest.BikeOwner> bikeOwners = client.query("MATCH (n) RETURN n")
					.fetchAs(Neo4jClientTest.BikeOwner.class).mappedBy((t, r) -> {
						if (r == record1) {
							return new Neo4jClientTest.BikeOwner(r.get("name").asString(), Collections.emptyList());
						} else {
							return null;
						}
					}).all();

			StepVerifier.create(bikeOwners).expectNextCount(1).verifyComplete();

			verifyDatabaseSelection(null);

			verify(session).run(eq("MATCH (n) RETURN n"),
					MockitoHamcrest.argThat(new Neo4jClientTest.MapAssertionMatcher(Collections.emptyMap())));
			verify(result).records();
			verify(resultSummary).notifications();
			verify(resultSummary).hasPlan();
			verify(record1).get("name");
			verify(session).close();
		}

		@Test
		void writing() {

			prepareMocks();

			when(session.run(anyString(), anyMap())).thenReturn(result);
			when(result.records()).thenReturn(Flux.empty());
			when(result.consume()).thenReturn(Mono.just(resultSummary));

			ReactiveNeo4jClient client = ReactiveNeo4jClient.create(driver);

			Neo4jClientTest.BikeOwner michael = new Neo4jClientTest.BikeOwner("Michael",
					Arrays.asList(new Neo4jClientTest.Bike("Road"), new Neo4jClientTest.Bike("MTB")));
			String cypher = "MERGE (u:User {name: 'Michael'}) WITH u UNWIND $bikes as bike MERGE (b:Bike {name: bike}) MERGE (u) - [o:OWNS] -> (b) ";

			Mono<ResultSummary> summary = client.query(cypher).bind(michael).with(new Neo4jClientTest.BikeOwnerBinder())
					.run();

			StepVerifier.create(summary).expectNext(resultSummary).verifyComplete();

			verifyDatabaseSelection(null);

			Map<String, Object> expectedParameters = new HashMap<>();
			expectedParameters.put("name", "Michael");

			verify(session).run(eq(cypher), MockitoHamcrest.argThat(new Neo4jClientTest.MapAssertionMatcher(expectedParameters)));
			verify(result).consume();
			verify(resultSummary).notifications();
			verify(resultSummary).hasPlan();
			verify(session).close();
		}

		@Test
		@DisplayName("Some automatic conversion is ok")
		void automaticConversion() {

			prepareMocks();

			when(session.run(anyString(), anyMap())).thenReturn(result);
			when(result.records()).thenReturn(Flux.just(record1));
			when(result.consume()).thenReturn(Mono.just(resultSummary));
			when(record1.size()).thenReturn(1);
			when(record1.get(0)).thenReturn(Values.value(23L));

			ReactiveNeo4jClient client = ReactiveNeo4jClient.create(driver);

			String cypher = "MATCH (b:Bike) RETURN count(b)";
			Mono<Long> numberOfBikes = client.query(cypher).fetchAs(Long.class).one();

			StepVerifier.create(numberOfBikes).expectNext(23L).verifyComplete();

			verifyDatabaseSelection(null);

			verify(result).consume();
			verify(resultSummary).notifications();
			verify(resultSummary).hasPlan();
			verify(session).run(eq(cypher), anyMap());
			verify(session).close();
		}
	}

	@Test
	@DisplayName("Queries that return nothing should fit in")
	void queriesWithoutResultShouldFitInAsWell() {

		prepareMocks();

		when(session.run(anyString(), anyMap())).thenReturn(result);
		when(result.records()).thenReturn(Flux.empty());
		when(result.consume()).thenReturn(Mono.just(resultSummary));

		ReactiveNeo4jClient client = ReactiveNeo4jClient.create(driver);

		String cypher = "DETACH DELETE (b) WHERE name = $name";

		Mono<ResultSummary> deletionResult = client.query(cypher).bind("fixie").to("name").run();

		StepVerifier.create(deletionResult).expectNext(resultSummary).verifyComplete();

		verifyDatabaseSelection(null);

		Map<String, Object> expectedParameters = new HashMap<>();
		expectedParameters.put("name", "fixie");

		verify(session).run(eq(cypher), MockitoHamcrest.argThat(new Neo4jClientTest.MapAssertionMatcher(expectedParameters)));
		verify(result).consume();
		verify(resultSummary).notifications();
		verify(resultSummary).hasPlan();
		verify(session).close();
	}

	void verifyDatabaseSelection(@Nullable String targetDatabase) {

		verify(driver).rxSession(configArgumentCaptor.capture());
		SessionConfig config = configArgumentCaptor.getValue();

		if (targetDatabase != null) {
			assertThat(config.database()).isPresent().contains(targetDatabase);
		} else {
			assertThat(config.database()).isEmpty();
		}
	}

	void verifyUserSelection(@Nullable String aUser) {

		verify(driver).rxSession(configArgumentCaptor.capture());
		SessionConfig config = configArgumentCaptor.getValue();

		// We assume the driver supports this before the test
		final Method impersonatedUser = ReflectionUtils.findMethod(SessionConfig.class, "impersonatedUser");
		if (aUser != null) {
			Optional<String> optionalValue = (Optional<String>) ReflectionUtils.invokeMethod(impersonatedUser, config);
			assertThat(optionalValue).isPresent().contains(aUser);
		} else {
			assertThat(config.database()).isEmpty();
		}
	}
}

/*
 * Copyright 2011-2025 the original author or authors.
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

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
import org.neo4j.driver.reactivestreams.ReactiveResult;
import org.neo4j.driver.reactivestreams.ReactiveSession;
import org.neo4j.driver.summary.ResultSummary;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import org.springframework.data.neo4j.core.transaction.Neo4jTransactionUtils;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author Michael J. Simons
 */
@ExtendWith(MockitoExtension.class)
class ReactiveNeo4jClientTests {

	@Mock
	private Driver driver;

	private ArgumentCaptor<SessionConfig> configArgumentCaptor = ArgumentCaptor.forClass(SessionConfig.class);

	@Mock
	private ReactiveSession session;

	@Mock
	private ReactiveResult result;

	@Mock
	private ResultSummary resultSummary;

	@Mock
	private Record record1;

	@Mock
	private Record record2;

	void prepareMocks() {

		given(this.driver.session(eq(ReactiveSession.class), any(SessionConfig.class))).willReturn(this.session);

		given(this.session.lastBookmarks()).willReturn(Set.of(Mockito.mock(Bookmark.class)));
		given(this.session.close()).willReturn(Mono.empty());
	}

	@AfterEach
	void verifyNoMoreInteractionsWithMocks() {
		verifyNoMoreInteractions(this.driver, this.session, this.result, this.resultSummary, this.record1,
				this.record2);
	}

	@Test // GH-2426
	void databaseSelectionShouldWorkBeforeAsUser() {

		assumeThat(Neo4jTransactionUtils.driverSupportsImpersonation()).isTrue();

		prepareMocks();

		given(this.session.run(anyString(), anyMap())).willReturn(Mono.just(this.result));
		given(this.result.records()).willReturn(Flux.just(this.record1, this.record2).publishOn(Schedulers.single()));
		given(this.result.consume()).willReturn(Mono.just(this.resultSummary));

		ReactiveNeo4jClient client = ReactiveNeo4jClient.create(this.driver);

		String cypher = "MATCH (u:User) WHERE u.name =~ $name";
		Mono<Map<String, Object>> firstMatchingUser = client.query(cypher)
			.in("bikingDatabase")
			.asUser("aUser")
			.bind("Someone.*")
			.to("name")
			.fetch()
			.first();

		StepVerifier.create(firstMatchingUser).expectNextCount(1L).verifyComplete();

		verifyDatabaseSelection("bikingDatabase");
		verifyUserSelection("aUser");

		Map<String, Object> expectedParameters = new HashMap<>();
		expectedParameters.put("name", "Someone.*");

		verify(this.session).run(eq(cypher),
				MockitoHamcrest.argThat(new Neo4jClientTests.MapAssertionMatcher(expectedParameters)));
		verify(this.result).records();
		verify(this.result).consume();
		verify(this.resultSummary).gqlStatusObjects();
		verify(this.resultSummary).hasPlan();
		verify(this.record1).asMap();
		verify(this.session).close();
	}

	@Test // GH-2426
	void databaseSelectionShouldWorkAfterAsUser() {

		assumeThat(Neo4jTransactionUtils.driverSupportsImpersonation()).isTrue();

		prepareMocks();

		given(this.session.run(anyString(), anyMap())).willReturn(Mono.just(this.result));
		given(this.result.records()).willReturn(Flux.just(this.record1, this.record2).publishOn(Schedulers.single()));
		given(this.result.consume()).willReturn(Mono.just(this.resultSummary));

		ReactiveNeo4jClient client = ReactiveNeo4jClient.create(this.driver);

		String cypher = "MATCH (u:User) WHERE u.name =~ $name";
		Mono<Map<String, Object>> firstMatchingUser = client.query(cypher)
			.asUser("aUser")
			.in("bikingDatabase")
			.bind("Someone.*")
			.to("name")
			.fetch()
			.first();

		StepVerifier.create(firstMatchingUser).expectNextCount(1L).verifyComplete();

		verifyDatabaseSelection("bikingDatabase");
		verifyUserSelection("aUser");

		Map<String, Object> expectedParameters = new HashMap<>();
		expectedParameters.put("name", "Someone.*");

		verify(this.session).run(eq(cypher),
				MockitoHamcrest.argThat(new Neo4jClientTests.MapAssertionMatcher(expectedParameters)));
		verify(this.result).records();
		verify(this.result).consume();
		verify(this.resultSummary).gqlStatusObjects();
		verify(this.resultSummary).hasPlan();
		verify(this.record1).asMap();
		verify(this.session).close();
	}

	@Test // GH-2426
	void userSelectionShouldWork() {

		assumeThat(Neo4jTransactionUtils.driverSupportsImpersonation()).isTrue();

		prepareMocks();

		given(this.session.run(anyString(), anyMap())).willReturn(Mono.just(this.result));
		given(this.result.records()).willReturn(Flux.just(this.record1, this.record2).publishOn(Schedulers.single()));
		given(this.result.consume()).willReturn(Mono.just(this.resultSummary));

		ReactiveNeo4jClient client = ReactiveNeo4jClient.create(this.driver);

		String cypher = "MATCH (u:User) WHERE u.name =~ $name";
		Mono<Map<String, Object>> firstMatchingUser = client.query(cypher)
			.asUser("aUser")
			.bind("Someone.*")
			.to("name")
			.fetch()
			.first();

		StepVerifier.create(firstMatchingUser).expectNextCount(1L).verifyComplete();

		verifyDatabaseSelection(null);
		verifyUserSelection("aUser");

		Map<String, Object> expectedParameters = new HashMap<>();
		expectedParameters.put("name", "Someone.*");

		verify(this.session).run(eq(cypher),
				MockitoHamcrest.argThat(new Neo4jClientTests.MapAssertionMatcher(expectedParameters)));
		verify(this.result).records();
		verify(this.result).consume();
		verify(this.resultSummary).gqlStatusObjects();
		verify(this.resultSummary).hasPlan();
		verify(this.record1).asMap();
		verify(this.session).close();
	}

	@Test
	@DisplayName("Creation of queries and binding parameters should feel natural")
	void queryCreationShouldFeelGood() {

		prepareMocks();

		given(this.session.run(anyString(), anyMap())).willReturn(Mono.just(this.result));
		given(this.result.records()).willReturn(Flux.just(this.record1, this.record2));
		given(this.result.consume()).willReturn(Mono.just(this.resultSummary));

		ReactiveNeo4jClient client = ReactiveNeo4jClient.create(this.driver);

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("bikeName", "M.*");
		parameters.put("location", "Sweden");

		String cypher = """
				MATCH (o:User {name: $name}) - [:OWNS] -> (b:Bike) - [:USED_ON] -> (t:Trip)
				WHERE t.takenOn > $aDate
				AND b.name =~ $bikeName
				AND t.location = $location RETURN b
				""";

		Flux<Map<String, Object>> usedBikes = client.query(cypher)
			.bind("michael")
			.to("name")
			.bindAll(parameters)
			.bind(LocalDate.of(2019, 1, 1))
			.to("aDate")
			.fetch()
			.all();

		StepVerifier.create(usedBikes).expectNextCount(2L).verifyComplete();

		verifyDatabaseSelection(null);

		Map<String, Object> expectedParameters = new HashMap<>();
		expectedParameters.putAll(parameters);
		expectedParameters.put("name", "michael");
		expectedParameters.put("aDate", LocalDate.of(2019, 1, 1));
		verify(this.session).run(eq(cypher),
				MockitoHamcrest.argThat(new Neo4jClientTests.MapAssertionMatcher(expectedParameters)));

		verify(this.result).records();
		verify(this.result).consume();
		verify(this.resultSummary).gqlStatusObjects();
		verify(this.resultSummary).hasPlan();
		verify(this.record1).asMap();
		verify(this.record2).asMap();
		verify(this.session).close();
	}

	@Test
	void databaseSelectionShouldBePossibleOnlyOnce() {

		prepareMocks();

		given(this.session.run(anyString(), anyMap())).willReturn(Mono.just(this.result));
		given(this.result.records()).willReturn(Flux.just(this.record1, this.record2).publishOn(Schedulers.single()));
		given(this.result.consume()).willReturn(Mono.just(this.resultSummary));

		ReactiveNeo4jClient client = ReactiveNeo4jClient.create(this.driver);

		String cypher = "MATCH (u:User) WHERE u.name =~ $name";
		Mono<Map<String, Object>> firstMatchingUser = client.query(cypher)
			.in("bikingDatabase")
			.bind("Someone.*")
			.to("name")
			.fetch()
			.first();

		StepVerifier.create(firstMatchingUser).expectNextCount(1L).verifyComplete();

		verifyDatabaseSelection("bikingDatabase");

		Map<String, Object> expectedParameters = new HashMap<>();
		expectedParameters.put("name", "Someone.*");

		verify(this.session).run(eq(cypher),
				MockitoHamcrest.argThat(new Neo4jClientTests.MapAssertionMatcher(expectedParameters)));
		verify(this.result).records();
		verify(this.result).consume();
		verify(this.resultSummary).gqlStatusObjects();
		verify(this.resultSummary).hasPlan();
		verify(this.record1).asMap();
		verify(this.session).close();
	}

	@Test
	void databaseSelectionShouldPreventIllegalValues() {

		ReactiveNeo4jClient client = ReactiveNeo4jClient.create(this.driver);

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
	}

	@Test // GH-2159
	void databaseSelectionBeanShouldGetRespectedIfExisting() {

		prepareMocks();

		given(this.session.run(anyString(), anyMap())).willReturn(Mono.just(this.result));
		given(this.result.records()).willReturn(Flux.just(this.record1, this.record2).publishOn(Schedulers.single()));
		given(this.result.consume()).willReturn(Mono.just(this.resultSummary));

		String databaseName = "customDatabaseSelection";
		String cypher = "RETURN 1";
		ReactiveDatabaseSelectionProvider databaseSelection = ReactiveDatabaseSelectionProvider
			.createStaticDatabaseSelectionProvider(databaseName);

		ReactiveNeo4jClient client = ReactiveNeo4jClient.create(this.driver, databaseSelection);

		StepVerifier.create(client.query(cypher).fetch().first()).expectNextCount(1L).verifyComplete();

		verifyDatabaseSelection(databaseName);

		verify(this.session).run(eq(cypher), anyMap());
		verify(this.result).records();
		verify(this.result).consume();
		verify(this.resultSummary).gqlStatusObjects();
		verify(this.resultSummary).hasPlan();
		verify(this.record1).asMap();
		verify(this.session).close();
	}

	@Test // GH-2369
	void databaseSelectionShouldBePropagatedToDelegate() {

		prepareMocks();

		String databaseName = "aDatabase";
		ReactiveDatabaseSelectionProvider databaseSelection = ReactiveDatabaseSelectionProvider
			.createStaticDatabaseSelectionProvider(databaseName);
		ReactiveNeo4jClient client = ReactiveNeo4jClient.create(this.driver, databaseSelection);
		Mono<Integer> singleResult = client.delegateTo(runner -> Mono.just(21)).run();

		StepVerifier.create(singleResult).expectNext(21).verifyComplete();

		verifyDatabaseSelection("aDatabase");

		verify(this.session).close();
	}

	@Test
	@DisplayName("Queries that return nothing should fit in")
	void queriesWithoutResultShouldFitInAsWell() {

		prepareMocks();

		given(this.session.run(anyString(), anyMap())).willReturn(Mono.just(this.result));
		given(this.result.consume()).willReturn(Mono.just(this.resultSummary));

		ReactiveNeo4jClient client = ReactiveNeo4jClient.create(this.driver);

		String cypher = "DETACH DELETE (b) WHERE name = $name";

		Mono<ResultSummary> deletionResult = client.query(cypher).bind("fixie").to("name").run();

		StepVerifier.create(deletionResult).expectNext(this.resultSummary).verifyComplete();

		verifyDatabaseSelection(null);

		Map<String, Object> expectedParameters = new HashMap<>();
		expectedParameters.put("name", "fixie");

		verify(this.session).run(eq(cypher),
				MockitoHamcrest.argThat(new Neo4jClientTests.MapAssertionMatcher(expectedParameters)));
		verify(this.result).consume();
		verify(this.resultSummary).gqlStatusObjects();
		verify(this.resultSummary).hasPlan();
		verify(this.session).close();
	}

	void verifyDatabaseSelection(String targetDatabase) {

		verify(this.driver).session(eq(ReactiveSession.class), this.configArgumentCaptor.capture());
		SessionConfig config = this.configArgumentCaptor.getValue();

		if (targetDatabase != null) {
			assertThat(config.database()).isPresent().contains(targetDatabase);
		}
		else {
			assertThat(config.database()).isEmpty();
		}
	}

	void verifyUserSelection(String aUser) {

		verify(this.driver).session(eq(ReactiveSession.class), this.configArgumentCaptor.capture());
		SessionConfig config = this.configArgumentCaptor.getValue();

		// We assume the driver supports this before the test
		final Method impersonatedUser = ReflectionUtils.findMethod(SessionConfig.class, "impersonatedUser");
		if (aUser != null) {
			Optional<String> optionalValue = (Optional<String>) ReflectionUtils.invokeMethod(impersonatedUser, config);
			assertThat(optionalValue).isPresent().contains(aUser);
		}
		else {
			assertThat(config.database()).isEmpty();
		}
	}

	@Nested
	@DisplayName("Callback handling should feel good")
	class CallbackHandlingShouldFeelGood {

		@Test
		void withDefaultDatabase() {

			prepareMocks();

			ReactiveNeo4jClient client = ReactiveNeo4jClient.create(ReactiveNeo4jClientTests.this.driver);
			Mono<Integer> singleResult = client.delegateTo(runner -> Mono.just(21)).run();

			StepVerifier.create(singleResult).expectNext(21).verifyComplete();

			verifyDatabaseSelection(null);

			verify(ReactiveNeo4jClientTests.this.session).close();
		}

		@Test
		void withDatabase() {

			prepareMocks();

			ReactiveNeo4jClient client = ReactiveNeo4jClient.create(ReactiveNeo4jClientTests.this.driver);
			Mono<Integer> singleResult = client.delegateTo(runner -> Mono.just(21)).in("aDatabase").run();

			StepVerifier.create(singleResult).expectNext(21).verifyComplete();

			verifyDatabaseSelection("aDatabase");

			verify(ReactiveNeo4jClientTests.this.session).close();
		}

	}

	@Nested
	@DisplayName("Mapping should feel good")
	class MappingShouldFeelGood {

		@Test
		void reading() {

			prepareMocks();

			given(ReactiveNeo4jClientTests.this.session.run(anyString(), anyMap()))
				.willReturn(Mono.just(ReactiveNeo4jClientTests.this.result));
			given(ReactiveNeo4jClientTests.this.result.records())
				.willReturn(Flux.just(ReactiveNeo4jClientTests.this.record1));
			given(ReactiveNeo4jClientTests.this.result.consume())
				.willReturn(Mono.just(ReactiveNeo4jClientTests.this.resultSummary));
			given(ReactiveNeo4jClientTests.this.record1.get("name")).willReturn(Values.value("michael"));

			ReactiveNeo4jClient client = ReactiveNeo4jClient.create(ReactiveNeo4jClientTests.this.driver);

			String cypher = "MATCH (o:User {name: $name}) - [:OWNS] -> (b:Bike) RETURN o, collect(b) as bikes";

			Neo4jClientTests.BikeOwnerReader mappingFunction = new Neo4jClientTests.BikeOwnerReader();
			Flux<Neo4jClientTests.BikeOwner> bikeOwners = client.query(cypher)
				.bind("michael")
				.to("name")
				.fetchAs(Neo4jClientTests.BikeOwner.class)
				.mappedBy(mappingFunction)
				.all();

			StepVerifier.create(bikeOwners).expectNextMatches(o -> o.getName().equals("michael")).verifyComplete();

			verifyDatabaseSelection(null);

			Map<String, Object> expectedParameters = new HashMap<>();
			expectedParameters.put("name", "michael");

			verify(ReactiveNeo4jClientTests.this.session).run(eq(cypher),
					MockitoHamcrest.argThat(new Neo4jClientTests.MapAssertionMatcher(expectedParameters)));
			verify(ReactiveNeo4jClientTests.this.result).records();
			verify(ReactiveNeo4jClientTests.this.resultSummary).gqlStatusObjects();
			verify(ReactiveNeo4jClientTests.this.resultSummary).hasPlan();
			verify(ReactiveNeo4jClientTests.this.record1).get("name");
			verify(ReactiveNeo4jClientTests.this.session).close();
		}

		@Test
		void shouldApplyNullChecksDuringReading() {

			prepareMocks();

			given(ReactiveNeo4jClientTests.this.session.run(anyString(), anyMap()))
				.willReturn(Mono.just(ReactiveNeo4jClientTests.this.result));
			given(ReactiveNeo4jClientTests.this.result.records())
				.willReturn(Flux.just(ReactiveNeo4jClientTests.this.record1, ReactiveNeo4jClientTests.this.record2));
			given(ReactiveNeo4jClientTests.this.result.consume())
				.willReturn(Mono.just(ReactiveNeo4jClientTests.this.resultSummary));
			given(ReactiveNeo4jClientTests.this.record1.get("name")).willReturn(Values.value("michael"));

			ReactiveNeo4jClient client = ReactiveNeo4jClient.create(ReactiveNeo4jClientTests.this.driver);
			Flux<Neo4jClientTests.BikeOwner> bikeOwners = client.query("MATCH (n) RETURN n")
				.fetchAs(Neo4jClientTests.BikeOwner.class)
				.mappedBy((t, r) -> {
					if (r == ReactiveNeo4jClientTests.this.record1) {
						return new Neo4jClientTests.BikeOwner(r.get("name").asString(), Collections.emptyList());
					}
					else {
						return null;
					}
				})
				.all();

			StepVerifier.create(bikeOwners).expectNextCount(1).verifyComplete();

			verifyDatabaseSelection(null);

			verify(ReactiveNeo4jClientTests.this.session).run(eq("MATCH (n) RETURN n"),
					MockitoHamcrest.argThat(new Neo4jClientTests.MapAssertionMatcher(Collections.emptyMap())));
			verify(ReactiveNeo4jClientTests.this.result).records();
			verify(ReactiveNeo4jClientTests.this.resultSummary).gqlStatusObjects();
			verify(ReactiveNeo4jClientTests.this.resultSummary).hasPlan();
			verify(ReactiveNeo4jClientTests.this.record1).get("name");
			verify(ReactiveNeo4jClientTests.this.session).close();
		}

		@Test
		void writing() {

			prepareMocks();

			given(ReactiveNeo4jClientTests.this.session.run(anyString(), anyMap()))
				.willReturn(Mono.just(ReactiveNeo4jClientTests.this.result));
			given(ReactiveNeo4jClientTests.this.result.consume())
				.willReturn(Mono.just(ReactiveNeo4jClientTests.this.resultSummary));

			ReactiveNeo4jClient client = ReactiveNeo4jClient.create(ReactiveNeo4jClientTests.this.driver);

			Neo4jClientTests.BikeOwner michael = new Neo4jClientTests.BikeOwner("Michael",
					Arrays.asList(new Neo4jClientTests.Bike("Road"), new Neo4jClientTests.Bike("MTB")));
			String cypher = "MERGE (u:User {name: 'Michael'}) WITH u UNWIND $bikes as bike MERGE (b:Bike {name: bike}) MERGE (u) - [o:OWNS] -> (b) ";

			Mono<ResultSummary> summary = client.query(cypher)
				.bind(michael)
				.with(new Neo4jClientTests.BikeOwnerBinder())
				.run();

			StepVerifier.create(summary).expectNext(ReactiveNeo4jClientTests.this.resultSummary).verifyComplete();

			verifyDatabaseSelection(null);

			Map<String, Object> expectedParameters = new HashMap<>();
			expectedParameters.put("name", "Michael");

			verify(ReactiveNeo4jClientTests.this.session).run(eq(cypher),
					MockitoHamcrest.argThat(new Neo4jClientTests.MapAssertionMatcher(expectedParameters)));
			verify(ReactiveNeo4jClientTests.this.result).consume();
			verify(ReactiveNeo4jClientTests.this.resultSummary).gqlStatusObjects();
			verify(ReactiveNeo4jClientTests.this.resultSummary).hasPlan();
			verify(ReactiveNeo4jClientTests.this.session).close();
		}

		@Test
		@DisplayName("Some automatic conversion is ok")
		void automaticConversion() {

			prepareMocks();

			given(ReactiveNeo4jClientTests.this.session.run(anyString(), anyMap()))
				.willReturn(Mono.just(ReactiveNeo4jClientTests.this.result));
			given(ReactiveNeo4jClientTests.this.result.records())
				.willReturn(Flux.just(ReactiveNeo4jClientTests.this.record1));
			given(ReactiveNeo4jClientTests.this.result.consume())
				.willReturn(Mono.just(ReactiveNeo4jClientTests.this.resultSummary));
			given(ReactiveNeo4jClientTests.this.record1.size()).willReturn(1);
			given(ReactiveNeo4jClientTests.this.record1.get(0)).willReturn(Values.value(23L));

			ReactiveNeo4jClient client = ReactiveNeo4jClient.create(ReactiveNeo4jClientTests.this.driver);

			String cypher = "MATCH (b:Bike) RETURN count(b)";
			Mono<Long> numberOfBikes = client.query(cypher).fetchAs(Long.class).one();

			StepVerifier.create(numberOfBikes).expectNext(23L).verifyComplete();

			verifyDatabaseSelection(null);

			verify(ReactiveNeo4jClientTests.this.result).consume();
			verify(ReactiveNeo4jClientTests.this.resultSummary).gqlStatusObjects();
			verify(ReactiveNeo4jClientTests.this.resultSummary).hasPlan();
			verify(ReactiveNeo4jClientTests.this.session).run(eq(cypher), anyMap());
			verify(ReactiveNeo4jClientTests.this.session).close();
		}

	}

}

/*
 * Copyright (c) 2019 "Neo4j,"
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
package org.neo4j.springframework.data.core;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Values;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.reactive.RxResult;
import org.neo4j.driver.reactive.RxSession;
import org.neo4j.driver.reactive.RxTransaction;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.types.TypeSystem;
import org.neo4j.springframework.data.core.Neo4jClientTest.Bike;
import org.neo4j.springframework.data.core.Neo4jClientTest.BikeOwner;
import org.neo4j.springframework.data.core.Neo4jClientTest.BikeOwnerBinder;
import org.neo4j.springframework.data.core.Neo4jClientTest.BikeOwnerReader;
import org.neo4j.springframework.data.core.Neo4jClientTest.MapAssertionMatcher;

/**
 * @author Michael J. Simons
 */
@ExtendWith(MockitoExtension.class)
class ReactiveNeo4jClientTest {

	@Mock
	private Driver driver;

	@Mock
	private TypeSystem typeSystem;

	private ArgumentCaptor<SessionConfig> configArgumentCaptor = ArgumentCaptor.forClass(SessionConfig.class);

	@Mock
	private RxSession session;

	@Mock
	private RxResult result;

	@Mock
	private RxTransaction transaction;

	@Mock
	private ResultSummary resultSummary;

	@Mock
	private Record record1;

	@Mock
	private Record record2;

	void prepareMocks() {

		when(driver.defaultTypeSystem()).thenReturn(typeSystem);

		when(driver.rxSession(any(SessionConfig.class))).thenReturn(session);
		when(session.beginTransaction()).thenReturn(Mono.just(transaction));

		when(session.close()).thenReturn(Mono.empty());
	}

	@AfterEach
	void verifyNoMoreInteractionsWithMocks() {
		verifyNoMoreInteractions(driver, session, transaction, result, resultSummary, record1, record2);
	}

	@Test
	@DisplayName("Creation of queries and binding parameters should feel natural")
	void queryCreationShouldFeelGood() {

		prepareMocks();

		when(transaction.run(anyString(), anyMap())).thenReturn(result);
		when(transaction.commit()).thenReturn(Mono.empty());
		when(result.records()).thenReturn(Flux.just(record1, record2));

		ReactiveNeo4jClient client = ReactiveNeo4jClient.create(driver);

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("bikeName", "M.*");
		parameters.put("location", "Sweden");

		String cypher = "MATCH (o:User {name: $name}) - [:OWNS] -> (b:Bike) - [:USED_ON] -> (t:Trip) " +
			"WHERE t.takenOn > $aDate " +
			"  AND b.name =~ $bikeName " +
			"  AND t.location = $location " +
			"RETURN b";

		Flux<Map<String, Object>> usedBikes = client
			.query(cypher)
			.bind("michael").to("name")
			.bindAll(parameters)
			.bind(LocalDate.of(2019, 1, 1)).to("aDate")
			.fetch()
			.all();

		StepVerifier.create(usedBikes)
			.expectNextCount(2L)
			.verifyComplete();

		verifyDatabaseSelection(null);

		Map<String, Object> expectedParameters = new HashMap<>();
		expectedParameters.putAll(parameters);
		expectedParameters.put("name", "michael");
		expectedParameters.put("aDate", LocalDate.of(2019, 1, 1));
		verify(transaction).run(eq(cypher), argThat(new MapAssertionMatcher(expectedParameters)));

		verify(result).records();
		verify(record1).asMap();
		verify(record2).asMap();
		verify(transaction).commit();
		verify(transaction).rollback();
		verify(session).close();
	}

	@Test
	void databaseSelectionShouldBePossibleOnlyOnce() {

		prepareMocks();

		when(transaction.run(anyString(), anyMap())).thenReturn(result);
		when(transaction.commit()).thenReturn(Mono.empty());
		when(result.records()).thenReturn(Flux.just(record1, record2));

		ReactiveNeo4jClient client = ReactiveNeo4jClient.create(driver);

		String cypher = "MATCH (u:User) WHERE u.name =~ $name";
		Mono<Map<String, Object>> firstMatchingUser = client
			.query(cypher)
			.in("bikingDatabase")
			.bind("Someone.*").to("name")
			.fetch()
			.first();

		StepVerifier.create(firstMatchingUser)
			.expectNextCount(1L)
			.verifyComplete();

		verifyDatabaseSelection("bikingDatabase");

		Map<String, Object> expectedParameters = new HashMap<>();
		expectedParameters.put("name", "Someone.*");

		verify(transaction).run(eq(cypher), argThat(new MapAssertionMatcher(expectedParameters)));
		verify(result).records();
		verify(record1).asMap();
		verify(transaction).commit();
		verify(transaction).rollback();
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

	@Nested
	@DisplayName("Callback handling should feel good")
	class CallbackHandlingShouldFeelGood {

		@Test
		void withDefaultDatabase() {

			prepareMocks();

			when(transaction.commit()).thenReturn(Mono.empty());

			ReactiveNeo4jClient client = ReactiveNeo4jClient.create(driver);
			Mono<Integer> singleResult = client
				.delegateTo(runner -> Mono.just(21))
				.run();

			StepVerifier.create(singleResult)
				.expectNext(21)
				.verifyComplete();

			verifyDatabaseSelection(null);

			verify(transaction).commit();
			verify(transaction).rollback();
			verify(session).close();
		}

		@Test
		void withDatabase() {

			prepareMocks();

			when(transaction.commit()).thenReturn(Mono.empty());

			ReactiveNeo4jClient client = ReactiveNeo4jClient.create(driver);
			Mono<Integer> singleResult = client
				.delegateTo(runner -> Mono.just(21))
				.in("aDatabase")
				.run();

			StepVerifier.create(singleResult)
				.expectNext(21)
				.verifyComplete();

			verifyDatabaseSelection("aDatabase");

			verify(transaction).commit();
			verify(transaction).rollback();
			verify(session).close();
		}
	}

	@Nested
	@DisplayName("Mapping should feel good")
	class MappingShouldFeelGood {

		@Test
		void reading() {

			prepareMocks();

			when(transaction.run(anyString(), anyMap())).thenReturn(result);
			when(transaction.commit()).thenReturn(Mono.empty());
			when(result.records()).thenReturn(Flux.just(record1));
			when(record1.get("name")).thenReturn(Values.value("michael"));

			ReactiveNeo4jClient client = ReactiveNeo4jClient.create(driver);

			String cypher = "MATCH (o:User {name: $name}) - [:OWNS] -> (b:Bike)" +
				"RETURN o, collect(b) as bikes";

			BikeOwnerReader mappingFunction = new BikeOwnerReader();
			Flux<BikeOwner> bikeOwners = client
				.query(cypher)
				.bind("michael").to("name")
				.fetchAs(BikeOwner.class).mappedBy(mappingFunction)
				.all();

			StepVerifier.create(bikeOwners)
				.expectNextMatches(o -> o.getName().equals("michael"))
				.verifyComplete();

			verifyDatabaseSelection(null);

			Map<String, Object> expectedParameters = new HashMap<>();
			expectedParameters.put("name", "michael");

			verify(transaction).run(eq(cypher), argThat(new MapAssertionMatcher(expectedParameters)));
			verify(result).records();
			verify(record1).get("name");
			verify(transaction).commit();
			verify(transaction).rollback();
			verify(session).close();
		}

		@Test
		void shouldApplyNullChecksDuringReading() {

			prepareMocks();

			when(transaction.run(anyString(), anyMap())).thenReturn(result);
			when(transaction.rollback()).thenReturn(Mono.empty());
			when(result.records()).thenReturn(Flux.just(record1, record2));
			when(record1.get("name")).thenReturn(Values.value("michael"));

			ReactiveNeo4jClient client = ReactiveNeo4jClient.create(driver);
			Flux<BikeOwner> bikeOwners = client
				.query("MATCH (n) RETURN n")
				.fetchAs(BikeOwner.class).mappedBy((t, r) -> {
					if (r == record1) {
						return new BikeOwner(r.get("name").asString(), Collections.emptyList());
					} else {
						return null;
					}
				})
				.all();

			StepVerifier.create(bikeOwners)
				.expectNextCount(1)
				.verifyError();

			verifyDatabaseSelection(null);

			verify(transaction).run(eq("MATCH (n) RETURN n"), argThat(new MapAssertionMatcher(Collections.emptyMap())));
			verify(result).records();
			verify(record1).get("name");
			verify(transaction).commit();
			verify(transaction).rollback();
			verify(session).close();
		}

		@Test
		void writing() {

			prepareMocks();

			when(transaction.run(anyString(), anyMap())).thenReturn(result);
			when(transaction.commit()).thenReturn(Mono.empty());
			when(result.consume()).thenReturn(Mono.just(resultSummary));

			ReactiveNeo4jClient client = ReactiveNeo4jClient.create(driver);

			BikeOwner michael = new BikeOwner("Michael", Arrays.asList(new Bike("Road"), new Bike("MTB")));
			String cypher = "MERGE (u:User {name: 'Michael'}) "
				+ "WITH u UNWIND $bikes as bike "
				+ "MERGE (b:Bike {name: bike}) "
				+ "MERGE (u) - [o:OWNS] -> (b) ";

			Mono<ResultSummary> summary = client
				.query(cypher)
				.bind(michael).with(new BikeOwnerBinder())
				.run();

			StepVerifier.create(summary)
				.expectNext(resultSummary)
				.verifyComplete();

			verifyDatabaseSelection(null);

			Map<String, Object> expectedParameters = new HashMap<>();
			expectedParameters.put("name", "Michael");

			verify(transaction).run(eq(cypher), argThat(new MapAssertionMatcher(expectedParameters)));
			verify(result).consume();
			verify(transaction).commit();
			verify(transaction).rollback();
			verify(session).close();
		}

		@Test
		@DisplayName("Some automatic conversion is ok")
		void automaticConversion() {

			prepareMocks();

			when(transaction.run(anyString(), anyMap())).thenReturn(result);
			when(transaction.commit()).thenReturn(Mono.empty());
			when(result.records()).thenReturn(Flux.just(record1));
			when(record1.size()).thenReturn(1);
			when(record1.get(0)).thenReturn(Values.value(23L));

			ReactiveNeo4jClient client = ReactiveNeo4jClient.create(driver);

			String cypher = "MATCH (b:Bike) RETURN count(b)";
			Mono<Long> numberOfBikes = client
				.query(cypher)
				.fetchAs(Long.class)
				.one();

			StepVerifier.create(numberOfBikes)
				.expectNext(23L)
				.verifyComplete();

			verifyDatabaseSelection(null);

			verify(transaction).run(eq(cypher), anyMap());
			verify(transaction).commit();
			verify(transaction).rollback();
			verify(session).close();
		}
	}

	@Test
	@DisplayName("Queries that return nothing should fit in")
	void queriesWithoutResultShouldFitInAsWell() {

		prepareMocks();

		when(transaction.run(anyString(), anyMap())).thenReturn(result);
		when(transaction.commit()).thenReturn(Mono.empty());
		when(result.consume()).thenReturn(Mono.just(resultSummary));

		ReactiveNeo4jClient client = ReactiveNeo4jClient.create(driver);

		String cypher = "DETACH DELETE (b) WHERE name = $name";

		Mono<ResultSummary> deletionResult = client
			.query(cypher)
			.bind("fixie").to("name")
			.run();

		StepVerifier.create(deletionResult)
			.expectNext(resultSummary)
			.verifyComplete();

		verifyDatabaseSelection(null);

		Map<String, Object> expectedParameters = new HashMap<>();
		expectedParameters.put("name", "fixie");

		verify(transaction).run(eq(cypher), argThat(new MapAssertionMatcher(expectedParameters)));
		verify(result).consume();
		verify(transaction).commit();
		verify(transaction).rollback();
		verify(session).close();
	}

	void verifyDatabaseSelection(String targetDatabase) {

		verify(driver).rxSession(configArgumentCaptor.capture());
		SessionConfig config = configArgumentCaptor.getValue();

		if (targetDatabase != null) {
			assertThat(config.database()).isPresent().contains(targetDatabase);
		} else {
			assertThat(config.database()).isEmpty();
		}
	}
}

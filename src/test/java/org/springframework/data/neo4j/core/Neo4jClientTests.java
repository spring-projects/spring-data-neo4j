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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import org.assertj.core.matcher.AssertionMatcher;
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
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Values;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.types.TypeSystem;

import org.springframework.data.neo4j.core.transaction.Neo4jTransactionUtils;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author Michael J. Simons
 */
@ExtendWith(MockitoExtension.class)
class Neo4jClientTests {

	@Mock
	private Driver driver;

	private ArgumentCaptor<SessionConfig> configArgumentCaptor = ArgumentCaptor.forClass(SessionConfig.class);

	@Mock
	private Session session;

	@Mock
	private TypeSystem typeSystem;

	@Mock
	private Result result;

	@Mock
	private ResultSummary resultSummary;

	@Mock
	private Record record1;

	@Mock
	private Record record2;

	void prepareMocks() {

		given(this.driver.session(any(SessionConfig.class))).willReturn(this.session);

		given(this.session.lastBookmarks()).willReturn(Set.of(Mockito.mock(Bookmark.class)));
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

		given(this.session.run(anyString(), anyMap())).willReturn(this.result);
		given(this.result.stream()).willReturn(Stream.of(this.record1, this.record2));
		given(this.result.consume()).willReturn(this.resultSummary);

		Neo4jClient client = Neo4jClient.create(this.driver);

		String cypher = "MATCH (u:User) WHERE u.name =~ $name";

		Optional<Map<String, Object>> firstMatchingUser = client.query(cypher)
			.in("bikingDatabase")
			.asUser("aUser")
			.bind("Someone.*")
			.to("name")
			.fetch()
			.first();

		assertThat(firstMatchingUser).isPresent();

		verifyDatabaseSelection("bikingDatabase");
		verifyUserSelection("aUser");

		Map<String, Object> expectedParameters = new HashMap<>();
		expectedParameters.put("name", "Someone.*");

		verify(this.session).run(eq(cypher), MockitoHamcrest.argThat(new MapAssertionMatcher(expectedParameters)));
		verify(this.result).stream();
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

		given(this.session.run(anyString(), anyMap())).willReturn(this.result);
		given(this.result.stream()).willReturn(Stream.of(this.record1, this.record2));
		given(this.result.consume()).willReturn(this.resultSummary);

		Neo4jClient client = Neo4jClient.create(this.driver);

		String cypher = "MATCH (u:User) WHERE u.name =~ $name";

		Optional<Map<String, Object>> firstMatchingUser = client.query(cypher)
			.asUser("aUser")
			.in("bikingDatabase")
			.bind("Someone.*")
			.to("name")
			.fetch()
			.first();

		assertThat(firstMatchingUser).isPresent();

		verifyDatabaseSelection("bikingDatabase");
		verifyUserSelection("aUser");

		Map<String, Object> expectedParameters = new HashMap<>();
		expectedParameters.put("name", "Someone.*");

		verify(this.session).run(eq(cypher), MockitoHamcrest.argThat(new MapAssertionMatcher(expectedParameters)));
		verify(this.result).stream();
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

		given(this.session.run(anyString(), anyMap())).willReturn(this.result);
		given(this.result.stream()).willReturn(Stream.of(this.record1, this.record2));
		given(this.result.consume()).willReturn(this.resultSummary);

		Neo4jClient client = Neo4jClient.create(this.driver);

		String cypher = "MATCH (u:User) WHERE u.name =~ $name";

		Optional<Map<String, Object>> firstMatchingUser = client.query(cypher)
			.asUser("aUser")
			.bind("Someone.*")
			.to("name")
			.fetch()
			.first();

		assertThat(firstMatchingUser).isPresent();

		verifyDatabaseSelection(null);
		verifyUserSelection("aUser");

		Map<String, Object> expectedParameters = new HashMap<>();
		expectedParameters.put("name", "Someone.*");

		verify(this.session).run(eq(cypher), MockitoHamcrest.argThat(new MapAssertionMatcher(expectedParameters)));
		verify(this.result).stream();
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

		given(this.session.run(anyString(), anyMap())).willReturn(this.result);
		given(this.result.stream()).willReturn(Stream.of(this.record1, this.record2));
		given(this.result.consume()).willReturn(this.resultSummary);

		Neo4jClient client = Neo4jClient.create(this.driver);

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("bikeName", "M.*");
		parameters.put("location", "Sweden");

		String cypher = """
				MATCH (o:User {name: $name}) - [:OWNS] -> (b:Bike) - [:USED_ON] -> (t:Trip)\s
				WHERE t.takenOn > $aDate   AND b.name =~ $bikeName   AND t.location = $location\s
				RETURN b
				""";

		Collection<Map<String, Object>> usedBikes = client.query(cypher)
			.bind("michael")
			.to("name")
			.bindAll(parameters)
			.bind(LocalDate.of(2019, 1, 1))
			.to("aDate")
			.fetch()
			.all();

		assertThat(usedBikes).hasSize(2);

		verifyDatabaseSelection(null);

		Map<String, Object> expectedParameters = new HashMap<>(parameters);
		expectedParameters.put("name", "michael");
		expectedParameters.put("aDate", LocalDate.of(2019, 1, 1));
		verify(this.session).run(eq(cypher), MockitoHamcrest.argThat(new MapAssertionMatcher(expectedParameters)));

		verify(this.result).stream();
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

		given(this.session.run(anyString(), anyMap())).willReturn(this.result);
		given(this.result.stream()).willReturn(Stream.of(this.record1, this.record2));
		given(this.result.consume()).willReturn(this.resultSummary);

		Neo4jClient client = Neo4jClient.create(this.driver);

		String cypher = "MATCH (u:User) WHERE u.name =~ $name";

		Optional<Map<String, Object>> firstMatchingUser = client.query(cypher)
			.in("bikingDatabase")
			.bind("Someone.*")
			.to("name")
			.fetch()
			.first();

		assertThat(firstMatchingUser).isPresent();

		verifyDatabaseSelection("bikingDatabase");

		Map<String, Object> expectedParameters = new HashMap<>();
		expectedParameters.put("name", "Someone.*");

		verify(this.session).run(eq(cypher), MockitoHamcrest.argThat(new MapAssertionMatcher(expectedParameters)));
		verify(this.result).stream();
		verify(this.result).consume();
		verify(this.resultSummary).gqlStatusObjects();
		verify(this.resultSummary).hasPlan();
		verify(this.record1).asMap();
		verify(this.session).close();
	}

	@Test
	void databaseSelectionShouldPreventIllegalValues() {

		Neo4jClient client = Neo4jClient.create(this.driver);

		assertThat(client.query("RETURN 1").in(null)).isNotNull();
		assertThat(client.query("RETURN 1").in("foobar")).isNotNull();

		String[] invalidDatabaseNames = { "", " ", "\t" };
		for (String invalidDatabaseName : invalidDatabaseNames) {
			assertThatIllegalArgumentException()
				.isThrownBy(() -> client.delegateTo(r -> Optional.empty()).in(invalidDatabaseName));
		}

		for (String invalidDatabaseName : invalidDatabaseNames) {
			assertThatIllegalArgumentException().isThrownBy(() -> client.query("RETURN 1").in(invalidDatabaseName));
		}
	}

	@Test // GH-2159
	void databaseSelectionBeanShouldGetRespectedIfExisting() {
		prepareMocks();
		given(this.session.run(anyString(), anyMap())).willReturn(this.result);
		given(this.result.stream()).willReturn(Stream.of(this.record1, this.record2));
		given(this.result.consume()).willReturn(this.resultSummary);

		String databaseName = "customDatabaseSelection";
		DatabaseSelectionProvider databaseSelection = DatabaseSelectionProvider
			.createStaticDatabaseSelectionProvider(databaseName);

		Neo4jClient client = Neo4jClient.create(this.driver, databaseSelection);

		String query = "RETURN 1";
		client.query(query).fetch().first();
		verifyDatabaseSelection(databaseName);

		verify(this.session).run(eq(query), anyMap());
		verify(this.result).stream();
		verify(this.result).consume();
		verify(this.resultSummary).gqlStatusObjects();
		verify(this.resultSummary).hasPlan();
		verify(this.record1).asMap();
		verify(this.session).close();

	}

	@Test
	@DisplayName("Queries that return nothing should fit in")
	void queriesWithoutResultShouldFitInAsWell() {

		prepareMocks();

		given(this.session.run(anyString(), anyMap())).willReturn(this.result);
		given(this.result.consume()).willReturn(this.resultSummary);

		Neo4jClient client = Neo4jClient.create(this.driver);

		String cypher = "DETACH DELETE (b) WHERE name = $name";

		client.query(cypher).bind("fixie").to("name").run();

		verifyDatabaseSelection(null);

		Map<String, Object> expectedParameters = new HashMap<>();
		expectedParameters.put("name", "fixie");

		verify(this.session).run(eq(cypher), MockitoHamcrest.argThat(new MapAssertionMatcher(expectedParameters)));
		verify(this.result).consume();
		verify(this.resultSummary).gqlStatusObjects();
		verify(this.resultSummary).hasPlan();
		verify(this.session).close();
	}

	void verifyDatabaseSelection(String targetDatabase) {

		verify(this.driver).session(this.configArgumentCaptor.capture());
		SessionConfig config = this.configArgumentCaptor.getValue();

		if (targetDatabase != null) {
			assertThat(config.database()).isPresent().contains(targetDatabase);
		}
		else {
			assertThat(config.database()).isEmpty();
		}
	}

	void verifyUserSelection(String aUser) {

		verify(this.driver).session(this.configArgumentCaptor.capture());
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

	static class BikeOwner {

		private final String name;

		private final List<Bike> bikes;

		BikeOwner(String name, List<Bike> bikes) {
			this.name = name;
			this.bikes = new ArrayList<>(bikes);
		}

		String getName() {
			return this.name;
		}

		List<Bike> getBikes() {
			return Collections.unmodifiableList(this.bikes);
		}

	}

	static class Bike {

		private final String name;

		Bike(String name) {
			this.name = name;
		}

		String getName() {
			return this.name;
		}

	}

	static class BikeOwnerReader implements BiFunction<TypeSystem, Record, BikeOwner> {

		@Override
		public BikeOwner apply(TypeSystem typeSystem, Record record) {
			return new BikeOwner(record.get("name").asString(), Collections.emptyList());
		}

	}

	static class BikeOwnerBinder implements Function<BikeOwner, Map<String, Object>> {

		@Override
		public Map<String, Object> apply(BikeOwner bikeOwner) {

			Map<String, Object> mappedValues = new HashMap<>();

			mappedValues.put("name", bikeOwner.getName());
			return mappedValues;
		}

	}

	static class MapAssertionMatcher extends AssertionMatcher<Map<String, Object>> {

		private final Map<String, Object> expectedParameters;

		MapAssertionMatcher(Map<String, Object> expectedParameters) {
			this.expectedParameters = expectedParameters;
		}

		@Override
		public void assertion(Map<String, Object> actual) {
			assertThat(actual).containsAllEntriesOf(this.expectedParameters);
		}

	}

	@Nested
	@DisplayName("Callback handling should feel good")
	class CallbackHandlingShouldFeelGood {

		@Test
		void withDefaultDatabase() {

			prepareMocks();

			Neo4jClient client = Neo4jClient.create(Neo4jClientTests.this.driver);
			Optional<Integer> singleResult = client.delegateTo(runner -> Optional.of(42)).run();

			assertThat(singleResult).isPresent().hasValue(42);

			verifyDatabaseSelection(null);

			verify(Neo4jClientTests.this.session).close();
		}

		@Test
		void withDatabase() {

			prepareMocks();

			Neo4jClient client = Neo4jClient.create(Neo4jClientTests.this.driver);
			Optional<Integer> singleResult = client.delegateTo(runner -> Optional.of(42)).in("aDatabase").run();

			assertThat(singleResult).isPresent().hasValue(42);

			verifyDatabaseSelection("aDatabase");

			verify(Neo4jClientTests.this.session).close();
		}

		@Test // GH-2369
		void databaseSelectionShouldBePropagatedToDelegate() {

			prepareMocks();

			String databaseName = "aDatabase";
			DatabaseSelectionProvider databaseSelection = DatabaseSelectionProvider
				.createStaticDatabaseSelectionProvider(databaseName);

			Neo4jClient client = Neo4jClient.create(Neo4jClientTests.this.driver, databaseSelection);
			Optional<Integer> singleResult = client.delegateTo(runner -> Optional.of(42)).run();

			assertThat(singleResult).isPresent().hasValue(42);

			verifyDatabaseSelection("aDatabase");

			verify(Neo4jClientTests.this.session).close();
		}

	}

	@Nested
	@DisplayName("Mapping should feel good")
	class MappingShouldFeelGood {

		@Test
		void reading() {

			prepareMocks();

			given(Neo4jClientTests.this.session.run(anyString(), anyMap())).willReturn(Neo4jClientTests.this.result);
			given(Neo4jClientTests.this.result.stream()).willReturn(Stream.of(Neo4jClientTests.this.record1));
			given(Neo4jClientTests.this.result.consume()).willReturn(Neo4jClientTests.this.resultSummary);
			given(Neo4jClientTests.this.record1.get("name")).willReturn(Values.value("michael"));

			Neo4jClient client = Neo4jClient.create(Neo4jClientTests.this.driver);

			String cypher = "MATCH (o:User {name: $name}) - [:OWNS] -> (b:Bike) RETURN o, collect(b) as bikes";

			BikeOwnerReader mappingFunction = new BikeOwnerReader();
			Collection<BikeOwner> bikeOwners = client.query(cypher)
				.bind("michael")
				.to("name")
				.fetchAs(BikeOwner.class)
				.mappedBy(mappingFunction)
				.all();

			assertThat(bikeOwners).hasSize(1).first().hasFieldOrPropertyWithValue("name", "michael");

			verifyDatabaseSelection(null);

			Map<String, Object> expectedParameters = new HashMap<>();
			expectedParameters.put("name", "michael");

			verify(Neo4jClientTests.this.session).run(eq(cypher),
					MockitoHamcrest.argThat(new MapAssertionMatcher(expectedParameters)));
			verify(Neo4jClientTests.this.result).stream();
			verify(Neo4jClientTests.this.result).consume();
			verify(Neo4jClientTests.this.resultSummary).gqlStatusObjects();
			verify(Neo4jClientTests.this.resultSummary).hasPlan();
			verify(Neo4jClientTests.this.record1).get("name");
			verify(Neo4jClientTests.this.session).close();
		}

		@Test
		void shouldApplyNullChecksDuringReading() {

			prepareMocks();

			given(Neo4jClientTests.this.session.run(anyString(), anyMap())).willReturn(Neo4jClientTests.this.result);
			given(Neo4jClientTests.this.result.stream())
				.willReturn(Stream.of(Neo4jClientTests.this.record1, Neo4jClientTests.this.record2));
			given(Neo4jClientTests.this.result.consume()).willReturn(Neo4jClientTests.this.resultSummary);
			given(Neo4jClientTests.this.record1.get("name")).willReturn(Values.value("michael"));

			Neo4jClient client = Neo4jClient.create(Neo4jClientTests.this.driver);

			Collection<BikeOwner> owners = client.query("MATCH (n) RETURN n")
				.fetchAs(BikeOwner.class)
				.mappedBy((t, r) -> {
					if (r == Neo4jClientTests.this.record1) {
						return new BikeOwner(r.get("name").asString(), Collections.emptyList());
					}
					else {
						return null;
					}
				})
				.all();
			assertThat(owners).hasSize(1);
			verifyDatabaseSelection(null);

			verify(Neo4jClientTests.this.session).run(eq("MATCH (n) RETURN n"),
					MockitoHamcrest.argThat(new MapAssertionMatcher(Collections.emptyMap())));
			verify(Neo4jClientTests.this.result).stream();
			verify(Neo4jClientTests.this.result).consume();
			verify(Neo4jClientTests.this.resultSummary).gqlStatusObjects();
			verify(Neo4jClientTests.this.resultSummary).hasPlan();
			verify(Neo4jClientTests.this.record1).get("name");
			verify(Neo4jClientTests.this.session).close();
		}

		@Test
		void writing() {

			prepareMocks();

			given(Neo4jClientTests.this.session.run(anyString(), anyMap())).willReturn(Neo4jClientTests.this.result);
			given(Neo4jClientTests.this.result.consume()).willReturn(Neo4jClientTests.this.resultSummary);

			Neo4jClient client = Neo4jClient.create(Neo4jClientTests.this.driver);

			BikeOwner michael = new BikeOwner("Michael", Arrays.asList(new Bike("Road"), new Bike("MTB")));
			String cypher = """
					MERGE (u:User {name: 'Michael'})
					WITH u UNWIND $bikes as bike
					MERGE (b:Bike {name: bike}) MERGE (u) - [o:OWNS] -> (b)
					""";
			ResultSummary summary = client.query(cypher).bind(michael).with(new BikeOwnerBinder()).run();

			verifyDatabaseSelection(null);

			Map<String, Object> expectedParameters = new HashMap<>();
			expectedParameters.put("name", "Michael");

			verify(Neo4jClientTests.this.session).run(eq(cypher),
					MockitoHamcrest.argThat(new MapAssertionMatcher(expectedParameters)));
			verify(Neo4jClientTests.this.result).consume();
			verify(Neo4jClientTests.this.resultSummary).gqlStatusObjects();
			verify(Neo4jClientTests.this.resultSummary).hasPlan();
			verify(Neo4jClientTests.this.session).close();
		}

		@Test
		@DisplayName("Some automatic conversion is ok")
		void automaticConversion() {

			prepareMocks();

			given(Neo4jClientTests.this.session.run(anyString(), anyMap())).willReturn(Neo4jClientTests.this.result);
			given(Neo4jClientTests.this.result.hasNext()).willReturn(true);
			given(Neo4jClientTests.this.result.single()).willReturn(Neo4jClientTests.this.record1);
			given(Neo4jClientTests.this.result.consume()).willReturn(Neo4jClientTests.this.resultSummary);
			given(Neo4jClientTests.this.record1.size()).willReturn(1);
			given(Neo4jClientTests.this.record1.get(0)).willReturn(Values.value(23L));

			Neo4jClient client = Neo4jClient.create(Neo4jClientTests.this.driver);

			String cypher = "MATCH (b:Bike) RETURN count(b)";
			Optional<Long> numberOfBikes = client.query(cypher).fetchAs(Long.class).one();

			assertThat(numberOfBikes).isPresent().hasValue(23L);

			verifyDatabaseSelection(null);

			verify(Neo4jClientTests.this.session).run(eq(cypher), anyMap());
			verify(Neo4jClientTests.this.result).hasNext();
			verify(Neo4jClientTests.this.result).single();
			verify(Neo4jClientTests.this.result).consume();
			verify(Neo4jClientTests.this.resultSummary).gqlStatusObjects();
			verify(Neo4jClientTests.this.resultSummary).hasPlan();
			verify(Neo4jClientTests.this.session).close();
		}

	}

}

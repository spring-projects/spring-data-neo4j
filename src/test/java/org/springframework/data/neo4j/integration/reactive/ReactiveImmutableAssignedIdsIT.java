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
package org.springframework.data.neo4j.integration.reactive;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractReactiveNeo4jConfig;
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.callback.ReactiveBeforeBindCallback;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;
import org.springframework.data.neo4j.integration.shared.common.ImmutablePersonWithAssignedId;
import org.springframework.data.neo4j.integration.shared.common.ImmutablePersonWithAssignedIdRelationshipProperties;
import org.springframework.data.neo4j.integration.shared.common.ImmutableSecondPersonWithAssignedId;
import org.springframework.data.neo4j.integration.shared.common.ImmutableSecondPersonWithAssignedIdRelationshipProperties;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.ReactiveTransactionManager;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gerrit Meier
 */
@Tag(Neo4jExtension.NEEDS_REACTIVE_SUPPORT)
@Neo4jIntegrationTest
public class ReactiveImmutableAssignedIdsIT {

	public static final String SOME_VALUE_VALUE = "testValue";
	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;
	private final Driver driver;

	public ReactiveImmutableAssignedIdsIT(@Autowired Driver driver) {
		this.driver = driver;
	}

	@BeforeEach
	void cleanUp(@Autowired BookmarkCapture bookmarkCapture) {
		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			session.run("MATCH (n) DETACH DELETE n").consume();
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	@Test // GH-2141
	void saveWithAssignedIdsReturnsObjectWithIdSet(
			@Autowired ReactiveImmutablePersonWithAssignedIdRepository repository) {

		ImmutablePersonWithAssignedId fallback1 = new ImmutablePersonWithAssignedId();
		ImmutablePersonWithAssignedId fallback2 = ImmutablePersonWithAssignedId.fallback(fallback1);
		ImmutablePersonWithAssignedId person = ImmutablePersonWithAssignedId.fallback(fallback2);

		StepVerifier.create(repository.save(person))
				.assertNext(savedPerson -> {
					assertThat(savedPerson.id).isNotNull();
					assertThat(savedPerson.fallback).isNotNull();
					assertThat(savedPerson.fallback.fallback).isNotNull();
					assertThat(savedPerson.someValue).isEqualTo(SOME_VALUE_VALUE);
					assertThat(savedPerson.fallback.someValue).isEqualTo(SOME_VALUE_VALUE);
					assertThat(savedPerson.fallback.fallback.someValue).isEqualTo(SOME_VALUE_VALUE);
				})
				.verifyComplete();
	}

	@Test // GH-2141
	void saveAllWithAssignedIdsReturnsObjectWithIdSet(
			@Autowired ReactiveImmutablePersonWithAssignedIdRepository repository) {

		ImmutablePersonWithAssignedId fallback1 = new ImmutablePersonWithAssignedId();
		ImmutablePersonWithAssignedId fallback2 = ImmutablePersonWithAssignedId.fallback(fallback1);
		ImmutablePersonWithAssignedId person = ImmutablePersonWithAssignedId.fallback(fallback2);

		StepVerifier.create(repository.saveAll(Collections.singleton(person)))
				.assertNext(savedPerson -> {
					assertThat(savedPerson.id).isNotNull();
					assertThat(savedPerson.fallback).isNotNull();
					assertThat(savedPerson.fallback.fallback).isNotNull();
					assertThat(savedPerson.someValue).isEqualTo(SOME_VALUE_VALUE);
					assertThat(savedPerson.fallback.someValue).isEqualTo(SOME_VALUE_VALUE);
					assertThat(savedPerson.fallback.fallback.someValue).isEqualTo(SOME_VALUE_VALUE);
				})
				.verifyComplete();
	}

	@Test // GH-2148
	void saveRelationshipWithAssignedIdsContainsObjectWithIdSetForList(
			@Autowired ReactiveImmutablePersonWithAssignedIdRepository repository) {

		ImmutablePersonWithAssignedId onboarder = new ImmutablePersonWithAssignedId();
		ImmutablePersonWithAssignedId person = ImmutablePersonWithAssignedId.wasOnboardedBy(Collections.singletonList(onboarder));

		StepVerifier.create(repository.saveAll(Collections.singleton(person)))
				.assertNext(savedPerson -> {
					assertThat(savedPerson.wasOnboardedBy.get(0).id).isNotNull();
					assertThat(savedPerson.wasOnboardedBy.get(0).someValue).isEqualTo(SOME_VALUE_VALUE);
				})
				.verifyComplete();
	}

	@Test // GH-2148
	void saveRelationshipWithAssignedIdsContainsObjectWithIdSetForSet(
			@Autowired ReactiveImmutablePersonWithAssignedIdRepository repository) {

		ImmutablePersonWithAssignedId knowingPerson = new ImmutablePersonWithAssignedId();
		ImmutablePersonWithAssignedId person = ImmutablePersonWithAssignedId.knownBy(Collections.singleton(knowingPerson));

		StepVerifier.create(repository.saveAll(Collections.singleton(person)))
				.assertNext(savedPerson -> {
					assertThat(savedPerson.knownBy.iterator().next().id).isNotNull();
					assertThat(savedPerson.knownBy.iterator().next().someValue).isEqualTo(SOME_VALUE_VALUE);
				})
				.verifyComplete();
	}

	@Test // GH-2148
	void saveRelationshipWithAssignedIdsContainsObjectWithIdSetForMap(
			@Autowired ReactiveImmutablePersonWithAssignedIdRepository repository) {

		ImmutablePersonWithAssignedId rater = new ImmutablePersonWithAssignedId();
		ImmutablePersonWithAssignedId person = ImmutablePersonWithAssignedId.ratedBy(Collections.singletonMap("Good", rater));

		StepVerifier.create(repository.saveAll(Collections.singleton(person)))
				.assertNext(savedPerson -> {
					assertThat(savedPerson.ratedBy.keySet().iterator().next()).isEqualTo("Good");
					assertThat(savedPerson.ratedBy.values().iterator().next().id).isNotNull();
					assertThat(savedPerson.ratedBy.values().iterator().next().someValue).isEqualTo(SOME_VALUE_VALUE);
				})
				.verifyComplete();
	}

	@Test // GH-2148
	void saveRelationshipWithAssignedIdsContainsObjectWithIdSetForMapWithMultipleKeys(
			@Autowired ReactiveImmutablePersonWithAssignedIdRepository repository) {

		ImmutablePersonWithAssignedId rater1 = new ImmutablePersonWithAssignedId();
		ImmutablePersonWithAssignedId rater2 = new ImmutablePersonWithAssignedId();
		Map<String, ImmutablePersonWithAssignedId> raterMap = new HashMap<>();
		raterMap.put("Good", rater1);
		raterMap.put("Bad", rater2);
		ImmutablePersonWithAssignedId person = ImmutablePersonWithAssignedId.ratedBy(raterMap);

		StepVerifier.create(repository.saveAll(Collections.singleton(person)))
				.assertNext(savedPerson -> {
					assertThat(savedPerson.ratedBy.keySet()).containsExactlyInAnyOrder("Good", "Bad");
					assertThat(savedPerson.ratedBy.get("Good").id).isNotNull();
					assertThat(savedPerson.ratedBy.get("Good").someValue).isEqualTo(SOME_VALUE_VALUE);
					assertThat(savedPerson.ratedBy.get("Bad").id).isNotNull();
					assertThat(savedPerson.ratedBy.get("Bad").someValue).isEqualTo(SOME_VALUE_VALUE);
				})
				.verifyComplete();
	}

	@Test // GH-2148
	void saveRelationshipWithAssignedIdsContainsObjectWithIdSetForMapCollection(
			@Autowired ReactiveImmutablePersonWithAssignedIdRepository repository) {

		ImmutableSecondPersonWithAssignedId rater = new ImmutableSecondPersonWithAssignedId();
		ImmutablePersonWithAssignedId person = ImmutablePersonWithAssignedId.ratedByCollection(Collections.singletonMap("Good", Collections.singletonList(rater)));

		StepVerifier.create(repository.saveAll(Collections.singleton(person)))
				.assertNext(savedPerson -> {
					assertThat(savedPerson.ratedByCollection.values().iterator().next().get(0).id).isNotNull();
				})
				.verifyComplete();
	}

	@Test // GH-2148
	void saveRelationshipWithAssignedIdsContainsObjectWithIdSetForRelationshipProperties(
			@Autowired ReactiveImmutablePersonWithAssignedIdRepository repository) {

		ImmutablePersonWithAssignedId somebody = new ImmutablePersonWithAssignedId();
		ImmutablePersonWithAssignedIdRelationshipProperties properties = new ImmutablePersonWithAssignedIdRelationshipProperties(null, "blubb", somebody);
		ImmutablePersonWithAssignedId person = ImmutablePersonWithAssignedId.relationshipProperties(properties);

		StepVerifier.create(repository.saveAll(Collections.singleton(person)))
				.assertNext(savedPerson -> {
					assertThat(savedPerson.relationshipProperties.name).isNotNull();
					assertThat(savedPerson.relationshipProperties.target.id).isNotNull();
					assertThat(savedPerson.relationshipProperties.target.someValue).isEqualTo(SOME_VALUE_VALUE);
				})
				.verifyComplete();
	}

	@Test // GH-2148
	void saveRelationshipWithAssignedIdsContainsObjectWithIdSetForRelationshipPropertiesCollection(
			@Autowired ReactiveImmutablePersonWithAssignedIdRepository repository) {

		ImmutablePersonWithAssignedId somebody = new ImmutablePersonWithAssignedId();
		ImmutablePersonWithAssignedIdRelationshipProperties properties = new ImmutablePersonWithAssignedIdRelationshipProperties(null, "blubb", somebody);
		ImmutablePersonWithAssignedId person = ImmutablePersonWithAssignedId.relationshipPropertiesCollection(Collections.singletonList(properties));

		StepVerifier.create(repository.saveAll(Collections.singleton(person)))
				.assertNext(savedPerson -> {
					assertThat(savedPerson.relationshipPropertiesCollection.get(0).name).isNotNull();
					assertThat(savedPerson.relationshipPropertiesCollection.get(0).target.id).isNotNull();
					assertThat(savedPerson.relationshipPropertiesCollection.get(0).target.someValue).isEqualTo(SOME_VALUE_VALUE);
				})
				.verifyComplete();
	}

	@Test // GH-2148
	void saveRelationshipWithAssignedIdsContainsObjectWithIdSetForRelationshipPropertiesDynamic(
			@Autowired ReactiveImmutablePersonWithAssignedIdRepository repository) {

		ImmutablePersonWithAssignedId somebody = new ImmutablePersonWithAssignedId();
		ImmutablePersonWithAssignedIdRelationshipProperties properties = new ImmutablePersonWithAssignedIdRelationshipProperties(null, "blubb", somebody);
		ImmutablePersonWithAssignedId person = ImmutablePersonWithAssignedId.relationshipPropertiesDynamic(Collections.singletonMap("Good", properties));

		StepVerifier.create(repository.saveAll(Collections.singleton(person)))
				.assertNext(savedPerson -> {
					assertThat(savedPerson.relationshipPropertiesDynamic.keySet().iterator().next()).isEqualTo("Good");
					assertThat(savedPerson.relationshipPropertiesDynamic.values().iterator().next().name).isNotNull();
					assertThat(savedPerson.relationshipPropertiesDynamic.values().iterator().next().target.id).isNotNull();
					assertThat(savedPerson.relationshipPropertiesDynamic.values().iterator().next().target.someValue).isEqualTo(SOME_VALUE_VALUE);
				})
				.verifyComplete();
	}


	@Test // GH-2148
	void saveRelationshipWithAssignedIdsContainsObjectWithIdSetForRelationshipPropertiesDynamicCollection(
			@Autowired ReactiveImmutablePersonWithAssignedIdRepository repository) {

		ImmutableSecondPersonWithAssignedId somebody = new ImmutableSecondPersonWithAssignedId();
		ImmutableSecondPersonWithAssignedIdRelationshipProperties properties = new ImmutableSecondPersonWithAssignedIdRelationshipProperties(null, "blubb", somebody);
		ImmutablePersonWithAssignedId person = ImmutablePersonWithAssignedId.relationshipPropertiesDynamicCollection(Collections.singletonMap("Good", Collections.singletonList(properties)));

		StepVerifier.create(repository.saveAll(Collections.singleton(person)))
				.assertNext(savedPerson -> {
					assertThat(savedPerson.relationshipPropertiesDynamicCollection.keySet().iterator().next()).isEqualTo("Good");
					assertThat(savedPerson.relationshipPropertiesDynamicCollection.values().iterator().next().get(0).name).isNotNull();
					assertThat(savedPerson.relationshipPropertiesDynamicCollection.values().iterator().next().get(0).target.id).isNotNull();
				})
				.verifyComplete();
	}

	@Test // GH-2148
	void saveRelationshipWithAssignedIdsContainsAllRelationshipTypes(
			@Autowired ReactiveImmutablePersonWithAssignedIdRepository repository) {

		ImmutablePersonWithAssignedId fallback =
				new ImmutablePersonWithAssignedId();

		List<ImmutablePersonWithAssignedId> wasOnboardedBy =
				Collections.singletonList(new ImmutablePersonWithAssignedId());

		Set<ImmutablePersonWithAssignedId> knownBy =
				Collections.singleton(new ImmutablePersonWithAssignedId());

		Map<String, ImmutablePersonWithAssignedId> ratedBy =
				Collections.singletonMap("Good", new ImmutablePersonWithAssignedId());

		Map<String, List<ImmutableSecondPersonWithAssignedId>> ratedByCollection =
				Collections.singletonMap("Na", Collections.singletonList(new ImmutableSecondPersonWithAssignedId()));

		ImmutablePersonWithAssignedIdRelationshipProperties relationshipProperties =
				new ImmutablePersonWithAssignedIdRelationshipProperties(null, "rel1", new ImmutablePersonWithAssignedId());

		List<ImmutablePersonWithAssignedIdRelationshipProperties> relationshipPropertiesCollection =
				Collections.singletonList(new ImmutablePersonWithAssignedIdRelationshipProperties(null, "rel2", new ImmutablePersonWithAssignedId()));

		Map<String, ImmutablePersonWithAssignedIdRelationshipProperties> relationshipPropertiesDynamic =
				Collections.singletonMap("Ok", new ImmutablePersonWithAssignedIdRelationshipProperties(null, "rel3", new ImmutablePersonWithAssignedId()));

		Map<String, List<ImmutableSecondPersonWithAssignedIdRelationshipProperties>> relationshipPropertiesDynamicCollection =
				Collections.singletonMap("Nope",
						Collections.singletonList(new ImmutableSecondPersonWithAssignedIdRelationshipProperties(
								null, "rel4", new ImmutableSecondPersonWithAssignedId()))
				);

		ImmutablePersonWithAssignedId person = new ImmutablePersonWithAssignedId(null,
				wasOnboardedBy,
				knownBy,
				ratedBy,
				ratedByCollection,
				fallback,
				relationshipProperties,
				relationshipPropertiesCollection,
				relationshipPropertiesDynamic,
				relationshipPropertiesDynamicCollection
		);

		StepVerifier.create(repository.saveAll(Collections.singleton(person)))
				.assertNext(savedPerson -> {

					assertThat(savedPerson.wasOnboardedBy.get(0).id).isNotNull();
					assertThat(savedPerson.knownBy.iterator().next().id).isNotNull();

					assertThat(savedPerson.ratedBy.keySet().iterator().next()).isEqualTo("Good");
					assertThat(savedPerson.ratedBy.values().iterator().next().id).isNotNull();

					assertThat(savedPerson.ratedByCollection.keySet().iterator().next()).isEqualTo("Na");
					assertThat(savedPerson.ratedByCollection.values().iterator().next().get(0).id).isNotNull();

					assertThat(savedPerson.fallback.id).isNotNull();

					assertThat(savedPerson.relationshipProperties.name).isEqualTo("rel1");
					assertThat(savedPerson.relationshipProperties.target.id).isNotNull();
					assertThat(savedPerson.relationshipProperties.target.someValue).isEqualTo(SOME_VALUE_VALUE);

					assertThat(savedPerson.relationshipPropertiesCollection.get(0).name).isEqualTo("rel2");
					assertThat(savedPerson.relationshipPropertiesCollection.get(0).target.id).isNotNull();
					assertThat(savedPerson.relationshipPropertiesCollection.get(0).target.someValue).isEqualTo(SOME_VALUE_VALUE);

					assertThat(savedPerson.relationshipPropertiesDynamic.keySet().iterator().next()).isEqualTo("Ok");
					assertThat(savedPerson.relationshipPropertiesDynamic.values().iterator().next().name).isEqualTo("rel3");
					assertThat(savedPerson.relationshipPropertiesDynamic.values().iterator().next().target.id).isNotNull();
					assertThat(savedPerson.relationshipPropertiesDynamic.values().iterator().next().target.someValue).isEqualTo(SOME_VALUE_VALUE);

					assertThat(savedPerson.relationshipPropertiesDynamicCollection.keySet().iterator().next()).isEqualTo("Nope");
					assertThat(savedPerson.relationshipPropertiesDynamicCollection.values().iterator().next().get(0).name).isEqualTo("rel4");
					assertThat(savedPerson.relationshipPropertiesDynamicCollection.values().iterator().next().get(0).target.id).isNotNull();
				})
				.verifyComplete();
	}


	@Test // GH-2235
	void saveWithGeneratedIdsWithMultipleRelationshipsToOneNode(@Autowired ReactiveImmutablePersonWithAssignedIdRepository repository,
																@Autowired BookmarkCapture bookmarkCapture) {
		ImmutablePersonWithAssignedId person1 = new ImmutablePersonWithAssignedId();
		ImmutablePersonWithAssignedId person2 = ImmutablePersonWithAssignedId.fallback(person1);
		List<ImmutablePersonWithAssignedId> onboardedBy = new ArrayList<>();
		onboardedBy.add(person1);
		onboardedBy.add(person2);
		ImmutablePersonWithAssignedId person3 = ImmutablePersonWithAssignedId.wasOnboardedBy(onboardedBy);

		StepVerifier.create(repository.save(person3))
				.assertNext(savedPerson -> {
					assertThat(savedPerson.id).isNotNull();
					assertThat(savedPerson.wasOnboardedBy).allMatch(ob -> ob.id != null);

					ImmutablePersonWithAssignedId savedPerson2 = savedPerson.wasOnboardedBy.stream().filter(p -> p.fallback != null)
							.findFirst().get();

					assertThat(savedPerson2.fallback.id).isNotNull();
				})
				.verifyComplete();

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			List<Record> result = session.run(
					"MATCH (person3:ImmutablePersonWithAssignedId) " +
							"-[:ONBOARDED_BY]->(person2:ImmutablePersonWithAssignedId) " +
							"-[:FALLBACK]->(person1:ImmutablePersonWithAssignedId), " +
							"(person3)-[:ONBOARDED_BY]->(person1) " +
							"return person3")
					.list();
			assertThat(result).hasSize(1);
		}
	}

	interface ReactiveImmutablePersonWithAssignedIdRepository extends ReactiveNeo4jRepository<ImmutablePersonWithAssignedId, Long> {
	}

	@Configuration
	@EnableReactiveNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends AbstractReactiveNeo4jConfig {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Override
		protected Collection<String> getMappingBasePackages() {
			return Arrays.asList(ImmutablePersonWithAssignedId.class.getPackage().getName());
		}

		@Bean
		ReactiveBeforeBindCallback<ImmutablePersonWithAssignedId> valueChange() {
			return entity -> {
				entity.someValue = SOME_VALUE_VALUE;
				return Mono.just(entity);
			};
		}

		@Bean
		public Neo4jMappingContext neo4jMappingContext(Neo4jConversions neo4JConversions) throws ClassNotFoundException {

			Neo4jMappingContext mappingContext = new Neo4jMappingContext(neo4JConversions);
			mappingContext.setInitialEntitySet(getInitialEntitySet());
			mappingContext.setStrict(true);

			return mappingContext;
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

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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.transaction.ReactiveTransactionManager;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractReactiveNeo4jConfig;
import org.springframework.data.neo4j.core.ReactiveNeo4jTemplate;
import org.springframework.data.neo4j.integration.shared.common.ImmutablePersonWithGeneratedId;
import org.springframework.data.neo4j.integration.shared.common.ImmutablePersonWithGeneratedIdRelationshipProperties;
import org.springframework.data.neo4j.integration.shared.common.ImmutableSecondPersonWithGeneratedId;
import org.springframework.data.neo4j.integration.shared.common.ImmutableSecondPersonWithGeneratedIdRelationshipProperties;
import org.springframework.data.neo4j.integration.shared.common.MutableChild;
import org.springframework.data.neo4j.integration.shared.common.MutableParent;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Gerrit Meier
 */
@Tag(Neo4jExtension.NEEDS_REACTIVE_SUPPORT)
@Neo4jIntegrationTest
public class ReactiveImmutableGeneratedIdsIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;
	private final Driver driver;

	public ReactiveImmutableGeneratedIdsIT(@Autowired Driver driver) {
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
	void saveWithGeneratedIdsReturnsObjectWithIdSet(
			@Autowired ReactiveImmutablePersonWithGeneratedIdRepository repository) {

		ImmutablePersonWithGeneratedId fallback1 = new ImmutablePersonWithGeneratedId();
		ImmutablePersonWithGeneratedId fallback2 = ImmutablePersonWithGeneratedId.fallback(fallback1);
		ImmutablePersonWithGeneratedId person = ImmutablePersonWithGeneratedId.fallback(fallback2);

		StepVerifier.create(repository.save(person))
				.assertNext(savedPerson -> {
					assertThat(savedPerson.id).isNotNull();
					assertThat(savedPerson.fallback).isNotNull();
					assertThat(savedPerson.fallback.fallback).isNotNull();
				})
				.verifyComplete();
	}

	@Test // GH-2148
	void saveRelationshipWithGeneratedIdsContainsObjectWithIdSetForList(
			@Autowired ReactiveImmutablePersonWithGeneratedIdRepository repository) {

		ImmutablePersonWithGeneratedId onboarder = new ImmutablePersonWithGeneratedId();
		ImmutablePersonWithGeneratedId person = ImmutablePersonWithGeneratedId.wasOnboardedBy(Collections.singletonList(onboarder));

		StepVerifier.create(repository.save(person))
				.assertNext(savedPerson -> {
					assertThat(person.id).isNull();
					assertThat(savedPerson.wasOnboardedBy.get(0).id).isNotNull();
				})
				.verifyComplete();
	}

	@Test // GH-2148
	void saveRelationshipWithGeneratedIdsContainsObjectWithIdSetForSet(
			@Autowired ReactiveImmutablePersonWithGeneratedIdRepository repository) {

		ImmutablePersonWithGeneratedId knowingPerson = new ImmutablePersonWithGeneratedId();
		ImmutablePersonWithGeneratedId person = ImmutablePersonWithGeneratedId.knownBy(Collections.singleton(knowingPerson));

		StepVerifier.create(repository.save(person))
				.assertNext(savedPerson -> {
					assertThat(person.id).isNull();
					assertThat(savedPerson.knownBy.iterator().next().id).isNotNull();
				})
				.verifyComplete();
	}

	@Test // GH-2148
	void saveRelationshipWithGeneratedIdsContainsObjectWithIdSetForMap(
			@Autowired ReactiveImmutablePersonWithGeneratedIdRepository repository) {

		ImmutablePersonWithGeneratedId rater = new ImmutablePersonWithGeneratedId();
		ImmutablePersonWithGeneratedId person = ImmutablePersonWithGeneratedId.ratedBy(Collections.singletonMap("Good", rater));

		StepVerifier.create(repository.save(person))
				.assertNext(savedPerson -> {
					assertThat(person.id).isNull();
					assertThat(savedPerson.ratedBy.keySet().iterator().next()).isEqualTo("Good");
					assertThat(savedPerson.ratedBy.values().iterator().next().id).isNotNull();
				})
				.verifyComplete();
	}

	@Test // GH-2148
	void saveRelationshipWithGeneratedIdsContainsObjectWithIdSetForMapCollection(
			@Autowired ReactiveImmutablePersonWithGeneratedIdRepository repository) {

		ImmutableSecondPersonWithGeneratedId rater = new ImmutableSecondPersonWithGeneratedId();
		ImmutablePersonWithGeneratedId person = ImmutablePersonWithGeneratedId.ratedByCollection(Collections.singletonMap("Good", Collections.singletonList(rater)));

		StepVerifier.create(repository.save(person))
				.assertNext(savedPerson -> {
					assertThat(person.id).isNull();
					assertThat(savedPerson.ratedByCollection.values().iterator().next().get(0).id).isNotNull();
				})
				.verifyComplete();
	}

	@Test // GH-2148
	void saveRelationshipWithGeneratedIdsContainsObjectWithIdSetForRelationshipProperties(
			@Autowired ReactiveImmutablePersonWithGeneratedIdRepository repository) {

		ImmutablePersonWithGeneratedId somebody = new ImmutablePersonWithGeneratedId();
		ImmutablePersonWithGeneratedIdRelationshipProperties properties = new ImmutablePersonWithGeneratedIdRelationshipProperties(null, "blubb", somebody);
		ImmutablePersonWithGeneratedId person = ImmutablePersonWithGeneratedId.relationshipProperties(properties);

		StepVerifier.create(repository.save(person))
				.assertNext(savedPerson -> {
					assertThat(person.id).isNull();
					assertThat(savedPerson.relationshipProperties.name).isNotNull();
					assertThat(savedPerson.relationshipProperties.target.id).isNotNull();
				})
				.verifyComplete();
	}

	@Test // GH-2148
	void saveRelationshipWithGeneratedIdsContainsObjectWithIdSetForRelationshipPropertiesCollection(
			@Autowired ReactiveImmutablePersonWithGeneratedIdRepository repository) {

		ImmutablePersonWithGeneratedId somebody = new ImmutablePersonWithGeneratedId();
		ImmutablePersonWithGeneratedIdRelationshipProperties properties = new ImmutablePersonWithGeneratedIdRelationshipProperties(null, "blubb", somebody);
		ImmutablePersonWithGeneratedId person = ImmutablePersonWithGeneratedId.relationshipPropertiesCollection(Collections.singletonList(properties));

		StepVerifier.create(repository.save(person))
				.assertNext(savedPerson -> {
					assertThat(person.id).isNull();
					assertThat(savedPerson.relationshipPropertiesCollection.get(0).name).isNotNull();
					assertThat(savedPerson.relationshipPropertiesCollection.get(0).target.id).isNotNull();
				})
				.verifyComplete();
	}

	@Test // GH-2148
	void saveRelationshipWithGeneratedIdsContainsObjectWithIdSetForRelationshipPropertiesDynamic(
			@Autowired ReactiveImmutablePersonWithGeneratedIdRepository repository) {

		ImmutablePersonWithGeneratedId somebody = new ImmutablePersonWithGeneratedId();
		ImmutablePersonWithGeneratedIdRelationshipProperties properties = new ImmutablePersonWithGeneratedIdRelationshipProperties(null, "blubb", somebody);
		ImmutablePersonWithGeneratedId person = ImmutablePersonWithGeneratedId.relationshipPropertiesDynamic(Collections.singletonMap("Good", properties));

		StepVerifier.create(repository.save(person))
				.assertNext(savedPerson -> {
					assertThat(person.id).isNull();
					assertThat(savedPerson.relationshipPropertiesDynamic.keySet().iterator().next()).isEqualTo("Good");
					assertThat(savedPerson.relationshipPropertiesDynamic.values().iterator().next().name).isNotNull();
					assertThat(savedPerson.relationshipPropertiesDynamic.values().iterator().next().target.id).isNotNull();
				})
				.verifyComplete();

	}

	@Test // GH-2148
	void saveRelationshipWithGeneratedIdsContainsObjectWithIdSetForRelationshipPropertiesDynamicCollection(
			@Autowired ReactiveImmutablePersonWithGeneratedIdRepository repository) {

		ImmutableSecondPersonWithGeneratedId somebody = new ImmutableSecondPersonWithGeneratedId();
		ImmutableSecondPersonWithGeneratedIdRelationshipProperties properties = new ImmutableSecondPersonWithGeneratedIdRelationshipProperties(null, "blubb", somebody);
		ImmutablePersonWithGeneratedId person = ImmutablePersonWithGeneratedId.relationshipPropertiesDynamicCollection(Collections.singletonMap("Good", Collections.singletonList(properties)));

		StepVerifier.create(repository.save(person))
				.assertNext(savedPerson -> {
					assertThat(person.id).isNull();
					assertThat(savedPerson.relationshipPropertiesDynamicCollection.keySet().iterator().next()).isEqualTo("Good");
					assertThat(savedPerson.relationshipPropertiesDynamicCollection.values().iterator().next().get(0).name).isNotNull();
					assertThat(savedPerson.relationshipPropertiesDynamicCollection.values().iterator().next().get(0).target.id).isNotNull();
				})
				.verifyComplete();
	}

	@Test // GH-2148
	void saveRelationshipWithGeneratedIdsContainsAllRelationshipTypes(
			@Autowired ReactiveImmutablePersonWithGeneratedIdRepository repository) {

		ImmutablePersonWithGeneratedId fallback =
				new ImmutablePersonWithGeneratedId();

		List<ImmutablePersonWithGeneratedId> wasOnboardedBy =
				Collections.singletonList(new ImmutablePersonWithGeneratedId());

		Set<ImmutablePersonWithGeneratedId> knownBy =
				Collections.singleton(new ImmutablePersonWithGeneratedId());

		Map<String, ImmutablePersonWithGeneratedId> ratedBy =
				Collections.singletonMap("Good", new ImmutablePersonWithGeneratedId());

		Map<String, List<ImmutableSecondPersonWithGeneratedId>> ratedByCollection =
				Collections.singletonMap("Na", Collections.singletonList(new ImmutableSecondPersonWithGeneratedId()));

		ImmutablePersonWithGeneratedIdRelationshipProperties relationshipProperties =
				new ImmutablePersonWithGeneratedIdRelationshipProperties(null, "rel1", new ImmutablePersonWithGeneratedId());

		List<ImmutablePersonWithGeneratedIdRelationshipProperties> relationshipPropertiesCollection =
				Collections.singletonList(new ImmutablePersonWithGeneratedIdRelationshipProperties(null, "rel2", new ImmutablePersonWithGeneratedId()));

		Map<String, ImmutablePersonWithGeneratedIdRelationshipProperties> relationshipPropertiesDynamic =
				Collections.singletonMap("Ok", new ImmutablePersonWithGeneratedIdRelationshipProperties(null, "rel3", new ImmutablePersonWithGeneratedId()));

		Map<String, List<ImmutableSecondPersonWithGeneratedIdRelationshipProperties>> relationshipPropertiesDynamicCollection =
				Collections.singletonMap("Nope",
						Collections.singletonList(new ImmutableSecondPersonWithGeneratedIdRelationshipProperties(
								null, "rel4", new ImmutableSecondPersonWithGeneratedId()))
				);

		ImmutablePersonWithGeneratedId person = new ImmutablePersonWithGeneratedId(null,
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

		StepVerifier.create(repository.save(person))
				.assertNext(savedPerson -> {

					assertThat(person.id).isNull();
					assertThat(savedPerson.wasOnboardedBy.get(0).id).isNotNull();
					assertThat(savedPerson.knownBy.iterator().next().id).isNotNull();

					assertThat(savedPerson.ratedBy.keySet().iterator().next()).isEqualTo("Good");
					assertThat(savedPerson.ratedBy.values().iterator().next().id).isNotNull();

					assertThat(savedPerson.ratedByCollection.keySet().iterator().next()).isEqualTo("Na");
					assertThat(savedPerson.ratedByCollection.values().iterator().next().get(0).id).isNotNull();

					assertThat(savedPerson.fallback.id).isNotNull();

					assertThat(savedPerson.relationshipProperties.name).isEqualTo("rel1");
					assertThat(savedPerson.relationshipProperties.target.id).isNotNull();

					assertThat(savedPerson.relationshipPropertiesCollection.get(0).name).isEqualTo("rel2");
					assertThat(savedPerson.relationshipPropertiesCollection.get(0).target.id).isNotNull();

					assertThat(savedPerson.relationshipPropertiesDynamic.keySet().iterator().next()).isEqualTo("Ok");
					assertThat(savedPerson.relationshipPropertiesDynamic.values().iterator().next().name).isEqualTo("rel3");
					assertThat(savedPerson.relationshipPropertiesDynamic.values().iterator().next().target.id).isNotNull();

					assertThat(savedPerson.relationshipPropertiesDynamicCollection.keySet().iterator().next()).isEqualTo("Nope");
					assertThat(savedPerson.relationshipPropertiesDynamicCollection.values().iterator().next().get(0).name).isEqualTo("rel4");
					assertThat(savedPerson.relationshipPropertiesDynamicCollection.values().iterator().next().get(0).target.id).isNotNull();
				})
				.verifyComplete();
	}

	interface ReactiveImmutablePersonWithGeneratedIdRepository extends ReactiveNeo4jRepository<ImmutablePersonWithGeneratedId, Long> {
	}

	@Test // GH-2148
	void childrenShouldNotBeRecreatedForNoReasons(@Autowired ReactiveNeo4jTemplate template) {

		MutableParent parent = new MutableParent();
		List<MutableChild> children = Arrays.asList(new MutableChild(), new MutableChild());
		parent.setChildren(children);

		template.save(parent).as(StepVerifier::create)
				.consumeNextWith(saved -> {
					assertThat(saved).isSameAs(parent);
					assertThat(saved.getId()).isNotNull();
					assertThat(saved.getChildren()).isSameAs(children);
					assertThat(saved.getChildren()).allMatch(c -> c.getId() != null && children.contains(c));

				})
				.verifyComplete();
	}

	@Test // GH-2223
	void saveWithGeneratedIdsWithMultipleRelationshipsToOneNode(
			@Autowired ReactiveImmutablePersonWithGeneratedIdRepository repository,
			@Autowired BookmarkCapture bookmarkCapture) {

		ImmutablePersonWithGeneratedId person1 = new ImmutablePersonWithGeneratedId();
		ImmutablePersonWithGeneratedId person2 = ImmutablePersonWithGeneratedId.fallback(person1);
		List<ImmutablePersonWithGeneratedId> onboardedBy = new ArrayList<>();
		onboardedBy.add(person1);
		onboardedBy.add(person2);
		ImmutablePersonWithGeneratedId person3 = ImmutablePersonWithGeneratedId.wasOnboardedBy(onboardedBy);

		StepVerifier.create(repository.save(person3))
				.assertNext(savedPerson -> {
					assertThat(savedPerson.id).isNotNull();
					assertThat(savedPerson.wasOnboardedBy).allMatch(ob -> ob.id != null);

					ImmutablePersonWithGeneratedId savedPerson2 = savedPerson.wasOnboardedBy.stream().filter(p -> p.fallback != null).findFirst().get();
					assertThat(savedPerson2.fallback.id).isNotNull();
				})
				.verifyComplete();

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			List<Record> result = session.run(
					"MATCH (person3:ImmutablePersonWithGeneratedId) " +
							"-[:ONBOARDED_BY]->(person2:ImmutablePersonWithGeneratedId) " +
							"-[:FALLBACK]->(person1:ImmutablePersonWithGeneratedId), " +
							"(person3)-[:ONBOARDED_BY]->(person1) " +
							"return person3")
					.list();
			assertThat(result).hasSize(1);
		}
	}

	@Configuration
	@EnableReactiveNeo4jRepositories(considerNestedRepositories = true)
	@EnableTransactionManagement
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

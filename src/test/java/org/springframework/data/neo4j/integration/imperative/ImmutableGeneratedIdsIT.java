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
package org.springframework.data.neo4j.integration.imperative;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.integration.shared.common.ImmutablePersonWithGeneratedId;
import org.springframework.data.neo4j.integration.shared.common.ImmutablePersonWithGeneratedIdRelationshipProperties;
import org.springframework.data.neo4j.integration.shared.common.ImmutableSecondPersonWithGeneratedId;
import org.springframework.data.neo4j.integration.shared.common.ImmutableSecondPersonWithGeneratedIdRelationshipProperties;
import org.springframework.data.neo4j.integration.shared.common.MutableChild;
import org.springframework.data.neo4j.integration.shared.common.MutableParent;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

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
@Neo4jIntegrationTest
public class ImmutableGeneratedIdsIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	private final Driver driver;

	public ImmutableGeneratedIdsIT(@Autowired Driver driver) {
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
			@Autowired ImmutablePersonWithGeneratedIdRepository repository) {

		ImmutablePersonWithGeneratedId fallback1 = new ImmutablePersonWithGeneratedId();
		ImmutablePersonWithGeneratedId fallback2 = ImmutablePersonWithGeneratedId.fallback(fallback1);
		ImmutablePersonWithGeneratedId person = ImmutablePersonWithGeneratedId.fallback(fallback2);

		ImmutablePersonWithGeneratedId savedPerson = repository.save(person);

		assertThat(savedPerson.id).isNotNull();
		assertThat(savedPerson.fallback).isNotNull();
		assertThat(savedPerson.fallback.fallback).isNotNull();
	}

	@Test // GH-2148
	void saveRelationshipWithGeneratedIdsContainsObjectWithIdSetForList(
			@Autowired ImmutablePersonWithGeneratedIdRepository repository) {

		ImmutablePersonWithGeneratedId onboarder = new ImmutablePersonWithGeneratedId();
		ImmutablePersonWithGeneratedId person = ImmutablePersonWithGeneratedId.wasOnboardedBy(Collections.singletonList(onboarder));

		ImmutablePersonWithGeneratedId savedPerson = repository.save(person);

		assertThat(person.id).isNull();
		assertThat(savedPerson.wasOnboardedBy.get(0).id).isNotNull();
	}

	@Test // GH-2148
	void saveRelationshipWithGeneratedIdsContainsObjectWithIdSetForSet(
			@Autowired ImmutablePersonWithGeneratedIdRepository repository) {

		ImmutablePersonWithGeneratedId knowingPerson = new ImmutablePersonWithGeneratedId();
		ImmutablePersonWithGeneratedId person = ImmutablePersonWithGeneratedId.knownBy(Collections.singleton(knowingPerson));

		ImmutablePersonWithGeneratedId savedPerson = repository.save(person);

		assertThat(person.id).isNull();
		assertThat(savedPerson.knownBy.iterator().next().id).isNotNull();
	}

	@Test // GH-2148
	void saveRelationshipWithGeneratedIdsContainsObjectWithIdSetForMap(
			@Autowired ImmutablePersonWithGeneratedIdRepository repository) {

		ImmutablePersonWithGeneratedId rater = new ImmutablePersonWithGeneratedId();
		ImmutablePersonWithGeneratedId person = ImmutablePersonWithGeneratedId.ratedBy(Collections.singletonMap("Good", rater));

		ImmutablePersonWithGeneratedId savedPerson = repository.save(person);

		assertThat(person.id).isNull();
		assertThat(savedPerson.ratedBy.keySet().iterator().next()).isEqualTo("Good");
		assertThat(savedPerson.ratedBy.values().iterator().next().id).isNotNull();
	}

	@Test // GH-2148
	void saveRelationshipWithGeneratedIdsContainsObjectWithIdSetForMapWithMultipleKeys(
			@Autowired ImmutablePersonWithGeneratedIdRepository repository) {

		ImmutablePersonWithGeneratedId rater1 = new ImmutablePersonWithGeneratedId();
		ImmutablePersonWithGeneratedId rater2 = new ImmutablePersonWithGeneratedId();
		Map<String, ImmutablePersonWithGeneratedId> raterMap = new HashMap<>();
		raterMap.put("Good", rater1);
		raterMap.put("Bad", rater2);
		ImmutablePersonWithGeneratedId person = ImmutablePersonWithGeneratedId.ratedBy(raterMap);

		ImmutablePersonWithGeneratedId savedPerson = repository.save(person);

		assertThat(person.id).isNull();
		assertThat(savedPerson.ratedBy.keySet()).containsExactlyInAnyOrder("Good", "Bad");
		assertThat(savedPerson.ratedBy.get("Good").id).isNotNull();
		assertThat(savedPerson.ratedBy.get("Bad").id).isNotNull();
	}

	@Test // GH-2148
	void saveRelationshipWithGeneratedIdsContainsObjectWithIdSetForMapCollection(
			@Autowired ImmutablePersonWithGeneratedIdRepository repository) {

		ImmutableSecondPersonWithGeneratedId rater = new ImmutableSecondPersonWithGeneratedId();
		ImmutablePersonWithGeneratedId person = ImmutablePersonWithGeneratedId.ratedByCollection(Collections.singletonMap("Good", Collections.singletonList(rater)));

		ImmutablePersonWithGeneratedId savedPerson = repository.save(person);

		assertThat(person.id).isNull();
		assertThat(savedPerson.ratedByCollection.values().iterator().next().get(0).id).isNotNull();
	}

	@Test // GH-2148
	void saveRelationshipWithGeneratedIdsContainsObjectWithIdSetForRelationshipProperties(
			@Autowired ImmutablePersonWithGeneratedIdRepository repository) {

		ImmutablePersonWithGeneratedId somebody = new ImmutablePersonWithGeneratedId();
		ImmutablePersonWithGeneratedIdRelationshipProperties properties = new ImmutablePersonWithGeneratedIdRelationshipProperties(null, "blubb", somebody);
		ImmutablePersonWithGeneratedId person = ImmutablePersonWithGeneratedId.relationshipProperties(properties);

		ImmutablePersonWithGeneratedId savedPerson = repository.save(person);

		assertThat(person.id).isNull();
		assertThat(savedPerson.relationshipProperties.name).isNotNull();
		assertThat(savedPerson.relationshipProperties.target.id).isNotNull();
	}

	@Test // GH-2148
	void saveRelationshipWithGeneratedIdsContainsObjectWithIdSetForRelationshipPropertiesCollection(
			@Autowired ImmutablePersonWithGeneratedIdRepository repository) {

		ImmutablePersonWithGeneratedId somebody = new ImmutablePersonWithGeneratedId();
		ImmutablePersonWithGeneratedIdRelationshipProperties properties = new ImmutablePersonWithGeneratedIdRelationshipProperties(null, "blubb", somebody);
		ImmutablePersonWithGeneratedId person = ImmutablePersonWithGeneratedId.relationshipPropertiesCollection(Collections.singletonList(properties));

		ImmutablePersonWithGeneratedId savedPerson = repository.save(person);

		assertThat(person.id).isNull();
		assertThat(savedPerson.relationshipPropertiesCollection.get(0).name).isNotNull();
		assertThat(savedPerson.relationshipPropertiesCollection.get(0).target.id).isNotNull();
	}

	@Test // GH-2148
	void saveRelationshipWithGeneratedIdsContainsObjectWithIdSetForRelationshipPropertiesDynamic(
			@Autowired ImmutablePersonWithGeneratedIdRepository repository) {

		ImmutablePersonWithGeneratedId somebody = new ImmutablePersonWithGeneratedId();
		ImmutablePersonWithGeneratedIdRelationshipProperties properties = new ImmutablePersonWithGeneratedIdRelationshipProperties(null, "blubb", somebody);
		ImmutablePersonWithGeneratedId person = ImmutablePersonWithGeneratedId.relationshipPropertiesDynamic(Collections.singletonMap("Good", properties));

		ImmutablePersonWithGeneratedId savedPerson = repository.save(person);

		assertThat(person.id).isNull();
		assertThat(savedPerson.relationshipPropertiesDynamic.keySet().iterator().next()).isEqualTo("Good");
		assertThat(savedPerson.relationshipPropertiesDynamic.values().iterator().next().name).isNotNull();
		assertThat(savedPerson.relationshipPropertiesDynamic.values().iterator().next().target.id).isNotNull();
	}


	@Test // GH-2148
	void saveRelationshipWithGeneratedIdsContainsObjectWithIdSetForRelationshipPropertiesDynamicCollection(
			@Autowired ImmutablePersonWithGeneratedIdRepository repository) {

		ImmutableSecondPersonWithGeneratedId somebody = new ImmutableSecondPersonWithGeneratedId();
		ImmutableSecondPersonWithGeneratedIdRelationshipProperties properties = new ImmutableSecondPersonWithGeneratedIdRelationshipProperties(null, "blubb", somebody);
		ImmutablePersonWithGeneratedId person = ImmutablePersonWithGeneratedId.relationshipPropertiesDynamicCollection(Collections.singletonMap("Good", Collections.singletonList(properties)));

		ImmutablePersonWithGeneratedId savedPerson = repository.save(person);

		assertThat(person.id).isNull();
		assertThat(savedPerson.relationshipPropertiesDynamicCollection.keySet().iterator().next()).isEqualTo("Good");
		assertThat(savedPerson.relationshipPropertiesDynamicCollection.values().iterator().next().get(0).name).isNotNull();
		assertThat(savedPerson.relationshipPropertiesDynamicCollection.values().iterator().next().get(0).target.id).isNotNull();
	}

	@Test // GH-2148
	void saveRelationshipWithGeneratedIdsContainsAllRelationshipTypes(
			@Autowired ImmutablePersonWithGeneratedIdRepository repository) {

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

		ImmutablePersonWithGeneratedId savedPerson = repository.save(person);

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
	}

	interface ImmutablePersonWithGeneratedIdRepository extends Neo4jRepository<ImmutablePersonWithGeneratedId, Long> {}

	@Test // GH-2148
	void childrenShouldNotBeRecreatedForNoReasons(@Autowired Neo4jTemplate template) {

		MutableParent parent = new MutableParent();
		List<MutableChild> children = Arrays.asList(new MutableChild(), new MutableChild());
		parent.setChildren(children);

		MutableParent saved = template.save(parent);
		assertThat(saved).isSameAs(parent);
		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getChildren()).isSameAs(children);
		assertThat(saved.getChildren()).allMatch(c -> c.getId() != null && children.contains(c));
	}

	@Test // GH-2223
	void saveWithGeneratedIdsWithMultipleRelationshipsToOneNode(
			@Autowired ImmutablePersonWithGeneratedIdRepository repository,
			@Autowired BookmarkCapture bookmarkCapture) {

		ImmutablePersonWithGeneratedId person1 = new ImmutablePersonWithGeneratedId();
		ImmutablePersonWithGeneratedId person2 = ImmutablePersonWithGeneratedId.fallback(person1);
		List<ImmutablePersonWithGeneratedId> onboardedBy = new ArrayList<>();
		onboardedBy.add(person1);
		onboardedBy.add(person2);
		ImmutablePersonWithGeneratedId person3 = ImmutablePersonWithGeneratedId.wasOnboardedBy(onboardedBy);

		ImmutablePersonWithGeneratedId savedPerson = repository.save(person3);
		assertThat(savedPerson.id).isNotNull();
		assertThat(savedPerson.wasOnboardedBy).allMatch(ob -> ob.id != null);

		ImmutablePersonWithGeneratedId savedPerson2 = savedPerson.wasOnboardedBy.stream().filter(p -> p.fallback != null).findFirst().get();
		assertThat(savedPerson2.fallback.id).isNotNull();

		try (Session session = driver.session()) {
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
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	@EnableTransactionManagement
	static class Config extends AbstractNeo4jConfig {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Override
		protected Collection<String> getMappingBasePackages() {
			return Arrays.asList(ImmutablePersonWithGeneratedId.class.getPackage().getName());
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
		public PlatformTransactionManager transactionManager(Driver driver, DatabaseSelectionProvider databaseNameProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new Neo4jTransactionManager(driver, databaseNameProvider, Neo4jBookmarkManager.create(bookmarkCapture));
		}
	}
}

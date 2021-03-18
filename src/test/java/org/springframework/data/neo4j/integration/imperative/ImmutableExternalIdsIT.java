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

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.callback.BeforeBindCallback;
import org.springframework.data.neo4j.integration.shared.common.ImmutablePersonWithExternalId;
import org.springframework.data.neo4j.integration.shared.common.ImmutablePersonWithExternalId;
import org.springframework.data.neo4j.integration.shared.common.ImmutablePersonWithExternalIdRelationshipProperties;
import org.springframework.data.neo4j.integration.shared.common.ImmutableSecondPersonWithExternalId;
import org.springframework.data.neo4j.integration.shared.common.ImmutableSecondPersonWithExternalIdRelationshipProperties;
import org.springframework.data.neo4j.integration.shared.common.MutableChild;
import org.springframework.data.neo4j.integration.shared.common.MutableParent;
import org.springframework.data.neo4j.integration.shared.common.ThingWithAssignedId;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.annotation.EnableTransactionManagement;

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
public class ImmutableExternalIdsIT {

	public static final String SOME_VALUE_VALUE = "testValue";
	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@Test // GH-2141
	void saveWithGeneratedIdsReturnsObjectWithIdSet(
			@Autowired ImmutablePersonWithExternalIdRepository repository) {

		ImmutablePersonWithExternalId fallback1 = new ImmutablePersonWithExternalId();
		ImmutablePersonWithExternalId fallback2 = ImmutablePersonWithExternalId.fallback(fallback1);
		ImmutablePersonWithExternalId person = ImmutablePersonWithExternalId.fallback(fallback2);

		ImmutablePersonWithExternalId savedPerson = repository.save(person);

		assertThat(savedPerson.id).isNotNull();
		assertThat(savedPerson.fallback).isNotNull();
		assertThat(savedPerson.fallback.fallback).isNotNull();
		assertThat(savedPerson.someValue).isEqualTo(SOME_VALUE_VALUE);
		assertThat(savedPerson.fallback.someValue).isEqualTo(SOME_VALUE_VALUE);
		assertThat(savedPerson.fallback.fallback.someValue).isEqualTo(SOME_VALUE_VALUE);
	}

	@Test // GH-2141
	void saveAllWithGeneratedIdsReturnsObjectWithIdSet(
			@Autowired ImmutablePersonWithExternalIdRepository repository) {

		ImmutablePersonWithExternalId fallback1 = new ImmutablePersonWithExternalId();
		ImmutablePersonWithExternalId fallback2 = ImmutablePersonWithExternalId.fallback(fallback1);
		ImmutablePersonWithExternalId person = ImmutablePersonWithExternalId.fallback(fallback2);

		ImmutablePersonWithExternalId savedPerson = repository.saveAll(Collections.singleton(person)).get(0);

		assertThat(savedPerson.id).isNotNull();
		assertThat(savedPerson.fallback).isNotNull();
		assertThat(savedPerson.fallback.fallback).isNotNull();
		assertThat(savedPerson.someValue).isEqualTo(SOME_VALUE_VALUE);
		assertThat(savedPerson.fallback.someValue).isEqualTo(SOME_VALUE_VALUE);
		assertThat(savedPerson.fallback.fallback.someValue).isEqualTo(SOME_VALUE_VALUE);
	}

	@Test // GH-2148
	void saveRelationshipWithGeneratedIdsContainsObjectWithIdSetForList(
			@Autowired ImmutablePersonWithExternalIdRepository repository) {

		ImmutablePersonWithExternalId onboarder = new ImmutablePersonWithExternalId();
		ImmutablePersonWithExternalId person = ImmutablePersonWithExternalId.wasOnboardedBy(Collections.singletonList(onboarder));

		ImmutablePersonWithExternalId savedPerson = repository.saveAll(Collections.singleton(person)).get(0);

		assertThat(savedPerson.wasOnboardedBy.get(0).id).isNotNull();
		assertThat(savedPerson.wasOnboardedBy.get(0).someValue).isEqualTo(SOME_VALUE_VALUE);
	}

	@Test // GH-2148
	void saveRelationshipWithGeneratedIdsContainsObjectWithIdSetForSet(
			@Autowired ImmutablePersonWithExternalIdRepository repository) {

		ImmutablePersonWithExternalId knowingPerson = new ImmutablePersonWithExternalId();
		ImmutablePersonWithExternalId person = ImmutablePersonWithExternalId.knownBy(Collections.singleton(knowingPerson));

		ImmutablePersonWithExternalId savedPerson = repository.saveAll(Collections.singleton(person)).get(0);

		assertThat(savedPerson.knownBy.iterator().next().id).isNotNull();
		assertThat(savedPerson.knownBy.iterator().next().someValue).isEqualTo(SOME_VALUE_VALUE);
	}

	@Test // GH-2148
	void saveRelationshipWithGeneratedIdsContainsObjectWithIdSetForMap(
			@Autowired ImmutablePersonWithExternalIdRepository repository) {

		ImmutablePersonWithExternalId rater = new ImmutablePersonWithExternalId();
		ImmutablePersonWithExternalId person = ImmutablePersonWithExternalId.ratedBy(Collections.singletonMap("Good", rater));

		ImmutablePersonWithExternalId savedPerson = repository.saveAll(Collections.singleton(person)).get(0);

		assertThat(savedPerson.ratedBy.keySet().iterator().next()).isEqualTo("Good");
		assertThat(savedPerson.ratedBy.values().iterator().next().id).isNotNull();
		assertThat(savedPerson.ratedBy.values().iterator().next().someValue).isEqualTo(SOME_VALUE_VALUE);
	}

	@Test // GH-2148
	void saveRelationshipWithGeneratedIdsContainsObjectWithIdSetForMapWithMultipleKeys(
			@Autowired ImmutablePersonWithExternalIdRepository repository) {

		ImmutablePersonWithExternalId rater1 = new ImmutablePersonWithExternalId();
		ImmutablePersonWithExternalId rater2 = new ImmutablePersonWithExternalId();
		Map<String, ImmutablePersonWithExternalId> raterMap = new HashMap<>();
		raterMap.put("Good", rater1);
		raterMap.put("Bad", rater2);
		ImmutablePersonWithExternalId person = ImmutablePersonWithExternalId.ratedBy(raterMap);

		ImmutablePersonWithExternalId savedPerson = repository.saveAll(Collections.singleton(person)).get(0);

		assertThat(savedPerson.ratedBy.keySet()).containsExactlyInAnyOrder("Good", "Bad");
		assertThat(savedPerson.ratedBy.get("Good").id).isNotNull();
		assertThat(savedPerson.ratedBy.get("Good").someValue).isEqualTo(SOME_VALUE_VALUE);
		assertThat(savedPerson.ratedBy.get("Bad").id).isNotNull();
		assertThat(savedPerson.ratedBy.get("Bad").someValue).isEqualTo(SOME_VALUE_VALUE);
	}

	@Test // GH-2148
	void saveRelationshipWithGeneratedIdsContainsObjectWithIdSetForMapCollection(
			@Autowired ImmutablePersonWithExternalIdRepository repository) {

		ImmutableSecondPersonWithExternalId rater = new ImmutableSecondPersonWithExternalId();
		ImmutablePersonWithExternalId person = ImmutablePersonWithExternalId.ratedByCollection(Collections.singletonMap("Good", Collections.singletonList(rater)));

		ImmutablePersonWithExternalId savedPerson = repository.saveAll(Collections.singleton(person)).get(0);

		assertThat(savedPerson.ratedByCollection.values().iterator().next().get(0).id).isNotNull();
	}

	@Test // GH-2148
	void saveRelationshipWithGeneratedIdsContainsObjectWithIdSetForRelationshipProperties(
			@Autowired ImmutablePersonWithExternalIdRepository repository) {

		ImmutablePersonWithExternalId somebody = new ImmutablePersonWithExternalId();
		ImmutablePersonWithExternalIdRelationshipProperties properties = new ImmutablePersonWithExternalIdRelationshipProperties(null, "blubb", somebody);
		ImmutablePersonWithExternalId person = ImmutablePersonWithExternalId.relationshipProperties(properties);

		ImmutablePersonWithExternalId savedPerson = repository.saveAll(Collections.singleton(person)).get(0);

		assertThat(savedPerson.relationshipProperties.name).isNotNull();
		assertThat(savedPerson.relationshipProperties.target.id).isNotNull();
		assertThat(savedPerson.relationshipProperties.target.someValue).isEqualTo(SOME_VALUE_VALUE);
	}

	@Test // GH-2148
	void saveRelationshipWithGeneratedIdsContainsObjectWithIdSetForRelationshipPropertiesCollection(
			@Autowired ImmutablePersonWithExternalIdRepository repository) {

		ImmutablePersonWithExternalId somebody = new ImmutablePersonWithExternalId();
		ImmutablePersonWithExternalIdRelationshipProperties properties = new ImmutablePersonWithExternalIdRelationshipProperties(null, "blubb", somebody);
		ImmutablePersonWithExternalId person = ImmutablePersonWithExternalId.relationshipPropertiesCollection(Collections.singletonList(properties));

		ImmutablePersonWithExternalId savedPerson = repository.saveAll(Collections.singleton(person)).get(0);

		assertThat(savedPerson.relationshipPropertiesCollection.get(0).name).isNotNull();
		assertThat(savedPerson.relationshipPropertiesCollection.get(0).target.id).isNotNull();
		assertThat(savedPerson.relationshipPropertiesCollection.get(0).target.someValue).isEqualTo(SOME_VALUE_VALUE);
	}

	@Test // GH-2148
	void saveRelationshipWithGeneratedIdsContainsObjectWithIdSetForRelationshipPropertiesDynamic(
			@Autowired ImmutablePersonWithExternalIdRepository repository) {

		ImmutablePersonWithExternalId somebody = new ImmutablePersonWithExternalId();
		ImmutablePersonWithExternalIdRelationshipProperties properties = new ImmutablePersonWithExternalIdRelationshipProperties(null, "blubb", somebody);
		ImmutablePersonWithExternalId person = ImmutablePersonWithExternalId.relationshipPropertiesDynamic(Collections.singletonMap("Good", properties));

		ImmutablePersonWithExternalId savedPerson = repository.saveAll(Collections.singleton(person)).get(0);

		assertThat(savedPerson.relationshipPropertiesDynamic.keySet().iterator().next()).isEqualTo("Good");
		assertThat(savedPerson.relationshipPropertiesDynamic.values().iterator().next().name).isNotNull();
		assertThat(savedPerson.relationshipPropertiesDynamic.values().iterator().next().target.id).isNotNull();
		assertThat(savedPerson.relationshipPropertiesDynamic.values().iterator().next().target.someValue).isEqualTo(SOME_VALUE_VALUE);
	}


	@Test // GH-2148
	void saveRelationshipWithGeneratedIdsContainsObjectWithIdSetForRelationshipPropertiesDynamicCollection(
			@Autowired ImmutablePersonWithExternalIdRepository repository) {

		ImmutableSecondPersonWithExternalId somebody = new ImmutableSecondPersonWithExternalId();
		ImmutableSecondPersonWithExternalIdRelationshipProperties properties = new ImmutableSecondPersonWithExternalIdRelationshipProperties(null, "blubb", somebody);
		ImmutablePersonWithExternalId person = ImmutablePersonWithExternalId.relationshipPropertiesDynamicCollection(Collections.singletonMap("Good", Collections.singletonList(properties)));

		ImmutablePersonWithExternalId savedPerson = repository.saveAll(Collections.singleton(person)).get(0);

		assertThat(savedPerson.relationshipPropertiesDynamicCollection.keySet().iterator().next()).isEqualTo("Good");
		assertThat(savedPerson.relationshipPropertiesDynamicCollection.values().iterator().next().get(0).name).isNotNull();
		assertThat(savedPerson.relationshipPropertiesDynamicCollection.values().iterator().next().get(0).target.id).isNotNull();
	}

	@Test // GH-2148
	void saveRelationshipWithGeneratedIdsContainsAllRelationshipTypes(
			@Autowired ImmutablePersonWithExternalIdRepository repository) {

		ImmutablePersonWithExternalId fallback =
				new ImmutablePersonWithExternalId();

		List<ImmutablePersonWithExternalId> wasOnboardedBy =
				Collections.singletonList(new ImmutablePersonWithExternalId());

		Set<ImmutablePersonWithExternalId> knownBy =
				Collections.singleton(new ImmutablePersonWithExternalId());

		Map<String, ImmutablePersonWithExternalId> ratedBy =
				Collections.singletonMap("Good", new ImmutablePersonWithExternalId());

		Map<String, List<ImmutableSecondPersonWithExternalId>> ratedByCollection =
				Collections.singletonMap("Na", Collections.singletonList(new ImmutableSecondPersonWithExternalId()));

		ImmutablePersonWithExternalIdRelationshipProperties relationshipProperties =
				new ImmutablePersonWithExternalIdRelationshipProperties(null, "rel1", new ImmutablePersonWithExternalId());

		List<ImmutablePersonWithExternalIdRelationshipProperties> relationshipPropertiesCollection =
				Collections.singletonList(new ImmutablePersonWithExternalIdRelationshipProperties(null, "rel2", new ImmutablePersonWithExternalId()));

		Map<String, ImmutablePersonWithExternalIdRelationshipProperties> relationshipPropertiesDynamic =
				Collections.singletonMap("Ok", new ImmutablePersonWithExternalIdRelationshipProperties(null, "rel3", new ImmutablePersonWithExternalId()));

		Map<String, List<ImmutableSecondPersonWithExternalIdRelationshipProperties>> relationshipPropertiesDynamicCollection =
				Collections.singletonMap("Nope",
						Collections.singletonList(new ImmutableSecondPersonWithExternalIdRelationshipProperties(
								null, "rel4", new ImmutableSecondPersonWithExternalId()))
				);

		ImmutablePersonWithExternalId person = new ImmutablePersonWithExternalId(null,
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

		ImmutablePersonWithExternalId savedPerson = repository.saveAll(Collections.singleton(person)).get(0);

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
	}

	interface ImmutablePersonWithExternalIdRepository extends Neo4jRepository<ImmutablePersonWithExternalId, Long> {}

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
			return Arrays.asList(ImmutablePersonWithExternalId.class.getPackage().getName());
		}

		@Bean
		BeforeBindCallback<ImmutablePersonWithExternalId> valueChange() {
			return entity -> {
				entity.someValue = SOME_VALUE_VALUE;
				return entity;
			};
		}

		@Bean
		public Neo4jMappingContext neo4jMappingContext(Neo4jConversions neo4JConversions) throws ClassNotFoundException {

			Neo4jMappingContext mappingContext = new Neo4jMappingContext(neo4JConversions);
			mappingContext.setInitialEntitySet(getInitialEntitySet());
			mappingContext.setStrict(true);

			return mappingContext;
		}
	}
}

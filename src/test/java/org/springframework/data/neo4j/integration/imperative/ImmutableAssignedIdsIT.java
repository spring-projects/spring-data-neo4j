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
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.callback.BeforeBindCallback;
import org.springframework.data.neo4j.integration.shared.common.ImmutablePersonWithAssignedlId;
import org.springframework.data.neo4j.integration.shared.common.ImmutablePersonWithAssignedIdRelationshipProperties;
import org.springframework.data.neo4j.integration.shared.common.ImmutableSecondPersonWithAssignedId;
import org.springframework.data.neo4j.integration.shared.common.ImmutableSecondPersonWithAssignedIdRelationshipProperties;
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
public class ImmutableAssignedIdsIT {

	public static final String SOME_VALUE_VALUE = "testValue";
	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@Test // GH-2141
	void saveWithGeneratedIdsReturnsObjectWithIdSet(
			@Autowired ImmutablePersonWithExternalIdRepository repository) {

		ImmutablePersonWithAssignedlId fallback1 = new ImmutablePersonWithAssignedlId();
		ImmutablePersonWithAssignedlId fallback2 = ImmutablePersonWithAssignedlId.fallback(fallback1);
		ImmutablePersonWithAssignedlId person = ImmutablePersonWithAssignedlId.fallback(fallback2);

		ImmutablePersonWithAssignedlId savedPerson = repository.save(person);

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

		ImmutablePersonWithAssignedlId fallback1 = new ImmutablePersonWithAssignedlId();
		ImmutablePersonWithAssignedlId fallback2 = ImmutablePersonWithAssignedlId.fallback(fallback1);
		ImmutablePersonWithAssignedlId person = ImmutablePersonWithAssignedlId.fallback(fallback2);

		ImmutablePersonWithAssignedlId savedPerson = repository.saveAll(Collections.singleton(person)).get(0);

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

		ImmutablePersonWithAssignedlId onboarder = new ImmutablePersonWithAssignedlId();
		ImmutablePersonWithAssignedlId person = ImmutablePersonWithAssignedlId.wasOnboardedBy(Collections.singletonList(onboarder));

		ImmutablePersonWithAssignedlId savedPerson = repository.saveAll(Collections.singleton(person)).get(0);

		assertThat(savedPerson.wasOnboardedBy.get(0).id).isNotNull();
		assertThat(savedPerson.wasOnboardedBy.get(0).someValue).isEqualTo(SOME_VALUE_VALUE);
	}

	@Test // GH-2148
	void saveRelationshipWithGeneratedIdsContainsObjectWithIdSetForSet(
			@Autowired ImmutablePersonWithExternalIdRepository repository) {

		ImmutablePersonWithAssignedlId knowingPerson = new ImmutablePersonWithAssignedlId();
		ImmutablePersonWithAssignedlId person = ImmutablePersonWithAssignedlId.knownBy(Collections.singleton(knowingPerson));

		ImmutablePersonWithAssignedlId savedPerson = repository.saveAll(Collections.singleton(person)).get(0);

		assertThat(savedPerson.knownBy.iterator().next().id).isNotNull();
		assertThat(savedPerson.knownBy.iterator().next().someValue).isEqualTo(SOME_VALUE_VALUE);
	}

	@Test // GH-2148
	void saveRelationshipWithGeneratedIdsContainsObjectWithIdSetForMap(
			@Autowired ImmutablePersonWithExternalIdRepository repository) {

		ImmutablePersonWithAssignedlId rater = new ImmutablePersonWithAssignedlId();
		ImmutablePersonWithAssignedlId person = ImmutablePersonWithAssignedlId.ratedBy(Collections.singletonMap("Good", rater));

		ImmutablePersonWithAssignedlId savedPerson = repository.saveAll(Collections.singleton(person)).get(0);

		assertThat(savedPerson.ratedBy.keySet().iterator().next()).isEqualTo("Good");
		assertThat(savedPerson.ratedBy.values().iterator().next().id).isNotNull();
		assertThat(savedPerson.ratedBy.values().iterator().next().someValue).isEqualTo(SOME_VALUE_VALUE);
	}

	@Test // GH-2148
	void saveRelationshipWithGeneratedIdsContainsObjectWithIdSetForMapWithMultipleKeys(
			@Autowired ImmutablePersonWithExternalIdRepository repository) {

		ImmutablePersonWithAssignedlId rater1 = new ImmutablePersonWithAssignedlId();
		ImmutablePersonWithAssignedlId rater2 = new ImmutablePersonWithAssignedlId();
		Map<String, ImmutablePersonWithAssignedlId> raterMap = new HashMap<>();
		raterMap.put("Good", rater1);
		raterMap.put("Bad", rater2);
		ImmutablePersonWithAssignedlId person = ImmutablePersonWithAssignedlId.ratedBy(raterMap);

		ImmutablePersonWithAssignedlId savedPerson = repository.saveAll(Collections.singleton(person)).get(0);

		assertThat(savedPerson.ratedBy.keySet()).containsExactlyInAnyOrder("Good", "Bad");
		assertThat(savedPerson.ratedBy.get("Good").id).isNotNull();
		assertThat(savedPerson.ratedBy.get("Good").someValue).isEqualTo(SOME_VALUE_VALUE);
		assertThat(savedPerson.ratedBy.get("Bad").id).isNotNull();
		assertThat(savedPerson.ratedBy.get("Bad").someValue).isEqualTo(SOME_VALUE_VALUE);
	}

	@Test // GH-2148
	void saveRelationshipWithGeneratedIdsContainsObjectWithIdSetForMapCollection(
			@Autowired ImmutablePersonWithExternalIdRepository repository) {

		ImmutableSecondPersonWithAssignedId rater = new ImmutableSecondPersonWithAssignedId();
		ImmutablePersonWithAssignedlId person = ImmutablePersonWithAssignedlId.ratedByCollection(Collections.singletonMap("Good", Collections.singletonList(rater)));

		ImmutablePersonWithAssignedlId savedPerson = repository.saveAll(Collections.singleton(person)).get(0);

		assertThat(savedPerson.ratedByCollection.values().iterator().next().get(0).id).isNotNull();
	}

	@Test // GH-2148
	void saveRelationshipWithGeneratedIdsContainsObjectWithIdSetForRelationshipProperties(
			@Autowired ImmutablePersonWithExternalIdRepository repository) {

		ImmutablePersonWithAssignedlId somebody = new ImmutablePersonWithAssignedlId();
		ImmutablePersonWithAssignedIdRelationshipProperties properties = new ImmutablePersonWithAssignedIdRelationshipProperties(null, "blubb", somebody);
		ImmutablePersonWithAssignedlId person = ImmutablePersonWithAssignedlId.relationshipProperties(properties);

		ImmutablePersonWithAssignedlId savedPerson = repository.saveAll(Collections.singleton(person)).get(0);

		assertThat(savedPerson.relationshipProperties.name).isNotNull();
		assertThat(savedPerson.relationshipProperties.target.id).isNotNull();
		assertThat(savedPerson.relationshipProperties.target.someValue).isEqualTo(SOME_VALUE_VALUE);
	}

	@Test // GH-2148
	void saveRelationshipWithGeneratedIdsContainsObjectWithIdSetForRelationshipPropertiesCollection(
			@Autowired ImmutablePersonWithExternalIdRepository repository) {

		ImmutablePersonWithAssignedlId somebody = new ImmutablePersonWithAssignedlId();
		ImmutablePersonWithAssignedIdRelationshipProperties properties = new ImmutablePersonWithAssignedIdRelationshipProperties(null, "blubb", somebody);
		ImmutablePersonWithAssignedlId person = ImmutablePersonWithAssignedlId.relationshipPropertiesCollection(Collections.singletonList(properties));

		ImmutablePersonWithAssignedlId savedPerson = repository.saveAll(Collections.singleton(person)).get(0);

		assertThat(savedPerson.relationshipPropertiesCollection.get(0).name).isNotNull();
		assertThat(savedPerson.relationshipPropertiesCollection.get(0).target.id).isNotNull();
		assertThat(savedPerson.relationshipPropertiesCollection.get(0).target.someValue).isEqualTo(SOME_VALUE_VALUE);
	}

	@Test // GH-2148
	void saveRelationshipWithGeneratedIdsContainsObjectWithIdSetForRelationshipPropertiesDynamic(
			@Autowired ImmutablePersonWithExternalIdRepository repository) {

		ImmutablePersonWithAssignedlId somebody = new ImmutablePersonWithAssignedlId();
		ImmutablePersonWithAssignedIdRelationshipProperties properties = new ImmutablePersonWithAssignedIdRelationshipProperties(null, "blubb", somebody);
		ImmutablePersonWithAssignedlId person = ImmutablePersonWithAssignedlId.relationshipPropertiesDynamic(Collections.singletonMap("Good", properties));

		ImmutablePersonWithAssignedlId savedPerson = repository.saveAll(Collections.singleton(person)).get(0);

		assertThat(savedPerson.relationshipPropertiesDynamic.keySet().iterator().next()).isEqualTo("Good");
		assertThat(savedPerson.relationshipPropertiesDynamic.values().iterator().next().name).isNotNull();
		assertThat(savedPerson.relationshipPropertiesDynamic.values().iterator().next().target.id).isNotNull();
		assertThat(savedPerson.relationshipPropertiesDynamic.values().iterator().next().target.someValue).isEqualTo(SOME_VALUE_VALUE);
	}


	@Test // GH-2148
	void saveRelationshipWithGeneratedIdsContainsObjectWithIdSetForRelationshipPropertiesDynamicCollection(
			@Autowired ImmutablePersonWithExternalIdRepository repository) {

		ImmutableSecondPersonWithAssignedId somebody = new ImmutableSecondPersonWithAssignedId();
		ImmutableSecondPersonWithAssignedIdRelationshipProperties properties = new ImmutableSecondPersonWithAssignedIdRelationshipProperties(null, "blubb", somebody);
		ImmutablePersonWithAssignedlId person = ImmutablePersonWithAssignedlId.relationshipPropertiesDynamicCollection(Collections.singletonMap("Good", Collections.singletonList(properties)));

		ImmutablePersonWithAssignedlId savedPerson = repository.saveAll(Collections.singleton(person)).get(0);

		assertThat(savedPerson.relationshipPropertiesDynamicCollection.keySet().iterator().next()).isEqualTo("Good");
		assertThat(savedPerson.relationshipPropertiesDynamicCollection.values().iterator().next().get(0).name).isNotNull();
		assertThat(savedPerson.relationshipPropertiesDynamicCollection.values().iterator().next().get(0).target.id).isNotNull();
	}

	@Test // GH-2148
	void saveRelationshipWithGeneratedIdsContainsAllRelationshipTypes(
			@Autowired ImmutablePersonWithExternalIdRepository repository) {

		ImmutablePersonWithAssignedlId fallback =
				new ImmutablePersonWithAssignedlId();

		List<ImmutablePersonWithAssignedlId> wasOnboardedBy =
				Collections.singletonList(new ImmutablePersonWithAssignedlId());

		Set<ImmutablePersonWithAssignedlId> knownBy =
				Collections.singleton(new ImmutablePersonWithAssignedlId());

		Map<String, ImmutablePersonWithAssignedlId> ratedBy =
				Collections.singletonMap("Good", new ImmutablePersonWithAssignedlId());

		Map<String, List<ImmutableSecondPersonWithAssignedId>> ratedByCollection =
				Collections.singletonMap("Na", Collections.singletonList(new ImmutableSecondPersonWithAssignedId()));

		ImmutablePersonWithAssignedIdRelationshipProperties relationshipProperties =
				new ImmutablePersonWithAssignedIdRelationshipProperties(null, "rel1", new ImmutablePersonWithAssignedlId());

		List<ImmutablePersonWithAssignedIdRelationshipProperties> relationshipPropertiesCollection =
				Collections.singletonList(new ImmutablePersonWithAssignedIdRelationshipProperties(null, "rel2", new ImmutablePersonWithAssignedlId()));

		Map<String, ImmutablePersonWithAssignedIdRelationshipProperties> relationshipPropertiesDynamic =
				Collections.singletonMap("Ok", new ImmutablePersonWithAssignedIdRelationshipProperties(null, "rel3", new ImmutablePersonWithAssignedlId()));

		Map<String, List<ImmutableSecondPersonWithAssignedIdRelationshipProperties>> relationshipPropertiesDynamicCollection =
				Collections.singletonMap("Nope",
						Collections.singletonList(new ImmutableSecondPersonWithAssignedIdRelationshipProperties(
								null, "rel4", new ImmutableSecondPersonWithAssignedId()))
				);

		ImmutablePersonWithAssignedlId person = new ImmutablePersonWithAssignedlId(null,
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

		ImmutablePersonWithAssignedlId savedPerson = repository.saveAll(Collections.singleton(person)).get(0);

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

	interface ImmutablePersonWithExternalIdRepository extends Neo4jRepository<ImmutablePersonWithAssignedlId, Long> {}

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
			return Arrays.asList(ImmutablePersonWithAssignedlId.class.getPackage().getName());
		}

		@Bean
		BeforeBindCallback<ImmutablePersonWithAssignedlId> valueChange() {
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

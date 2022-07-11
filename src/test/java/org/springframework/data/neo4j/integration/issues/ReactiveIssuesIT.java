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
package org.springframework.data.neo4j.integration.issues;

import static org.assertj.core.api.Assertions.assertThat;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.ReactiveNeo4jTemplate;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;
import org.springframework.data.neo4j.integration.issues.gh2289.ReactiveSkuRORepository;
import org.springframework.data.neo4j.integration.issues.gh2289.ReactiveSkuRepository;
import org.springframework.data.neo4j.integration.issues.gh2289.RelationType;
import org.springframework.data.neo4j.integration.issues.gh2289.Sku;
import org.springframework.data.neo4j.integration.issues.gh2289.SkuRO;
import org.springframework.data.neo4j.integration.issues.gh2326.AbstractLevel2;
import org.springframework.data.neo4j.integration.issues.gh2326.BaseEntity;
import org.springframework.data.neo4j.integration.issues.gh2326.ReactiveAnimalRepository;
import org.springframework.data.neo4j.integration.issues.gh2328.ReactiveEntity2328Repository;
import org.springframework.data.neo4j.integration.issues.gh2347.ReactiveApplicationRepository;
import org.springframework.data.neo4j.integration.issues.gh2347.ReactiveWorkflowRepository;
import org.springframework.data.neo4j.integration.issues.gh2500.Device;
import org.springframework.data.neo4j.integration.issues.gh2500.Group;
import org.springframework.data.neo4j.integration.issues.gh2533.EntitiesAndProjections;
import org.springframework.data.neo4j.integration.issues.gh2533.ReactiveGH2533Repository;
import org.springframework.data.neo4j.integration.issues.gh2572.GH2572Child;
import org.springframework.data.neo4j.integration.issues.gh2572.ReactiveGH2572Repository;
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.data.neo4j.test.Neo4jReactiveTestConfiguration;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 * @soundtrack Sodom - Sodom
 */
@Neo4jIntegrationTest
@DisplayNameGeneration(SimpleDisplayNameGeneratorWithTags.class)
@TestMethodOrder(MethodOrderer.DisplayName.class)
class ReactiveIssuesIT extends TestBase {

	@BeforeAll
	protected static void setupData(@Autowired BookmarkCapture bookmarkCapture) {
		try (Session session = neo4jConnectionSupport.getDriver().session(bookmarkCapture.createSessionConfig())) {
			try (Transaction transaction = session.beginTransaction()) {
				transaction.run("MATCH (n) detach delete n");

				setupGH2289(transaction);
				setupGH2328(transaction);
				setupGH2572(transaction);

				transaction.commit();
			}
			bookmarkCapture.seedWith(session.lastBookmarks());
		}
	}

	@RepeatedTest(23)
	@Tag("GH-2289")
	void testNewRelation(@Autowired ReactiveSkuRepository skuRepo) {

		AtomicLong aId = new AtomicLong();
		AtomicLong bId = new AtomicLong();
		AtomicReference<Sku> cRef = new AtomicReference<>();
		skuRepo.save(new Sku(0L, "A"))
				.zipWith(skuRepo.save(new Sku(1L, "B")))
				.zipWith(skuRepo.save(new Sku(2L, "C")))
				.zipWith(skuRepo.save(new Sku(3L, "D"))).flatMap(t -> {
					Sku a = t.getT1().getT1().getT1();
					Sku b = t.getT1().getT1().getT2();
					Sku c = t.getT1().getT2();
					Sku d = t.getT2();

					bId.set(b.getId());
					cRef.set(c);
					a.rangeRelationTo(b, 1, 1, RelationType.MULTIPLICATIVE);
					a.rangeRelationTo(c, 1, 1, RelationType.MULTIPLICATIVE);
					a.rangeRelationTo(d, 1, 1, RelationType.MULTIPLICATIVE);
					return skuRepo.save(a);
				}).as(StepVerifier::create)
				.expectNextMatches(a -> {
					aId.set(a.getId()); // side-effects for the win
					return a.getRangeRelationsOut().size() == 3;
				})
				.verifyComplete();

		skuRepo.findById(bId.get())
				.doOnNext(b -> assertThat(b.getRangeRelationsIn()).hasSize(1))
				.flatMap(b -> {
					b.rangeRelationTo(cRef.get(), 1, 1, RelationType.MULTIPLICATIVE);
					return skuRepo.save(b);
				})
				.as(StepVerifier::create)
				.assertNext(b -> {
					assertThat(b.getRangeRelationsIn()).hasSize(1);
					assertThat(b.getRangeRelationsOut()).hasSize(1);
				})
				.verifyComplete();

		skuRepo.findById(aId.get())
				.as(StepVerifier::create)
				.assertNext(a -> {
					assertThat(a.getRangeRelationsOut()).hasSize(3);
					assertThat(a.getRangeRelationsOut()).allSatisfy(r -> {
						int expectedSize = 1;
						if ("C".equals(r.getTargetSku().getName())) {
							expectedSize = 2;
						}
						assertThat(r.getTargetSku().getRangeRelationsIn()).hasSize(expectedSize);
					});
				})
				.verifyComplete();
	}

	@RepeatedTest(5)
	@Tag("GH-2289")
	@Tag("GH-2294")
	void testNewRelationRo(@Autowired ReactiveSkuRORepository skuRepo) {

		AtomicLong bId = new AtomicLong();
		AtomicReference<SkuRO> cRef = new AtomicReference<>();
		skuRepo.findOneByName("A")
				.zipWith(skuRepo.findOneByName("B"))
				.zipWith(skuRepo.findOneByName("C"))
				.zipWith(skuRepo.findOneByName("D"))
				.flatMap(t -> {
					SkuRO a = t.getT1().getT1().getT1();
					SkuRO b = t.getT1().getT1().getT2();
					SkuRO c = t.getT1().getT2();
					SkuRO d = t.getT2();

					bId.set(b.getId());
					cRef.set(c);
					a.rangeRelationTo(b, 1, 1, RelationType.MULTIPLICATIVE);
					a.rangeRelationTo(c, 1, 1, RelationType.MULTIPLICATIVE);
					a.rangeRelationTo(d, 1, 1, RelationType.MULTIPLICATIVE);

					a.setName("a new name");

					return skuRepo.save(a);
				}).as(StepVerifier::create)
				.expectNextMatches(a -> a.getRangeRelationsOut().size() == 3 && "a new name".equals(a.getName()))
				.verifyComplete();

		skuRepo.findOneByName("a new name")
				.as(StepVerifier::create)
				.verifyComplete();

		skuRepo.findOneByName("B")
				.doOnNext(b -> {
					assertThat(b.getRangeRelationsIn()).hasSize(1);
					assertThat(b.getRangeRelationsOut()).hasSizeLessThanOrEqualTo(1);
				})
				.flatMap(b -> {
					b.rangeRelationTo(cRef.get(), 1, 1, RelationType.MULTIPLICATIVE);
					return skuRepo.save(b);
				})
				.as(StepVerifier::create)
				.expectNextMatches(b -> b.getRangeRelationsIn().size() == 1 && b.getRangeRelationsOut().size() == 1)
				.verifyComplete();
	}

	@Test
	@Tag("GH-2326")
	void saveShouldAddAllLabels(@Autowired ReactiveAnimalRepository animalRepository,
			@Autowired BookmarkCapture bookmarkCapture) {

		List<String> ids = new ArrayList<>();
		List<AbstractLevel2> animals = Arrays.asList(new AbstractLevel2.AbstractLevel3.Concrete1(),
				new AbstractLevel2.AbstractLevel3.Concrete2());
		Flux.fromIterable(animals).flatMap(animalRepository::save)
				.map(BaseEntity::getId)
				.as(StepVerifier::create)
				.recordWith(() -> ids)
				.expectNextCount(2)
				.verifyComplete();

		assertLabels(bookmarkCapture, ids);
	}

	@Test
	@Tag("GH-2326")
	void saveAllShouldAddAllLabels(@Autowired ReactiveAnimalRepository animalRepository,
			@Autowired BookmarkCapture bookmarkCapture) {

		List<String> ids = new ArrayList<>();
		List<AbstractLevel2> animals = Arrays.asList(new AbstractLevel2.AbstractLevel3.Concrete1(),
				new AbstractLevel2.AbstractLevel3.Concrete2());
		animalRepository.saveAll(animals)
				.map(BaseEntity::getId)
				.as(StepVerifier::create)
				.recordWith(() -> ids)
				.expectNextCount(2)
				.verifyComplete();

		assertLabels(bookmarkCapture, ids);
	}

	@Test
	@Tag("GH-2328")
	void queriesFromCustomLocationsShouldBeFound(@Autowired ReactiveEntity2328Repository someRepository) {

		someRepository.getSomeEntityViaNamedQuery()
				.as(StepVerifier::create)
				.expectNextMatches(TestBase::requirements)
				.verifyComplete();
	}

	@Test
	@Tag("GH-2347")
	void entitiesWithAssignedIdsSavedInBatchMustBeIdentifiableWithTheirInternalIds(
			@Autowired ReactiveApplicationRepository applicationRepository,
			@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture
	) {
		applicationRepository
				.saveAll(Collections.singletonList(createData()))
				.as(StepVerifier::create)
				.expectNextCount(1L)
				.verifyComplete();

		assertSingleApplicationNodeWithMultipleWorkflows(driver, bookmarkCapture);
	}

	@Test
	@Tag("GH-2347")
	void entitiesWithAssignedIdsMustBeIdentifiableWithTheirInternalIds(
			@Autowired ReactiveApplicationRepository applicationRepository,
			@Autowired Driver driver,
			@Autowired BookmarkCapture bookmarkCapture
	) {
		applicationRepository
				.save(createData())
				.as(StepVerifier::create)
				.expectNextCount(1L)
				.verifyComplete();
		assertSingleApplicationNodeWithMultipleWorkflows(driver, bookmarkCapture);
	}

	@Test
	@Tag("GH-2346")
	void relationshipsStartingAtEntitiesWithAssignedIdsShouldBeCreated(
			@Autowired ReactiveApplicationRepository applicationRepository,
			@Autowired Driver driver,
			@Autowired BookmarkCapture bookmarkCapture
	) {
		createData((applications, workflows) -> {
			applicationRepository.saveAll(applications)
					.as(StepVerifier::create)
					.expectNextCount(2L)
					.verifyComplete();

			assertMultipleApplicationsNodeWithASingleWorkflow(driver, bookmarkCapture);
		});
	}

	@Test
	@Tag("GH-2346")
	void relationshipsStartingAtEntitiesWithAssignedIdsShouldBeCreatedOtherDirection(
			@Autowired ReactiveWorkflowRepository workflowRepository,
			@Autowired Driver driver,
			@Autowired BookmarkCapture bookmarkCapture
	) {
		createData((applications, workflows) -> {
			workflowRepository.saveAll(workflows)
					.as(StepVerifier::create)
					.expectNextCount(2L)
					.verifyComplete();

			assertMultipleApplicationsNodeWithASingleWorkflow(driver, bookmarkCapture);
		});
	}

	@Test
	@Tag("GH-2498")
	void shouldNotDeleteFreshlyCreatedRelationships(@Autowired Driver driver, @Autowired
			ReactiveNeo4jTemplate template) {

		Group group = new Group();
		group.setName("test");
		template.findById(1L, Device.class)
				.flatMap(d -> {
					group.getDevices().add(d);
					return template.save(group);
				})
				.as(StepVerifier::create)
				.expectNextCount(1L)
				.verifyComplete();

		try (Session session = driver.session()) {
			Map<String, Object> parameters = new HashMap<>();
			parameters.put("name", group.getName());
			parameters.put("deviceId", 1L);
			long cnt = session.run(
							"MATCH (g:Group {name: $name}) <-[:BELONGS_TO]- (d:Device {id: $deviceId}) RETURN count(*)",
							parameters)
					.single().get(0).asLong();
			assertThat(cnt).isOne();
		}
	}

	@Test
	@Tag("GH-2533")
	void projectionWorksForDynamicRelationshipsOnSave(@Autowired ReactiveGH2533Repository repository, @Autowired ReactiveNeo4jTemplate neo4jTemplate) {
		createData(repository)
				.flatMap(rootEntity -> repository.findByIdWithLevelOneLinks(rootEntity.id))
				.flatMap(rootEntity -> neo4jTemplate.saveAs(rootEntity,
						EntitiesAndProjections.GH2533EntityNodeWithOneLevelLinks.class))
				.flatMap(rootEntity -> neo4jTemplate.findById(rootEntity.getId(),
						EntitiesAndProjections.GH2533Entity.class))
				.as(StepVerifier::create)
				.assertNext(entity -> {
					assertThat(entity.relationships).isNotEmpty();
					assertThat(entity.relationships.get("has_relationship_with")).isNotEmpty();
					assertThat(entity.relationships.get("has_relationship_with").get(0).target).isNotNull();
					assertThat(
							entity.relationships.get("has_relationship_with").get(0).target.relationships).isNotEmpty();
				})
				.verifyComplete();
	}

	@Test
	@Tag("GH-2533")
	void saveRelatedEntityWithRelationships(@Autowired ReactiveGH2533Repository repository, @Autowired ReactiveNeo4jTemplate neo4jTemplate) {
		createData(repository)
				.flatMap(rootEntity -> repository.findByIdWithLevelOneLinks(rootEntity.id))
				.flatMap(rootEntity -> neo4jTemplate.saveAs(rootEntity,
						EntitiesAndProjections.GH2533EntityNodeWithOneLevelLinks.class))
				.flatMap(rootEntity -> neo4jTemplate.findById(rootEntity.getId(),
						EntitiesAndProjections.GH2533Entity.class))
				.as(StepVerifier::create)
				.assertNext(entity -> {
					assertThat(entity.relationships.get("has_relationship_with").get(0).target.name).isEqualTo("n2");
					assertThat(entity.relationships.get("has_relationship_with").get(0).target.relationships.get(
							"has_relationship_with").get(0).target.name).isEqualTo("n3");
				})
				.verifyComplete();
	}

	@Test
	@Tag("GH-2572")
	void allShouldFetchCorrectNumberOfChildNodes(@Autowired ReactiveGH2572Repository reactiveGH2572Repository) {
		reactiveGH2572Repository.getDogsForPerson("GH2572Parent-2")
				.as(StepVerifier::create)
				.expectNextCount(2L)
				.verifyComplete();
	}

	@Test
	@Tag("GH-2572")
	void allShouldNotFailWithoutMatchingRootNodes(@Autowired ReactiveGH2572Repository reactiveGH2572Repository) {
		reactiveGH2572Repository.getDogsForPerson("GH2572Parent-1")
				.as(StepVerifier::create)
				.expectNextCount(0L)
				.verifyComplete();
	}

	@Test
	@Tag("GH-2572")
	void oneShouldFetchCorrectNumberOfChildNodes(@Autowired ReactiveGH2572Repository reactiveGH2572Repository) {
		reactiveGH2572Repository.findOneDogForPerson("GH2572Parent-2")
				.map(GH2572Child::getName)
				.as(StepVerifier::create)
				.expectNext("a-pet")
				.verifyComplete();
	}

	@Test
	@Tag("GH-2572")
	void oneShouldNotFailWithoutMatchingRootNodes(@Autowired ReactiveGH2572Repository reactiveGH2572Repository) {
		reactiveGH2572Repository.findOneDogForPerson("GH2572Parent-1")
				.as(StepVerifier::create)
				.expectNextCount(0L)
				.verifyComplete();
	}

	@Configuration
	@EnableTransactionManagement
	@EnableReactiveNeo4jRepositories(namedQueriesLocation = "more-custom-queries.properties")
	static class Config extends Neo4jReactiveTestConfiguration {

		@Bean
		public Driver driver() {

			return neo4jConnectionSupport.getDriver();
		}

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Override
		public ReactiveTransactionManager reactiveTransactionManager(Driver driver,
				ReactiveDatabaseSelectionProvider databaseSelectionProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new ReactiveNeo4jTransactionManager(driver, databaseSelectionProvider,
					Neo4jBookmarkManager.create(bookmarkCapture));
		}

		@Override
		public boolean isCypher5Compatible() {
			return neo4jConnectionSupport.isCypher5SyntaxCompatible();
		}
	}

	private Mono<EntitiesAndProjections.GH2533Entity> createData(ReactiveGH2533Repository repository) {
		EntitiesAndProjections.GH2533Entity n1 = new EntitiesAndProjections.GH2533Entity();
		EntitiesAndProjections.GH2533Entity n2 = new EntitiesAndProjections.GH2533Entity();
		EntitiesAndProjections.GH2533Entity n3 = new EntitiesAndProjections.GH2533Entity();

		EntitiesAndProjections.GH2533Relationship r1 = new EntitiesAndProjections.GH2533Relationship();
		EntitiesAndProjections.GH2533Relationship r2 = new EntitiesAndProjections.GH2533Relationship();

		n1.name = "n1";
		n2.name = "n2";
		n3.name = "n3";

		r1.target = n2;
		r2.target = n3;

		n1.relationships = Collections.singletonMap("has_relationship_with", List.of(r1));
		n2.relationships = Collections.singletonMap("has_relationship_with", List.of(r2));

		return repository.save(n1);
	}

}

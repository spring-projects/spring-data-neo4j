/*
 * Copyright 2011-2024 the original author or authors.
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

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.LabelExpression;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Relationship;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.data.neo4j.integration.issues.gh2905.BugFromV1;
import org.springframework.data.neo4j.integration.issues.gh2905.BugRelationshipV1;
import org.springframework.data.neo4j.integration.issues.gh2905.BugTargetV1;
import org.springframework.data.neo4j.integration.issues.gh2905.FromRepositoryV1;
import org.springframework.data.neo4j.integration.issues.gh2905.ReactiveFromRepositoryV1;
import org.springframework.data.neo4j.integration.issues.gh2905.ReactiveToRepositoryV1;
import org.springframework.data.neo4j.integration.issues.gh2905.ToRepositoryV1;
import org.springframework.data.neo4j.integration.issues.gh2906.BugFrom;
import org.springframework.data.neo4j.integration.issues.gh2906.BugTarget;
import org.springframework.data.neo4j.integration.issues.gh2906.BugTargetContainer;
import org.springframework.data.neo4j.integration.issues.gh2906.FromRepository;
import org.springframework.data.neo4j.integration.issues.gh2906.OutgoingBugRelationship;
import org.springframework.data.neo4j.integration.issues.gh2906.ReactiveFromRepository;
import org.springframework.data.neo4j.integration.issues.gh2906.ReactiveToRepository;
import org.springframework.data.neo4j.integration.issues.gh2906.ToRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

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

				setupGH2328(transaction);
				setupGH2572(transaction);

				transaction.commit();
			}
			bookmarkCapture.seedWith(session.lastBookmarks());
		}
	}

	@BeforeEach
	void setup(@Autowired BookmarkCapture bookmarkCapture) {
		List<String> labelsToBeRemoved = List.of("BugFromV1", "BugFrom", "BugTargetV1", "BugTarget", "BugTargetBaseV1", "BugTargetBase", "BugTargetContainer");
		var labelExpression = new LabelExpression(labelsToBeRemoved.get(0));
		for (int i = 1; i < labelsToBeRemoved.size(); i++) {
			labelExpression = labelExpression.or(new LabelExpression(labelsToBeRemoved.get(i)));
		}
		try (Session session = neo4jConnectionSupport.getDriver().session(bookmarkCapture.createSessionConfig())) {
			try (Transaction transaction = session.beginTransaction()) {
				setupGH2289(transaction);
				Node nodes = Cypher.node(labelExpression);
				String cypher = Cypher.match(nodes).detachDelete(nodes).build().getCypher();
				transaction.run(cypher).consume();
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

	@Test
	@Tag("GH-2905")
	void storeFromRootAggregate(@Autowired ReactiveToRepositoryV1 toRepositoryV1, @Autowired Driver driver) {
		var to1 = BugTargetV1.builder().name("T1").type("BUG").build();

		var from1 = BugFromV1.builder()
				.name("F1")
				.reli(BugRelationshipV1.builder().target(to1).comment("F1<-T1").build())
				.build();
		var from2 = BugFromV1.builder()
				.name("F2")
				.reli(BugRelationshipV1.builder().target(to1).comment("F2<-T1").build())
				.build();
		var from3 = BugFromV1.builder()
				.name("F3")
				.reli(BugRelationshipV1.builder().target(to1).comment("F3<-T1").build())
				.build();

		to1.relatedBugs = Set.of(from1, from2, from3);
		toRepositoryV1.save(to1).block();

		assertGH2905Graph(driver);
	}

	@Test
	@Tag("GH-2905")
	void saveSingleEntities(@Autowired ReactiveFromRepositoryV1 fromRepositoryV1, @Autowired ReactiveToRepositoryV1 toRepositoryV1, @Autowired Driver driver) {
		var to1 = BugTargetV1.builder().name("T1").type("BUG").build();
		to1.relatedBugs = new HashSet<>();
		to1 = toRepositoryV1.save(to1).block();

		var from1 = BugFromV1.builder()
				.name("F1")
				.reli(BugRelationshipV1.builder().target(to1).comment("F1<-T1").build())
				.build();
		// This is the key to solve 2905 when you had the annotation previously, you must maintain both ends of the bidirectional relationship.
		// SDN does not do this for you.
		to1.relatedBugs.add(from1);
		from1 = fromRepositoryV1.save(from1).block();

		var from2 = BugFromV1.builder()
				.name("F2")
				.reli(BugRelationshipV1.builder().target(to1).comment("F2<-T1").build())
				.build();
		// See above
		to1.relatedBugs.add(from2);

		var from3 = BugFromV1.builder()
				.name("F3")
				.reli(BugRelationshipV1.builder().target(to1).comment("F3<-T1").build())
				.build();
		to1.relatedBugs.add(from3);
		// See above
		fromRepositoryV1.saveAll(List.of(from1, from2, from3)).collectList().block();

		assertGH2905Graph(driver);
	}

	private static void assertGH2905Graph(Driver driver) {
		var result = driver.executableQuery("MATCH (t:BugTargetV1) -[:RELI] ->(f:BugFromV1) RETURN t, collect(f) AS f").execute().records();
		assertThat(result)
				.hasSize(1)
				.element(0).satisfies(r -> {
					assertThat(r.get("t")).matches(TypeSystem.getDefault().NODE()::isTypeOf);
					assertThat(r.get("f"))
							.matches(TypeSystem.getDefault().LIST()::isTypeOf)
							.extracting(Value::asList, as(InstanceOfAssertFactories.LIST))
							.hasSize(3);
				});
	}

	@Test
	@Tag("GH-2906")
	void storeFromRootAggregateToLeaf(@Autowired ReactiveToRepository toRepository, @Autowired Driver driver) {
		var to1 = new BugTarget("T1", "BUG");

		var from1 = new BugFrom("F1", "F1<-T1", to1);
		var from2 = new BugFrom("F2", "F2<-T1", to1);
		var from3 = new BugFrom("F3", "F3<-T1", to1);

		to1.relatedBugs = Set.of(
				new OutgoingBugRelationship(from1.reli.comment, from1),
				new OutgoingBugRelationship(from2.reli.comment, from2),
				new OutgoingBugRelationship(from3.reli.comment, from3)
		);
		toRepository.save(to1).block();

		assertGH2906Graph(driver);
	}


	@Test
	@Tag("GH-2906")
	void storeFromRootAggregateToContainer(@Autowired ReactiveToRepository toRepository, @Autowired Driver driver) {

		var t1 = new BugTarget("T1", "BUG");
		var t2 = new BugTarget("T2", "BUG");

		var to1 = new BugTargetContainer("C1");
		to1.items.add(t1);
		to1.items.add(t2);

		var from1 = new BugFrom("F1", "F1<-T1", to1);
		var from2 = new BugFrom("F2", "F2<-T1", to1);
		var from3 = new BugFrom("F3", "F3<-T1", to1);

		to1.relatedBugs = Set.of(
				new OutgoingBugRelationship(from1.reli.comment, from1),
				new OutgoingBugRelationship(from2.reli.comment, from2),
				new OutgoingBugRelationship(from3.reli.comment, from3)
		);
		toRepository.save(to1).block();

		assertGH2906Graph(driver);
	}

	@Test
	@Tag("GH-2906")
	void saveSingleEntitiesToLeaf(@Autowired ReactiveFromRepository fromRepository, @Autowired ReactiveToRepository toRepository, @Autowired Driver driver) {

		var to1 = new BugTarget("T1", "BUG");
		to1 = toRepository.save(to1).block();

		var from1 = new BugFrom("F1", "F1<-T1", to1);
		to1.relatedBugs.add(new OutgoingBugRelationship(from1.reli.comment, from1));
		from1 = fromRepository.save(from1).block();

		assertThat(from1.reli.id).isNotNull();
		assertThat(from1.reli.target.relatedBugs).first().extracting(r -> r.id).isNotNull();

		var from2 = new BugFrom("F2", "F2<-T1", to1);
		to1.relatedBugs.add(new OutgoingBugRelationship(from2.reli.comment, from2));

		var from3 = new BugFrom("F3", "F3<-T1", to1);
		to1.relatedBugs.add(new OutgoingBugRelationship(from3.reli.comment, from3));

		// See above
		var bugs = fromRepository.saveAll(List.of(from1, from2, from3)).collectList().block();
		for (BugFrom from : bugs) {
			assertThat(from.reli.id).isNotNull();
			assertThat(from.reli.target.relatedBugs).first().extracting(r -> r.id).isNotNull();
		}

		assertGH2906Graph(driver);
	}

	@Test
	@Tag("GH-2906")
	void saveSingleEntitiesToContainer(@Autowired ReactiveFromRepository fromRepository, @Autowired ReactiveToRepository toRepository, @Autowired Driver driver) {

		var t1 = new BugTarget("T1", "BUG");
		var t2 = new BugTarget("T2", "BUG");

		var to1 = new BugTargetContainer("C1");
		to1.items.add(t1);
		to1.items.add(t2);

		to1 = toRepository.save(to1).block();

		var from1 = new BugFrom("F1", "F1<-T1", to1);
		to1.relatedBugs.add(new OutgoingBugRelationship(from1.reli.comment, from1));

		var from2 = new BugFrom("F2", "F2<-T1", to1);
		to1.relatedBugs.add(new OutgoingBugRelationship(from2.reli.comment, from2));

		var from3 = new BugFrom("F3", "F3<-T1", to1);
		to1.relatedBugs.add(new OutgoingBugRelationship(from3.reli.comment, from3));

		// See above
		fromRepository.saveAll(List.of(from1, from2, from3));

		assertGH2906Graph(driver);
	}

	@Test
	@Tag("GH-2906")
	void saveSingleEntitiesViaServiceToContainer(@Autowired ReactiveFromRepository fromRepository, @Autowired ReactiveToRepository toRepository, @Autowired Driver driver) {

		var t1 = new BugTarget("T1", "BUG");
		var t2 = new BugTarget("T2", "BUG");

		var to1 = new BugTargetContainer("C1");
		to1.items.add(t1);
		to1.items.add(t2);

		to1 = toRepository.save(to1).block();
		var uuid = to1.uuid;
		to1 = null;

		var from1 = new BugFrom("F1", "F1<-T1", null);
		from1 = saveGH2906Entity(from1, uuid, fromRepository, toRepository).block();

		var from2 = new BugFrom("F2", "F2<-T1", null);
		from2 = saveGH2906Entity(from2, uuid, fromRepository, toRepository).block();

		var from3 = new BugFrom("F3", "F3<-T1", null);
		from3 = saveGH2906Entity(from3, uuid, fromRepository, toRepository).block();

		assertGH2906Graph(driver);
	}

	@Test
	@Tag("GH-2906")
	void saveTwoSingleEntitiesViaServiceToContainer(@Autowired ReactiveFromRepository fromRepository, @Autowired ReactiveToRepository toRepository, @Autowired Driver driver) {

		var t1 = new BugTarget("T1", "BUG");
		var t2 = new BugTarget("T2", "BUG");

		var to1 = new BugTargetContainer("C1");
		to1.items.add(t1);
		to1.items.add(t2);

		to1 = toRepository.save(to1).block();
		var uuid = to1.uuid;
		to1 = null;

		var from1 = new BugFrom("F1", "F1<-T1", null);
		from1 = saveGH2906Entity(from1, uuid, fromRepository, toRepository).block();

		var from2 = new BugFrom("F2", "F2<-T1", null);
		from2 = saveGH2906Entity(from2, uuid, fromRepository, toRepository).block();

		assertGH2906Graph(driver, 2);
	}

	@Test
	@Tag("GH-2906")
	void saveSingleEntitiesViaServiceToLeaf(@Autowired ReactiveFromRepository fromRepository, @Autowired ReactiveToRepository toRepository, @Autowired Driver driver) {

		var uuid = toRepository.save(new BugTarget("T1", "BUG")).block().uuid;

		var e1 = saveGH2906Entity(new BugFrom("F1", "F1<-T1", null), uuid, fromRepository, toRepository).block();

		assertThat(e1.reli.id).isNotNull();
		assertThat(e1.reli.target.relatedBugs).first().extracting(r -> r.id).isNotNull();

		e1 = saveGH2906Entity(new BugFrom("F2", "F2<-T1", null), uuid, fromRepository, toRepository).block();
		assertThat(e1.reli.id).isNotNull();
		assertThat(e1.reli.target.relatedBugs).first().extracting(r -> r.id).isNotNull();

		e1 = saveGH2906Entity(new BugFrom("F3", "F3<-T1", null), uuid, fromRepository, toRepository).block();
		assertThat(e1.reli.id).isNotNull();
		assertThat(e1.reli.target.relatedBugs).first().extracting(r -> r.id).isNotNull();

		assertGH2906Graph(driver);
	}

	@Test
	@Tag("GH-2906")
	void saveTwoSingleEntitiesViaServiceToLeaf(@Autowired ReactiveFromRepository fromRepository, @Autowired ReactiveToRepository toRepository, @Autowired Driver driver) {

		var to1 = new BugTarget("T1", "BUG");
		to1 = toRepository.save(to1).block();
		var uuid = to1.uuid;
		to1 = null;

		var from1 = new BugFrom("F1", "F1<-T1", null);
		from1 = saveGH2906Entity(from1, uuid, fromRepository, toRepository).block();

		var from2 = new BugFrom("F2", "F2<-T1", null);
		from2 = saveGH2906Entity(from2, uuid, fromRepository, toRepository).block();

		assertGH2906Graph(driver, 2);
	}

	private Mono<BugFrom> saveGH2906Entity(BugFrom from, String uuid, ReactiveFromRepository fromRepository, ReactiveToRepository toRepository) {
		var to = toRepository.findById(uuid).blockOptional().orElseThrow();

		from.reli.target = to;
		to.relatedBugs.add(new OutgoingBugRelationship(from.reli.comment, from));

		return fromRepository.save(from);
	}

	private static void assertGH2906Graph(Driver driver) {
		assertGH2906Graph(driver, 3);
	}

	private static void assertGH2906Graph(Driver driver, int cnt) {

		var expectedNodes = IntStream.rangeClosed(1, cnt).mapToObj(i -> String.format("F%d", i)).toArray(String[]::new);
		var expectedRelationships = IntStream.rangeClosed(1, cnt).mapToObj(i -> String.format("F%d<-T1", i)).toArray(String[]::new);

		var result = driver.executableQuery("MATCH (t:BugTargetBase) -[r:RELI] ->(f:BugFrom) RETURN t, collect(f) AS f, collect(r) AS r").execute().records();
		assertThat(result)
				.hasSize(1)
				.element(0).satisfies(r -> {
					assertThat(r.get("t")).matches(TypeSystem.getDefault().NODE()::isTypeOf);
					assertThat(r.get("f"))
							.matches(TypeSystem.getDefault().LIST()::isTypeOf)
							.extracting(Value::asList, as(InstanceOfAssertFactories.LIST))
							.map(node -> ((org.neo4j.driver.types.Node) node).get("name").asString())
							.containsExactlyInAnyOrder(expectedNodes);
					assertThat(r.get("r"))
							.matches(TypeSystem.getDefault().LIST()::isTypeOf)
							.extracting(Value::asList, as(InstanceOfAssertFactories.LIST))
							.map(rel -> ((Relationship) rel).get("comment").asString())
							.containsExactlyInAnyOrder(expectedRelationships);
				});
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
					Neo4jBookmarkManager.createReactive(bookmarkCapture));
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

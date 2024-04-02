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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.neo4j.cypherdsl.core.Condition;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Parameter;
import org.neo4j.cypherdsl.core.Property;
import org.neo4j.driver.Driver;
import org.neo4j.driver.QueryRunner;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Values;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.mapping.Constants;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.integration.issues.gh2168.DomainObject;
import org.springframework.data.neo4j.integration.issues.gh2168.DomainObjectRepository;
import org.springframework.data.neo4j.integration.issues.gh2168.UnrelatedObject;
import org.springframework.data.neo4j.integration.issues.gh2168.UnrelatedObjectPropertyConverterAsBean;
import org.springframework.data.neo4j.integration.issues.gh2210.SomeEntity;
import org.springframework.data.neo4j.integration.issues.gh2244.Step;
import org.springframework.data.neo4j.integration.issues.gh2289.RelationType;
import org.springframework.data.neo4j.integration.issues.gh2289.Sku;
import org.springframework.data.neo4j.integration.issues.gh2289.SkuRO;
import org.springframework.data.neo4j.integration.issues.gh2289.SkuRORepository;
import org.springframework.data.neo4j.integration.issues.gh2289.SkuRepository;
import org.springframework.data.neo4j.integration.issues.gh2323.Knows;
import org.springframework.data.neo4j.integration.issues.gh2323.Language;
import org.springframework.data.neo4j.integration.issues.gh2323.Person;
import org.springframework.data.neo4j.integration.issues.gh2323.PersonService;
import org.springframework.data.neo4j.integration.issues.gh2326.AbstractLevel2;
import org.springframework.data.neo4j.integration.issues.gh2326.AnimalRepository;
import org.springframework.data.neo4j.integration.issues.gh2326.BaseEntity;
import org.springframework.data.neo4j.integration.issues.gh2328.Entity2328Repository;
import org.springframework.data.neo4j.integration.issues.gh2347.Application;
import org.springframework.data.neo4j.integration.issues.gh2347.ApplicationRepository;
import org.springframework.data.neo4j.integration.issues.gh2347.Workflow;
import org.springframework.data.neo4j.integration.issues.gh2347.WorkflowRepository;
import org.springframework.data.neo4j.integration.issues.gh2415.BaseNodeEntity;
import org.springframework.data.neo4j.integration.issues.gh2415.NodeEntity;
import org.springframework.data.neo4j.integration.issues.gh2415.NodeWithDefinedCredentials;
import org.springframework.data.neo4j.integration.issues.gh2459.PetOwner;
import org.springframework.data.neo4j.integration.issues.gh2459.PetOwnerRepository;
import org.springframework.data.neo4j.integration.issues.gh2474.CityModel;
import org.springframework.data.neo4j.integration.issues.gh2474.CityModelDTO;
import org.springframework.data.neo4j.integration.issues.gh2474.CityModelRepository;
import org.springframework.data.neo4j.integration.issues.gh2474.PersonModel;
import org.springframework.data.neo4j.integration.issues.gh2474.PersonModelRepository;
import org.springframework.data.neo4j.integration.issues.gh2493.TestData;
import org.springframework.data.neo4j.integration.issues.gh2493.TestObject;
import org.springframework.data.neo4j.integration.issues.gh2493.TestObjectRepository;
import org.springframework.data.neo4j.integration.issues.gh2498.DomainModel;
import org.springframework.data.neo4j.integration.issues.gh2498.DomainModelRepository;
import org.springframework.data.neo4j.integration.issues.gh2498.Vertex;
import org.springframework.data.neo4j.integration.issues.gh2498.VertexRepository;
import org.springframework.data.neo4j.integration.issues.gh2500.Device;
import org.springframework.data.neo4j.integration.issues.gh2500.Group;
import org.springframework.data.neo4j.integration.issues.gh2526.BaseNodeRepository;
import org.springframework.data.neo4j.integration.issues.gh2526.DataPoint;
import org.springframework.data.neo4j.integration.issues.gh2526.Measurand;
import org.springframework.data.neo4j.integration.issues.gh2526.MeasurementProjection;
import org.springframework.data.neo4j.integration.issues.gh2530.InitialEntities;
import org.springframework.data.neo4j.integration.issues.gh2530.SomethingInBetweenRepository;
import org.springframework.data.neo4j.integration.issues.gh2533.EntitiesAndProjections;
import org.springframework.data.neo4j.integration.issues.gh2533.GH2533Repository;
import org.springframework.data.neo4j.integration.issues.gh2542.TestNode;
import org.springframework.data.neo4j.integration.issues.gh2542.TestNodeRepository;
import org.springframework.data.neo4j.integration.issues.gh2572.GH2572Repository;
import org.springframework.data.neo4j.integration.issues.gh2572.GH2572Child;
import org.springframework.data.neo4j.integration.issues.gh2576.College;
import org.springframework.data.neo4j.integration.issues.gh2576.CollegeRepository;
import org.springframework.data.neo4j.integration.issues.gh2576.Student;
import org.springframework.data.neo4j.integration.issues.gh2579.ColumnNode;
import org.springframework.data.neo4j.integration.issues.gh2579.TableAndColumnRelation;
import org.springframework.data.neo4j.integration.issues.gh2579.TableNode;
import org.springframework.data.neo4j.integration.issues.gh2579.TableRepository;
import org.springframework.data.neo4j.integration.issues.gh2583.GH2583Node;
import org.springframework.data.neo4j.integration.issues.gh2583.GH2583Repository;
import org.springframework.data.neo4j.integration.issues.gh2622.GH2622Repository;
import org.springframework.data.neo4j.integration.issues.gh2622.MePointingTowardsMe;
import org.springframework.data.neo4j.integration.issues.gh2639.Company;
import org.springframework.data.neo4j.integration.issues.gh2639.CompanyPerson;
import org.springframework.data.neo4j.integration.issues.gh2639.CompanyRepository;
import org.springframework.data.neo4j.integration.issues.gh2639.Developer;
import org.springframework.data.neo4j.integration.issues.gh2639.Enterprise;
import org.springframework.data.neo4j.integration.issues.gh2639.Individual;
import org.springframework.data.neo4j.integration.issues.gh2639.LanguageRelationship;
import org.springframework.data.neo4j.integration.issues.gh2639.ProgrammingLanguage;
import org.springframework.data.neo4j.integration.issues.gh2639.Sales;
import org.springframework.data.neo4j.integration.issues.gh2819.GH2819Model;
import org.springframework.data.neo4j.integration.issues.gh2819.GH2819Repository;
import org.springframework.data.neo4j.integration.issues.gh2886.Apple;
import org.springframework.data.neo4j.integration.issues.gh2886.FruitRepository;
import org.springframework.data.neo4j.integration.issues.gh2886.Orange;
import org.springframework.data.neo4j.integration.issues.qbe.A;
import org.springframework.data.neo4j.integration.issues.qbe.ARepository;
import org.springframework.data.neo4j.integration.issues.qbe.B;
import org.springframework.data.neo4j.integration.issues.gh2727.FirstLevelEntity;
import org.springframework.data.neo4j.integration.issues.gh2727.FirstLevelEntityRepository;
import org.springframework.data.neo4j.integration.issues.gh2727.FirstLevelProjection;
import org.springframework.data.neo4j.integration.issues.gh2727.SecondLevelEntity;
import org.springframework.data.neo4j.integration.issues.gh2727.SecondLevelEntityRelationship;
import org.springframework.data.neo4j.integration.issues.gh2727.ThirdLevelEntity;
import org.springframework.data.neo4j.integration.issues.gh2727.ThirdLevelEntityRelationship;
import org.springframework.data.neo4j.integration.misc.ConcreteImplementationTwo;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.repository.query.QueryFragmentsAndParameters;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jImperativeTestConfiguration;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 * @soundtrack Sodom - Sodom
 */
@Neo4jIntegrationTest
@DisplayNameGeneration(SimpleDisplayNameGeneratorWithTags.class)
@TestMethodOrder(MethodOrderer.DisplayName.class)
class IssuesIT extends TestBase {

	// GH-2210
	private static final Long numberA = 1L;
	private static final Long numberB = 2L;
	private static final Long numberC = 3L;
	private static final Long numberD = 4L;

	// GH-2323
	protected static String personId;

	@BeforeAll
	protected static void setupData(@Autowired BookmarkCapture bookmarkCapture) {
		try (Session session = neo4jConnectionSupport.getDriver().session(bookmarkCapture.createSessionConfig())) {
			if (neo4jConnectionSupport.isCypher5SyntaxCompatible()) {
				session.run("CREATE CONSTRAINT TNC IF NOT EXISTS FOR (tn:TestNode) REQUIRE tn.name IS UNIQUE")
						.consume();
			} else {
				session.run("CREATE CONSTRAINT TNC IF NOT EXISTS ON (tn:TestNode) ASSERT tn.name IS UNIQUE").consume();
			}
			try (Transaction transaction = session.beginTransaction()) {
				transaction.run("MATCH (n) detach delete n");

				setupGH2168(transaction);
				setupGH2210(transaction);
				setupGH2289(transaction);
				setupGH2323(transaction);
				setupGH2328(transaction);
				setupGH2459(transaction);
				setupGH2572(transaction);
				setupGH2583(transaction);

				transaction.run("CREATE (:A {name: 'A name', id: randomUUID()}) -[:HAS] ->(:B {anotherName: 'Whatever', id: randomUUID()})");

				transaction.commit();
			}
			bookmarkCapture.seedWith(session.lastBookmarks());
		}
	}

	private static void setupGH2168(QueryRunner queryRunner) {
		queryRunner.run("CREATE (:DomainObject{id: 'A'})").consume();
	}

	private static void setupGH2210(QueryRunner queryRunner) {
		queryRunner.run("""
						create (a:SomeEntity {number: $numberA, name: "A"})
						create (b:SomeEntity {number: $numberB, name: "B"})
						create (c:SomeEntity {number: $numberC, name: "C"})
						create (d:SomeEntity {number: $numberD, name: "D"})
						create (a) -[:SOME_RELATION_TO {someData: "d1"}] -> (b)
						create (b) <-[:SOME_RELATION_TO {someData: "d2"}] - (c)
						create (c) <-[:SOME_RELATION_TO {someData: "d3"}] - (d)
						return *""",
				Map.of("numberA", numberA,
						"numberB", numberB,
						"numberC", numberC,
						"numberD", numberD)).consume();
	}

	private static void setupGH2323(QueryRunner queryRunner) {
		queryRunner.run("unwind ['German', 'English'] as name create (n:Language {name: name}) return name")
				.consume();
		personId = queryRunner.run("""
						MATCH (l:Language {name: 'German'})
						CREATE (n:Person {id: randomUUID(), name: 'Helge'}) -[:HAS_MOTHER_TONGUE]-> (l)
						return n.id"""
				).single()
				.get(0)
				.asString();
	}

	private static void setupGH2459(QueryRunner queryRunner) {
		queryRunner.run("""
				CREATE(po1:Boy:PetOwner {name: 'Boy1', uuid: '10'})
				CREATE(a1:Dog:Animal {name: 'Dog1',uuid: '11'})
				CREATE (po1)-[:hasPet]->(a1)
				CREATE(a3:Cat:Animal {name: 'Cat1',uuid: '12'})
				CREATE (po1)-[:hasPet]->(a3)""").consume();
	}

	private static void setupGH2583(QueryRunner queryRunner) {
		queryRunner.run("""
				CREATE (n:GH2583Node)-[:LINKED]->(m:GH2583Node)-[:LINKED]->(n)-[:LINKED]->(m)
				-[:LINKED]->(n)-[:LINKED]->(m)-[:LINKED]->(n)-[:LINKED]->(m)
				-[:LINKED]->(n)-[:LINKED]->(m)-[:LINKED]->(n)-[:LINKED]->(m)
				-[:LINKED]->(n)-[:LINKED]->(m)-[:LINKED]->(n)-[:LINKED]->(m)
				-[:LINKED]->(n)-[:LINKED]->(m)-[:LINKED]->(n)-[:LINKED]->(m)
				-[:LINKED]->(n)-[:LINKED]->(m)-[:LINKED]->(n)-[:LINKED]->(m)
				-[:LINKED]->(n)-[:LINKED]->(m)-[:LINKED]->(n)-[:LINKED]->(m)
				-[:LINKED]->(n)-[:LINKED]->(m)-[:LINKED]->(n)-[:LINKED]->(m)
				-[:LINKED]->(n)-[:LINKED]->(m)-[:LINKED]->(n)-[:LINKED]->(m)
				-[:LINKED]->(n)-[:LINKED]->(m)-[:LINKED]->(n)-[:LINKED]->(m)
				-[:LINKED]->(n)-[:LINKED]->(m)-[:LINKED]->(n)-[:LINKED]->(m)""").consume();
	}

	@BeforeEach
	protected void prepareIndividual(
			@Autowired CityModelRepository cityModelRepository
	) {
		CityModel aachen = new CityModel();
		aachen.setName("Aachen");
		aachen.setExoticProperty("Cars");

		CityModel utrecht = new CityModel();
		utrecht.setName("Utrecht");
		utrecht.setExoticProperty("Bikes");

		cityModelRepository.saveAll(List.of(aachen, utrecht));
	}

	@Test
	void qbeWithRelationship(@Autowired ARepository repository) {

		var probe = new A();
		probe.setB(new B("Whatever"));
		var optionalResult = repository.findOne(Example.of(probe));
		assertThat(optionalResult).hasValueSatisfying(a -> {
			assertThat(a.getName()).isEqualTo("A name");
			assertThat(a.getId()).isNotNull();
		});

		var allResults = repository.findAll(Example.of(probe));
		assertThat(allResults).hasSize(1);
	}

	@Test
	@Tag("GH-2168")
	void findByIdShouldWork(@Autowired DomainObjectRepository domainObjectRepository) {

		Optional<DomainObject> optionalResult = domainObjectRepository.findById("A");
		assertThat(optionalResult)
				.map(DomainObject::getId)
				.hasValue("A");
	}

	@Test
	@Tag("GH-2415")
	void saveWithProjectionImplementedByEntity(@Autowired Neo4jMappingContext mappingContext,
			@Autowired Neo4jTemplate neo4jTemplate) {

		Neo4jPersistentEntity<?> metaData = mappingContext.getPersistentEntity(BaseNodeEntity.class);
		NodeEntity nodeEntity = neo4jTemplate
				.find(BaseNodeEntity.class)
				.as(NodeEntity.class)
				.matching(QueryFragmentsAndParameters.forCondition(metaData,
						Constants.NAME_OF_TYPED_ROOT_NODE.apply(metaData).property("nodeId")
								.isEqualTo(Cypher.literalOf("root"))))
				.one().get();
		neo4jTemplate.saveAs(nodeEntity, NodeWithDefinedCredentials.class);

		nodeEntity = neo4jTemplate.findById(nodeEntity.getNodeId(), NodeEntity.class).get();
		assertThat(nodeEntity.getChildren()).hasSize(1);
	}

	@Test
	@Tag("GH-2168")
	void compositePropertyCustomConverterDefaultPrefixShouldWork(
			@Autowired DomainObjectRepository repository,
			@Autowired Driver driver,
			@Autowired BookmarkCapture bookmarkCapture
	) {

		DomainObject domainObject = new DomainObject();
		domainObject.setStoredAsMultipleProperties(new UnrelatedObject(true, 4711L));
		domainObject = repository.save(domainObject);

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			var node = session
					.run("MATCH (n:DomainObject {id: $id}) RETURN n",
							Collections.singletonMap("id", domainObject.getId()))
					.single().get(0).asNode();
			assertThat(node.get("storedAsMultipleProperties.aBooleanValue").asBoolean()).isTrue();
			assertThat(node.get("storedAsMultipleProperties.aLongValue").asLong()).isEqualTo(4711L);
		}

		domainObject = repository.findById(domainObject.getId()).get();
		assertThat(domainObject.getStoredAsMultipleProperties())
				.satisfies(t -> {
					assertThat(t.isABooleanValue()).isTrue();
					assertThat(t.getALongValue()).isEqualTo(4711L);
				});
	}

	// That test and the underlying mapping cause the original issue to fail, as `@ConvertWith` was missing for non-simple
	// types in the lookup that checked whether something is an association or not
	@Test
	@Tag("GH-2168")
	void propertyCustomConverterDefaultPrefixShouldWork(
			@Autowired Neo4jMappingContext ctx,
			@Autowired DomainObjectRepository repository,
			@Autowired Driver driver,
			@Autowired BookmarkCapture bookmarkCapture
	) {
		Neo4jPersistentEntity<?> entity = ctx.getRequiredPersistentEntity(DomainObject.class);
		assertWriteAndReadConversionForProperty(entity, "storedAsSingleProperty", repository, driver, bookmarkCapture);
	}

	@Test
	@Tag("GH-2430")
	void propertyConversionsWithBeansShouldWork(
			@Autowired Neo4jMappingContext ctx,
			@Autowired DomainObjectRepository repository,
			@Autowired Driver driver,
			@Autowired BookmarkCapture bookmarkCapture
	) {
		Neo4jPersistentEntity<?> entity = ctx.getRequiredPersistentEntity(DomainObject.class);
		assertWriteAndReadConversionForProperty(entity, "storedAsAnotherSingleProperty", repository, driver,
				bookmarkCapture);
	}

	@Test
	@Tag("GH-2210")
	void standardFinderShouldWork(@Autowired Neo4jTemplate template) {

		assertA(template.findById(numberA, SomeEntity.class));

		assertB(template.findById(numberB, SomeEntity.class));

		assertD(template.findById(numberD, SomeEntity.class));
	}

	@Test
	@Tag("GH-2210")
	void pathsBasedQueryShouldWork(@Autowired Neo4jTemplate template) {

		String query = "MATCH p = (leaf:SomeEntity {number: $a})-[:SOME_RELATION_TO*]-(:SomeEntity) RETURN leaf, collect(nodes(p)), collect(relationships(p))";
		assertA(template.findOne(query, Collections.singletonMap("a", numberA), SomeEntity.class));

		assertB(template.findOne(query, Collections.singletonMap("a", numberB), SomeEntity.class));

		assertD(template.findOne(query, Collections.singletonMap("a", numberD), SomeEntity.class));
	}

	@Test
	@Tag("GH-2210")
	void aPathReturnedShouldPopulateAllNodes(@Autowired Neo4jTemplate template) {

		String query = "MATCH p = (leaf:SomeEntity {number: $a})-[:SOME_RELATION_TO*]-(:SomeEntity) RETURN p";
		assertAll(template.findAll(query, Collections.singletonMap("a", numberA), SomeEntity.class));
	}

	@Test
	@Tag("GH-2210")
	void standardFindAllShouldWork(@Autowired Neo4jTemplate template) {

		assertAll(template.findAll(SomeEntity.class));
	}

	@Test
	@Tag("GH-2244")
	void safeAllWithSubTypesShouldWork(@Autowired Neo4jTemplate neo4jTemplate) {

		List<Step> steps = Arrays.asList(new Step.Origin(), new Step.Chain(), new Step.End());
		steps = neo4jTemplate.saveAll(steps);
		assertThat(steps).allSatisfy(s -> assertThat(s.getId()).isNotNull());
	}

	@RepeatedTest(23)
	@Tag("GH-2289")
	void testNewRelation(@Autowired SkuRepository skuRepo) {
		Sku a = skuRepo.save(new Sku(0L, "A"));
		Sku b = skuRepo.save(new Sku(1L, "B"));
		Sku c = skuRepo.save(new Sku(2L, "C"));
		Sku d = skuRepo.save(new Sku(3L, "D"));

		a.rangeRelationTo(b, 1, 1, RelationType.MULTIPLICATIVE);
		a.rangeRelationTo(c, 1, 1, RelationType.MULTIPLICATIVE);
		a.rangeRelationTo(d, 1, 1, RelationType.MULTIPLICATIVE);
		a = skuRepo.save(a);

		assertThat(a.getRangeRelationsOut()).hasSize(3);
		b = skuRepo.findById(b.getId()).get();
		assertThat(b.getRangeRelationsIn()).hasSize(1);

		assertThat(b.getRangeRelationsIn().stream().findFirst().get().getTargetSku().getRangeRelationsOut()).hasSize(3);

		b.rangeRelationTo(c, 1, 1, RelationType.MULTIPLICATIVE);
		b = skuRepo.save(b);
		assertThat(b.getRangeRelationsIn()).hasSize(1);
		assertThat(b.getRangeRelationsOut()).hasSize(1);

		a = skuRepo.findById(a.getId()).get();
		assertThat(a.getRangeRelationsOut()).hasSize(3);
		assertThat(a.getRangeRelationsOut()).allSatisfy(r -> {
			int expectedSize = 1;
			if ("C".equals(r.getTargetSku().getName())) {
				expectedSize = 2;
			}
			assertThat(r.getTargetSku().getRangeRelationsIn()).hasSize(expectedSize);
		});
	}

	@RepeatedTest(5)
	@Tag("GH-2289")
	@Tag("GH-2294")
	void testNewRelationRo(@Autowired SkuRORepository skuRepo) {
		SkuRO a = skuRepo.findOneByName("A");
		SkuRO b = skuRepo.findOneByName("B");
		SkuRO c = skuRepo.findOneByName("C");
		SkuRO d = skuRepo.findOneByName("D");

		a.rangeRelationTo(b, 1, 1, RelationType.MULTIPLICATIVE);
		a.rangeRelationTo(c, 1, 1, RelationType.MULTIPLICATIVE);
		a.rangeRelationTo(d, 1, 1, RelationType.MULTIPLICATIVE);
		a.setName("a new name");
		a = skuRepo.save(a);
		assertThat(a.getRangeRelationsOut()).hasSize(3);
		assertThat(a.getName()).isEqualTo("a new name");

		assertThat(skuRepo.findOneByName("a new name")).isNull();

		b = skuRepo.findOneByName("B");
		assertThat(b.getRangeRelationsIn()).hasSize(1);
		assertThat(b.getRangeRelationsOut()).hasSizeLessThanOrEqualTo(1);

		b.rangeRelationTo(c, 1, 1, RelationType.MULTIPLICATIVE);
		b = skuRepo.save(b);
		assertThat(b.getRangeRelationsIn()).hasSize(1);
		assertThat(b.getRangeRelationsOut()).hasSize(1);
	}

	@Test
	@Tag("GH-2323")
	void listOfRelationshipPropertiesShouldBeUnwindable(@Autowired PersonService personService) {
		Person person = personService.updateRel(personId, List.of("German"));
		assertThat(person).isNotNull();
		assertThat(person.getKnownLanguages()).hasSize(1);
		assertThat(person.getKnownLanguages()).first().satisfies(knows -> {
			assertThat(knows.getDescription()).isEqualTo("Some description");
			assertThat(knows.getLanguage()).extracting(Language::getName).isEqualTo("German");
		});
	}

	@Test
	@Tag("GH-2537")
	void ensureRelationshipsAreSerialized(@Autowired PersonService personService) {

		Optional<Person> optionalPerson = personService.updateRel2(personId, List.of("German"));
		assertThat(optionalPerson).isPresent().hasValueSatisfying(person -> {
			assertThat(person.getKnownLanguages()).hasSize(1);
			assertThat(person.getKnownLanguages()).first().satisfies(knows -> {
				assertThat(knows.getDescription()).isEqualTo("Some description");
				assertThat(knows.getLanguage()).extracting(Language::getName).isEqualTo("German");
			});
		});
	}

	@Test
	@Tag("GH-2537")
	void ensure1To1RelationshipsAreSerialized(@Autowired PersonService personService) {

		Optional<Person> optionalPerson = personService.updateRel3(personId);
		assertThat(optionalPerson).isPresent().hasValueSatisfying(person -> {

			assertThat(person.getKnownLanguages()).hasSize(1);
			assertThat(person.getKnownLanguages()).first().satisfies(knows -> {
				assertThat(knows.getDescription()).isEqualTo("Whatever");
				assertThat(knows.getLanguage()).extracting(Language::getName).isEqualTo("German");
			});
		});
	}

	@Test
	@Tag("GH-2791")
	void ensureMapOfRelationshipGetsSerialized(@Autowired PersonService personService) {
		Knows knowsRelationship = new Knows("somedescription", new Language("German"));

		Person person = personService.queryWithMapOfRelationship("someKey", knowsRelationship);

		assertThat(person.getName()).isEqualTo("Helge");
	}

	@Test
	@Tag("GH-2326")
	void saveShouldAddAllLabels(@Autowired AnimalRepository animalRepository,
			@Autowired BookmarkCapture bookmarkCapture) {

		List<AbstractLevel2> animals = Arrays.asList(new AbstractLevel2.AbstractLevel3.Concrete1(),
				new AbstractLevel2.AbstractLevel3.Concrete2());
		List<String> ids = animals.stream().map(animalRepository::save).map(BaseEntity::getId)
				.collect(Collectors.toList());

		assertLabels(bookmarkCapture, ids);
	}

	@Test
	@Tag("GH-2326")
	void saveAllShouldAddAllLabels(@Autowired AnimalRepository animalRepository,
			@Autowired BookmarkCapture bookmarkCapture) {

		List<AbstractLevel2> animals = Arrays.asList(new AbstractLevel2.AbstractLevel3.Concrete1(),
				new AbstractLevel2.AbstractLevel3.Concrete2());
		List<String> ids = animalRepository.saveAll(animals).stream().map(BaseEntity::getId)
				.collect(Collectors.toList());

		assertLabels(bookmarkCapture, ids);
	}

	@Test
	@Tag("GH-2328")
	void queriesFromCustomLocationsShouldBeFound(@Autowired Entity2328Repository someRepository) {

		assertThat(someRepository.getSomeEntityViaNamedQuery()).satisfies(IssuesIT::requirements);
	}

	@Test
	@Tag("GH-2347")
	void entitiesWithAssignedIdsSavedInBatchMustBeIdentifiableWithTheirInternalIds(
			@Autowired ApplicationRepository applicationRepository,
			@Autowired Driver driver,
			@Autowired BookmarkCapture bookmarkCapture
	) {
		List<Application> savedApplications = applicationRepository.saveAll(Collections.singletonList(createData()));

		assertThat(savedApplications).hasSize(1);
		assertSingleApplicationNodeWithMultipleWorkflows(driver, bookmarkCapture);
	}

	@Test
	@Tag("GH-2347")
	void entitiesWithAssignedIdsMustBeIdentifiableWithTheirInternalIds(
			@Autowired ApplicationRepository applicationRepository,
			@Autowired Driver driver,
			@Autowired BookmarkCapture bookmarkCapture
	) {
		applicationRepository.save(createData());
		assertSingleApplicationNodeWithMultipleWorkflows(driver, bookmarkCapture);
	}

	@Test
	@Tag("GH-2346")
	void relationshipsStartingAtEntitiesWithAssignedIdsShouldBeCreated(
			@Autowired ApplicationRepository applicationRepository,
			@Autowired Driver driver,
			@Autowired BookmarkCapture bookmarkCapture
	) {
		createData((applications, workflows) -> {
			List<Application> savedApplications = applicationRepository.saveAll(applications);

			assertThat(savedApplications).hasSize(2);
			assertMultipleApplicationsNodeWithASingleWorkflow(driver, bookmarkCapture);
		});
	}

	@Test
	@Tag("GH-2346")
	void relationshipsStartingAtEntitiesWithAssignedIdsShouldBeCreatedOtherDirection(
			@Autowired WorkflowRepository workflowRepository,
			@Autowired Driver driver,
			@Autowired BookmarkCapture bookmarkCapture
	) {
		createData((applications, workflows) -> {
			List<Workflow> savedWorkflows = workflowRepository.saveAll(workflows);

			assertThat(savedWorkflows).hasSize(2);
			assertMultipleApplicationsNodeWithASingleWorkflow(driver, bookmarkCapture);
		});
	}

	@Test
	@Tag("GH-2459")
	void dontOverrideAbstractMappedData(@Autowired PetOwnerRepository repository) {
		Optional<PetOwner> optionalPetOwner = repository.findById("10");
		assertThat(optionalPetOwner).isPresent()
				.hasValueSatisfying(petOwner ->
						assertThat(petOwner.getPets()).hasSize(2));
	}

	@Test
	@Tag("GH-2474")
	public void testStoreExoticProperty(@Autowired CityModelRepository cityModelRepository) {

		CityModel cityModel = new CityModel();
		cityModel.setName("The Jungle");
		cityModel.setExoticProperty("lions");
		cityModel = cityModelRepository.save(cityModel);

		CityModel reloaded = cityModelRepository.findById(cityModel.getCityId())
				.orElseThrow(RuntimeException::new);
		assertThat(reloaded.getExoticProperty()).isEqualTo("lions");

		long cnt = cityModelRepository.deleteAllByExoticProperty("lions");
		assertThat(cnt).isOne();
	}

	@Test
	@Tag("GH-2474")
	public void testSortOnExoticProperty(@Autowired CityModelRepository cityModelRepository) {

		Sort sort = Sort.by(Sort.Order.asc("exoticProperty"));
		List<CityModel> cityModels = cityModelRepository.findAll(sort);

		assertThat(cityModels).extracting(CityModel::getExoticProperty).containsExactly("Bikes", "Cars");
	}

	@Test
	@Tag("GH-2474")
	public void testSortOnExoticPropertyCustomQuery_MakeSureIUnderstand(
			@Autowired CityModelRepository cityModelRepository) {

		Sort sort = Sort.by(Sort.Order.asc("n.name"));
		List<CityModel> cityModels = cityModelRepository.customQuery(sort);

		assertThat(cityModels).extracting(CityModel::getExoticProperty).containsExactly("Cars", "Bikes");
	}

	@Test
	@Tag("GH-2474")
	public void testSortOnExoticPropertyCustomQuery(@Autowired CityModelRepository cityModelRepository) {
		Sort sort = Sort.by(Sort.Order.asc("n.`exotic.property`"));
		List<CityModel> cityModels = cityModelRepository.customQuery(sort);

		assertThat(cityModels).extracting(CityModel::getExoticProperty).containsExactly("Bikes", "Cars");
	}

	@Test
	@Tag("GH-2475")
	public void testCityModelProjectionPersistence(
			@Autowired CityModelRepository cityModelRepository,
			@Autowired PersonModelRepository personModelRepository,
			@Autowired Neo4jTemplate neo4jTemplate
	) {
		CityModel cityModel = new CityModel();
		cityModel.setName("New Cool City");
		cityModel = cityModelRepository.save(cityModel);

		PersonModel personModel = new PersonModel();
		personModel.setName("Mr. Mayor");
		personModel.setAddress("1600 City Avenue");
		personModel.setFavoriteFood("tacos");
		personModelRepository.save(personModel);

		CityModelDTO cityModelDTO = cityModelRepository.findByCityId(cityModel.getCityId())
				.orElseThrow(RuntimeException::new);
		cityModelDTO.setName("Changed name");
		cityModelDTO.setExoticProperty("tigers");

		CityModelDTO.PersonModelDTO personModelDTO = new CityModelDTO.PersonModelDTO();
		personModelDTO.setPersonId(personModelDTO.getPersonId());

		CityModelDTO.JobRelationshipDTO jobRelationshipDTO = new CityModelDTO.JobRelationshipDTO();
		jobRelationshipDTO.setPerson(personModelDTO);

		cityModelDTO.setMayor(personModelDTO);
		cityModelDTO.setCitizens(Collections.singletonList(personModelDTO));
		cityModelDTO.setCityEmployees(Collections.singletonList(jobRelationshipDTO));
		neo4jTemplate.save(CityModel.class).one(cityModelDTO);

		CityModel reloaded = cityModelRepository.findById(cityModel.getCityId())
				.orElseThrow(RuntimeException::new);
		assertThat(reloaded.getName()).isEqualTo("Changed name");
		assertThat(reloaded.getMayor()).isNotNull();
		assertThat(reloaded.getCitizens()).hasSize(1);
		assertThat(reloaded.getCityEmployees()).hasSize(1);
	}

	@Test
	@Tag("GH-2493")
	void saveOneShouldWork(@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture,
			@Autowired TestObjectRepository repository) {

		TestObject testObject = new TestObject(new TestData(4711, "Foobar"));
		testObject = repository.save(testObject);

		assertThat(testObject.getId()).isNotNull();
		assertThatTestObjectHasBeenCreated(driver, bookmarkCapture, testObject);
	}

	@Test
	@Tag("GH-2493")
	void saveAllShouldWork(@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture,
			@Autowired TestObjectRepository repository) {

		TestObject testObject = new TestObject(new TestData(4711, "Foobar"));
		testObject = repository.saveAll(Collections.singletonList(testObject)).get(0);

		assertThat(testObject.getId()).isNotNull();
		assertThatTestObjectHasBeenCreated(driver, bookmarkCapture, testObject);
	}

	@Test
	@Tag("GH-2498")
	void cypherdslConditionExecutorShouldWorkWithAnonParameters(@Autowired DomainModelRepository repository) {

		Node node = Cypher.node("DomainModel").named("domainModel");
		Property name = node.property("name");
		Parameter<List<String>> parameters = Cypher.anonParameter(List.of("A", "C"));
		Condition in = name.in(parameters);
		Collection<DomainModel> result = repository.findAll(in, Cypher.sort(name).descending());
		assertThat(result).hasSize(2)
				.map(DomainModel::getName)
				.containsExactly("C", "A");
	}

	@Test
	@Tag("GH-2498")
	void cypherdslConditionExecutorMustApplyParametersToNestedStatementsToo(@Autowired VertexRepository repository) {
		Node personNode = Cypher.node("Vertex").named("vertex");
		Property name = personNode.property("name");
		Parameter<List<String>> param = Cypher.anonParameter(List.of("a", "b"));
		Condition in = name.in(param);
		Collection<Vertex> people = repository.findAll(in);
		assertThat(people)
				.extracting(Vertex::getName)
				.containsExactlyInAnyOrder("a", "b");
	}

	@Test
	@Tag("GH-2500")
	void shouldNotDeleteFreshlyCreatedRelationships(@Autowired Driver driver, @Autowired Neo4jTemplate template) {

		Group group = new Group();
		group.setName("test");
		group.getDevices().add(template.findById(1L, Device.class).get());

		template.save(group);

		try (Session session = driver.session()) {
			long cnt = session.run(
							"MATCH (g:Group {name: $name}) <-[:BELONGS_TO]- (d:Device {id: $deviceId}) RETURN count(*)",
							Map.of("name", group.getName(), "deviceId", 1L))
					.single().get(0).asLong();
			assertThat(cnt).isOne();
		}
	}

	@Test
	@Tag("GH-2526")
	void relationshipWillGetFoundInResultOfMultilevelInheritance(@Autowired BaseNodeRepository repository) {
		MeasurementProjection m = repository.findByNodeId("acc1", MeasurementProjection.class);
		assertThat(m).isNotNull();
		assertThat(m.getDataPoints()).isNotEmpty();
		assertThat(m).extracting(MeasurementProjection::getDataPoints,
						InstanceOfAssertFactories.collection(DataPoint.class))
				.extracting(DataPoint::isManual, DataPoint::getMeasurand).contains(tuple(true, new Measurand("o1")));
	}

	@Test
	@Tag("GH-2530")
	void shouldPutLazyFoundEntityIntoHierarchy(@Autowired SomethingInBetweenRepository repository) {
		InitialEntities.ConcreteImplementationOne cc1 = new InitialEntities.ConcreteImplementationOne();
		cc1.name = "CC1";
		repository.save(cc1);

		InitialEntities.SpecialKind foundCC1 = (InitialEntities.SpecialKind) repository.findById(cc1.id).get();
		SoftAssertions softly = new SoftAssertions();
		softly.assertThat(foundCC1).as("type").isInstanceOf(InitialEntities.ConcreteImplementationOne.class);
		softly.assertThat(((InitialEntities.ConcreteImplementationOne) foundCC1).name).as("CC1").isNotEmpty();
		softly.assertAll();

		ConcreteImplementationTwo cat = new ConcreteImplementationTwo();
		repository.save(cat);

		InitialEntities.SpecialKind foundCC2 = (InitialEntities.SpecialKind) repository.findById(cat.id).get();
		assertThat(foundCC2).as("type").isInstanceOf(ConcreteImplementationTwo.class);
	}

	@Test
	@Tag("GH-2533")
	void projectionWorksForDynamicRelationshipsOnSave(@Autowired GH2533Repository repository,
			@Autowired Neo4jTemplate neo4jTemplate) {
		EntitiesAndProjections.GH2533Entity rootEntity = createData(repository);

		rootEntity = repository.findByIdWithLevelOneLinks(rootEntity.id).get();

		// this had caused the rootEntity -> child -X-> child relationship to get removed (X).
		neo4jTemplate.saveAs(rootEntity, EntitiesAndProjections.GH2533EntityNodeWithOneLevelLinks.class);

		EntitiesAndProjections.GH2533Entity entity = neo4jTemplate.findById(rootEntity.id,
				EntitiesAndProjections.GH2533Entity.class).get();

		assertThat(entity.relationships).isNotEmpty();
		assertThat(entity.relationships.get("has_relationship_with")).isNotEmpty();
		assertThat(entity.relationships.get("has_relationship_with").get(0).target).isNotNull();
		assertThat(entity.relationships.get("has_relationship_with").get(0).target.relationships).isNotEmpty();
	}

	@Test
	@Tag("GH-2533")
	void saveRelatedEntityWithRelationships(@Autowired GH2533Repository repository,
			@Autowired Neo4jTemplate neo4jTemplate) {
		EntitiesAndProjections.GH2533Entity rootEntity = createData(repository);

		neo4jTemplate.saveAs(rootEntity, EntitiesAndProjections.GH2533EntityWithRelationshipToEntity.class);

		EntitiesAndProjections.GH2533Entity entity = neo4jTemplate.findById(rootEntity.id,
				EntitiesAndProjections.GH2533Entity.class).get();

		assertThat(entity.relationships.get("has_relationship_with").get(0).target.name).isEqualTo("n2");
		assertThat(entity.relationships.get("has_relationship_with").get(0).target.relationships.get(
				"has_relationship_with").get(0).target.name).isEqualTo("n3");
	}

	@Test
	@Tag("GH-2542")
	void shouldThrowDataIntegrityViolationException(@Autowired TestNodeRepository repository) {

		repository.save(new TestNode("Bob"));
		var secondNode = new TestNode("Bob");
		assertThatExceptionOfType(DataIntegrityViolationException.class)
				.isThrownBy(() -> repository.save(secondNode));
	}

	@Test
	@Tag("GH-2572")
	void allShouldFetchCorrectNumberOfChildNodes(@Autowired GH2572Repository GH2572Repository) {
		List<GH2572Child> dogsForPerson = GH2572Repository.getDogsForPerson("GH2572Parent-2");
		assertThat(dogsForPerson).hasSize(2);
	}

	@Test
	@Tag("GH-2572")
	void allShouldNotFailWithoutMatchingRootNodes(@Autowired GH2572Repository GH2572Repository) {
		List<GH2572Child> dogsForPerson = GH2572Repository.getDogsForPerson("GH2572Parent-1");
		assertThat(dogsForPerson).isEmpty();
	}

	@Test
	@Tag("GH-2572")
	void oneShouldFetchCorrectNumberOfChildNodes(@Autowired GH2572Repository GH2572Repository) {
		Optional<GH2572Child> optionalChild = GH2572Repository.findOneDogForPerson("GH2572Parent-2");
		assertThat(optionalChild).map(GH2572Child::getName).hasValue("a-pet");
	}

	@Test
	@Tag("GH-2572")
	void oneShouldNotFailWithoutMatchingRootNodes(@Autowired GH2572Repository GH2572Repository) {
		Optional<GH2572Child> optionalChild = GH2572Repository.findOneDogForPerson("GH2572Parent-1");
		assertThat(optionalChild).isEmpty();
	}

	@Test
	@Tag("GH-2572")
	void getOneShouldFetchCorrectNumberOfChildNodes(@Autowired GH2572Repository GH2572Repository) {
		GH2572Child gh2572Child = GH2572Repository.getOneDogForPerson("GH2572Parent-2");
		assertThat(gh2572Child.getName()).isEqualTo("a-pet");
	}

	@Test
	@Tag("GH-2572")
	void getOneShouldNotFailWithoutMatchingRootNodes(@Autowired GH2572Repository GH2572Repository) {
		GH2572Child gh2572Child = GH2572Repository.getOneDogForPerson("GH2572Parent-1");
		assertThat(gh2572Child).isNull();
	}

	@Test
	@Tag("GH-2576")
	void listOfMapsShouldBeUsableAsArguments(@Autowired Neo4jTemplate template,  @Autowired CollegeRepository collegeRepository) {

		var student = template.save(new Student("S1"));
		var college = template.save(new College("C1"));

		var pair = Map.of("stuGuid", student.getGuid(), "collegeGuid", college.getGuid());
		var listOfPairs = List.of(pair);

		var uuids = collegeRepository.addStudentToCollege(listOfPairs);
		assertThat(uuids).containsExactly(student.getGuid());
	}

	@Test
	@Tag("GH-2576")
	void listOfMapsShouldBeUsableAsArgumentsWithWorkaround(@Autowired Neo4jTemplate template,  @Autowired CollegeRepository collegeRepository) {

		var student = template.save(new Student("S1"));
		var college = template.save(new College("C1"));

		var pair = Map.of("stuGuid", student.getGuid(), "collegeGuid", college.getGuid());
		var listOfPairs = List.of(Values.value(pair));

		var uuids = collegeRepository.addStudentToCollegeWorkaround(listOfPairs);
		assertThat(uuids).containsExactly(student.getGuid());
	}

	@Test
	@Tag("GH-2579")
	void unwindWithMergeShouldWork(@Autowired Neo4jTemplate template, @Autowired TableRepository tableRepository) {

		TableNode tableNode = new TableNode();
		tableNode.setName("t1");
		tableNode.setSchemaName("a");
		tableNode.setSourceName("source1");
		tableNode = template.save(tableNode);

		ColumnNode c1 = new ColumnNode();
		c1.setName("c1");
		c1.setSchemaName("a");
		c1.setSourceName("source1");
		c1.setTableName(tableNode.getName());
		long c1Id = template.save(c1).getId();

		ColumnNode c2 = new ColumnNode();
		c2.setName("c2");
		c2.setSchemaName("a");
		c2.setSourceName("source2");
		c2.setTableName(tableNode.getName());
		long c2Id = template.save(c2).getId();

		tableRepository.mergeTableAndColumnRelations(List.of(c1, c2), tableNode);

		Optional<TableNode> resolvedTableNode = tableRepository.findById(tableNode.getId());
		assertThat(resolvedTableNode)
				.map(TableNode::getTableAndColumnRelation)
				.hasValueSatisfying(l -> {
					assertThat(l)
							.map(TableAndColumnRelation::getColumnNode)
							.map(ColumnNode::getId)
							.containsExactlyInAnyOrder(c1Id, c2Id);
				});
	}

	@Test
	@Tag("GH-2583")
	void mapStandardCustomQueryWithLotsOfRelationshipsProperly(@Autowired GH2583Repository repository) {
		Page<GH2583Node> nodePage = repository.getNodesByCustomQuery(PageRequest.of(0, 300));

		List<GH2583Node> nodes = nodePage.getContent();
		assertThat(nodes).hasSize(2);
	}

	@Test
	@Tag("GH-2639")
	void relationshipsOfGenericRelationshipsGetResolvedCorrectly(@Autowired CompanyRepository companyRepository) {
		CompanyPerson greg = new Sales("Greg");
		CompanyPerson roy = new Sales("Roy");
		CompanyPerson craig = new Sales("Craig");

		ProgrammingLanguage java = new ProgrammingLanguage("java", "1.5");
		java.inventor = new Enterprise("Sun", ";(");
		ProgrammingLanguage perl = new ProgrammingLanguage("perl", "6.0");
		perl.inventor = new Individual("Larry Wall", "larryW");

		List<LanguageRelationship> languageRelationships = new ArrayList<>();
		LanguageRelationship javaRelationship = new LanguageRelationship(5, java);
		LanguageRelationship perlRelationship = new LanguageRelationship(2, perl);
		languageRelationships.add(javaRelationship);
		languageRelationships.add(perlRelationship);

		Developer harry = new Developer("Harry", languageRelationships);
		List<CompanyPerson> team = Arrays.asList(greg,	roy, craig,	harry);
		Company acme = new Company("ACME", team);
		companyRepository.save(acme);

		Company loadedAcme = companyRepository.findByName("ACME");

		Developer loadedHarry = loadedAcme.getEmployees().stream()
				.filter(e -> e instanceof Developer)
				.map(e -> (Developer) e)
				.filter(developer -> developer.getName().equals("Harry"))
				.findFirst().get();

		List<LanguageRelationship> programmingLanguages = loadedHarry.getProgrammingLanguages();
		assertThat(programmingLanguages)
				.isNotEmpty()
				.extracting("score")
				.containsExactlyInAnyOrder(5, 2);

		assertThat(programmingLanguages)
				.extracting("language")
				.extracting("inventor")
				.containsExactlyInAnyOrder(
						new Individual("Larry Wall", "larryW"), new Enterprise("Sun", ";(")
				);
	}

	@Test
	@Tag("GH-2622")
	void throwCyclicMappingDependencyExceptionOnSelfReference(@Autowired GH2622Repository repository) {
		MePointingTowardsMe entity = new MePointingTowardsMe("me", new ArrayList<>());
		entity.others.add(entity);
		repository.save(entity);

		assertThatExceptionOfType(MappingException.class).isThrownBy(repository::findAll)
				.withRootCauseInstanceOf(MappingException.class)
				.extracting(Throwable::getCause, as(InstanceOfAssertFactories.THROWABLE))
				.hasMessageContaining("has a logical cyclic mapping dependency");

	}

	@Test
	@Tag("GH-2727")
	void mapsProjectionChainWithRelationshipProperties(@Autowired FirstLevelEntityRepository firstLevelEntityRepository) {

		String secondLevelValue = "someSecondLevelValue";
		String thirdLevelValue = "someThirdLevelValue";

		final List<SecondLevelEntityRelationship> secondLevelEntityRelationships = new ArrayList<>();
		for (int i = 0; i < 2; i++) {
			final List<ThirdLevelEntityRelationship> thirdLevelEntityRelationships = new ArrayList<>();
			for (int j = 0; j < 3; j++) {
				final ThirdLevelEntity thirdLevelEntity = new ThirdLevelEntity();
				thirdLevelEntity.setSomeValue(thirdLevelValue);
				final ThirdLevelEntityRelationship thirdLevelRelationship = new ThirdLevelEntityRelationship();
				thirdLevelRelationship.setTarget(thirdLevelEntity);
				thirdLevelRelationship.setOrder(j + 1);
				thirdLevelEntityRelationships.add(thirdLevelRelationship);
			}

			final SecondLevelEntity secondLevelEntity = SecondLevelEntity.builder()
					.thirdLevelEntityRelationshipProperties(thirdLevelEntityRelationships)
					.someValue(secondLevelValue)
					.build();

			final SecondLevelEntityRelationship secondLevelRelationship = new SecondLevelEntityRelationship();
			secondLevelRelationship.setTarget(secondLevelEntity);
			secondLevelRelationship.setOrder(i + 1);
			secondLevelEntityRelationships.add(secondLevelRelationship);
		}

		final FirstLevelEntity firstLevelEntity = FirstLevelEntity.builder()
				.secondLevelEntityRelationshipProperties(secondLevelEntityRelationships)
				.name("Test")
				.build();

		firstLevelEntityRepository.save(firstLevelEntity);

		FirstLevelProjection firstLevelProjection = firstLevelEntityRepository.findOneById(firstLevelEntity.getId());
		assertThat(firstLevelProjection).isNotNull();
		assertThat(firstLevelProjection.getSecondLevelEntityRelationshipProperties()).hasSize(2)
				.allSatisfy(secondLevelRelationship -> {
					assertThat(secondLevelRelationship.getTarget().getSomeValue().equals(secondLevelValue));
					assertThat(secondLevelRelationship.getOrder()).isGreaterThan(0);
					assertThat(secondLevelRelationship.getTarget().getThirdLevelEntityRelationshipProperties())
							.isNotEmpty()
							.allSatisfy(thirdLevel -> {
								assertThat(thirdLevel.getOrder()).isGreaterThan(0);
								assertThat(thirdLevel.getTarget()).isNotNull();
								assertThat(thirdLevel.getTarget().getSomeValue()).isEqualTo(thirdLevelValue);
							});
				});
	}

	@Test
	@Tag("GH-2819")
	void inheritanceAndProjectionShouldMapRelatedNodesCorrectly(@Autowired GH2819Repository repository, @Autowired Driver driver) {
		try (var session = driver.session()) {
			session.run("CREATE (a:ParentA:ChildA{name:'parentA', id:'a'})-[:HasBs]->(b:ParentB:ChildB{name:'parentB', id:'b'})-[:HasCs]->(c:ParentC:ChildC{name:'parentC', id:'c'})").consume();
		}

		var childAProjection = repository.findById("a", GH2819Model.ChildAProjection.class);

		assertThat(childAProjection.getName()).isEqualTo("parentA");
		var parentB = childAProjection.getParentB();
		assertThat(parentB).isNotNull();
		assertThat(parentB.getName()).isEqualTo("parentB");
		var parentC = parentB.getParentC();
		assertThat(parentC).isNotNull();
		assertThat(parentC.getName()).isEqualTo("parentC");

	}

	@Test
	@Tag("GH-2886")
	void dynamicLabels(@Autowired FruitRepository repository) {

		var f1 = new Apple();
		f1.setVolume(1.0);
		f1.setColor("Red");
		f1.setLabels(Set.of("X"));

		var f2 = new Apple();
		f2.setColor("Blue");

		var f3 = new Orange();
		f2.setVolume(3.0);
		f3.setColor("Red");
		f3.setLabels(Set.of("Y"));

		var f4 = new Orange();
		f4.setColor("Yellow");

		repository.saveAll(List.of(f1, f2, f3, f4));

		var fruits = repository.findAllFruits();
		assertThat(fruits).allMatch(f -> f instanceof Apple || f instanceof Orange);
	}

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories(namedQueriesLocation = "more-custom-queries.properties")
	@ComponentScan(basePackages = "org.springframework.data.neo4j.integration.issues.gh2323")
	static class Config extends Neo4jImperativeTestConfiguration {

		@Bean
		public Driver driver() {

			return neo4jConnectionSupport.getDriver();
		}

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Bean
		public UnrelatedObjectPropertyConverterAsBean converterBean() {
			return new UnrelatedObjectPropertyConverterAsBean();
		}

		@Override
		public PlatformTransactionManager transactionManager(Driver driver,
				DatabaseSelectionProvider databaseNameProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new Neo4jTransactionManager(driver, databaseNameProvider,
					Neo4jBookmarkManager.create(bookmarkCapture));
		}

		@Override
		public boolean isCypher5Compatible() {
			return neo4jConnectionSupport.isCypher5SyntaxCompatible();
		}
	}

	private static void assertWriteAndReadConversionForProperty(
			Neo4jPersistentEntity<?> entity,
			String propertyName,
			DomainObjectRepository repository,
			Driver driver,
			BookmarkCapture bookmarkCapture
	) {
		Neo4jPersistentProperty property = entity.getPersistentProperty(propertyName);
		PersistentPropertyAccessor<DomainObject> propertyAccessor = entity.getPropertyAccessor(new DomainObject());

		propertyAccessor.setProperty(property, new UnrelatedObject(true, 4711L));
		DomainObject domainObject = repository.save(propertyAccessor.getBean());

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			var node = session
					.run("MATCH (n:DomainObject {id: $id}) RETURN n",
							Collections.singletonMap("id", domainObject.getId()))
					.single().get(0).asNode();
			assertThat(node.get(propertyName).asString()).isEqualTo("true;4711");
		}

		domainObject = repository.findById(domainObject.getId()).get();
		UnrelatedObject unrelatedObject = (UnrelatedObject) entity.getPropertyAccessor(domainObject)
				.getProperty(property);
		assertThat(unrelatedObject)
				.satisfies(t -> {
					assertThat(t.isABooleanValue()).isTrue();
					assertThat(t.getALongValue()).isEqualTo(4711L);
				});
	}

	private static void assertAll(List<SomeEntity> entities) {

		assertThat(entities).hasSize(4);
		assertThat(entities).allSatisfy(v -> {
			switch (v.getName()) {
				case "A" -> assertA(Optional.of(v));
				case "B" -> assertB(Optional.of(v));
				case "D" -> assertD(Optional.of(v));
			}
		});
	}

	private static void assertA(Optional<SomeEntity> a) {

		assertThat(a).hasValueSatisfying(s -> {
			assertThat(s.getName()).isEqualTo("A");
			assertThat(s.getSomeRelationsOut())
					.hasSize(1)
					.first().satisfies(b -> {
						assertThat(b.getSomeData()).isEqualTo("d1");
						assertThat(b.getTargetPerson().getName()).isEqualTo("B");
						assertThat(b.getTargetPerson().getSomeRelationsOut()).isEmpty();
					});
		});
	}

	private static void assertD(Optional<SomeEntity> d) {

		assertThat(d).hasValueSatisfying(s -> {
			assertThat(s.getName()).isEqualTo("D");
			assertThat(s.getSomeRelationsOut())
					.hasSize(1)
					.first().satisfies(c -> {
						assertThat(c.getSomeData()).isEqualTo("d3");
						assertThat(c.getTargetPerson().getName()).isEqualTo("C");
						assertThat(c.getTargetPerson().getSomeRelationsOut())
								.hasSize(1)
								.first().satisfies(b -> {
									assertThat(b.getSomeData()).isEqualTo("d2");
									assertThat(b.getTargetPerson().getName()).isEqualTo("B");
									assertThat(b.getTargetPerson().getSomeRelationsOut()).isEmpty();
								});
					});
		});
	}

	private static void assertB(Optional<SomeEntity> b) {

		assertThat(b).hasValueSatisfying(s -> {
			assertThat(s.getName()).isEqualTo("B");
			assertThat(s.getSomeRelationsOut()).isEmpty();
		});
	}

	private static void assertThatTestObjectHasBeenCreated(Driver driver, BookmarkCapture bookmarkCapture,
			TestObject testObject) {
		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			Map<String, Object> arguments = new HashMap<>();
			arguments.put("id", testObject.getId());
			arguments.put("num", testObject.getData().getNum());
			arguments.put("string", testObject.getData().getString());
			long cnt = session.run(
							"MATCH (n:TestObject) WHERE n.id = $id AND n.dataNum = $num AND n.dataString = $string RETURN count(n)",
							arguments)
					.single().get(0).asLong();
			assertThat(cnt).isOne();
		}
	}

	private static EntitiesAndProjections.GH2533Entity createData(GH2533Repository repository) {
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

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
package org.springframework.data.neo4j.integration.imperative;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.types.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.integration.shared.common.AbstractPet;
import org.springframework.data.neo4j.integration.shared.common.Cat;
import org.springframework.data.neo4j.integration.shared.common.Dog;
import org.springframework.data.neo4j.integration.shared.common.Inheritance;
import org.springframework.data.neo4j.integration.shared.common.KotlinAnimationMovie;
import org.springframework.data.neo4j.integration.shared.common.KotlinCinema;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
public class InheritanceMappingIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	private final Driver driver;

	public InheritanceMappingIT(@Autowired Driver driver) {
		this.driver = driver;
	}

	@BeforeEach
	void deleteData() {
		try (Session session = driver.session()) {
			session.run("MATCH (n) DETACH DELETE n").consume();
		}
	}

	@Test // GH-2138
	void relationshipsShouldHaveCorrectTypes(@Autowired BuildingRepository repository) {

		Long buildingId;
		try (Session session = driver.session()) {
			buildingId = session.run("CREATE (b:Building:Entity{name:'b'})-[:IS_CHILD]->(:Site:Entity{name:'s'})-[:IS_CHILD]->(:Company:Entity{name:'c'}) return id(b) as id").single()
					.get(0).asLong();
		}

		Inheritance.Building building = repository.findById(buildingId).get();

		assertThat(building.name).isEqualTo("b");
		assertThat(building.parent.name).isEqualTo("s");
		assertThat(building.parent).isOfAnyClassIn(Inheritance.Site.class);
		assertThat(building.parent.parent.name).isEqualTo("c");
		assertThat(building.parent.parent).isOfAnyClassIn(Inheritance.Company.class);
	}

	@Test // GH-2138
	void collectionsShouldHaveCorrectTypes(@Autowired TerritoryRepository repository) {

		Long territoryId = createDivisionAndTerritories().get("territoryId").asLong();

		Inheritance.BaseTerritory territory = repository.findById(territoryId).get();

		assertThat(territory.nameEn).isEqualTo("country");

		Inheritance.Country country = new Inheritance.Country("anotherCountry", "large");
		Inheritance.Continent continent = new Inheritance.Continent("continent", "small");
		Inheritance.GenericTerritory genericTerritory = new Inheritance.GenericTerritory("generic");

		assertThat(((Inheritance.Country) territory).relationshipList).containsExactlyInAnyOrder(
				country,
				continent,
				genericTerritory
		);
	}

	@Test // GH-2138
	void resultCollectionShouldHaveCorrectTypes(@Autowired TerritoryRepository repository) {

		createDivisionAndTerritories();

		List<Inheritance.BaseTerritory> territories = repository.findAll();

		Inheritance.Country country1 = new Inheritance.Country("country", "baseCountry");
		Inheritance.Country country2 = new Inheritance.Country("anotherCountry", "large");
		Inheritance.Continent continent = new Inheritance.Continent("continent", "small");
		Inheritance.GenericTerritory genericTerritory = new Inheritance.GenericTerritory("generic");

		assertThat(territories).containsExactlyInAnyOrder(
				country1,
				country2,
				continent,
				genericTerritory
		);
	}

	@Test // GH-2199
	void findAndMapAllConcreteSubclassesWithoutParentLabel(@Autowired PetsRepository petsRepository) {

		try (Session session = driver.session()) {
			Transaction transaction = session.beginTransaction();
			transaction.run("CREATE (:Cat{name:'a'})");
			transaction.run("CREATE (:Cat{name:'a'})");
			transaction.run("CREATE (:Cat{name:'a'})");
			transaction.run("CREATE (:Dog{name:'a'})");
			transaction.commit();
		}

		List<AbstractPet> pets = petsRepository.findPets("a");
		assertThat(pets)
				.hasOnlyElementsOfType(AbstractPet.class)
				.hasAtLeastOneElementOfType(Dog.class)
				.hasAtLeastOneElementOfType(Cat.class);
	}

	@Test // GH-2201
	void shouldDealWithInterfacesWithoutNodeAnnotationRead(@Autowired Neo4jTemplate template) {

		Long id;
		try (Session session = driver.session()) {
			Transaction transaction = session.beginTransaction();
			id = transaction.run("CREATE (s:SomeInterface{name:'s'}) -[:RELATED]-> (:SomeInterface {name:'e'}) RETURN id(s)").single().get(0).asLong();
			transaction.commit();
		}

		Optional<Inheritance.SomeInterfaceEntity> optionalEntity = template.findById(id, Inheritance.SomeInterfaceEntity.class);
		assertThat(optionalEntity).hasValueSatisfying(v -> {
			assertThat(v.getName()).isEqualTo("s");
			assertThat(v).extracting(Inheritance.SomeInterface::getRelated)
					.extracting(Inheritance.SomeInterface::getName).isEqualTo("e");
		});
	}

	@Test // GH-2201
	void shouldDealWithInterfacesWithoutNodeAnnotationWrite(@Autowired Neo4jTemplate template) {

		Inheritance.SomeInterfaceEntity entity = new Inheritance.SomeInterfaceEntity("s");
		entity.setRelated(new Inheritance.SomeInterfaceEntity("e"));
		long id = template.save(entity).getId();

		Optional<Inheritance.SomeInterfaceEntity> optionalEntity = template.findById(id, Inheritance.SomeInterfaceEntity.class);
		assertThat(optionalEntity).hasValueSatisfying(v -> {
			assertThat(v.getName()).isEqualTo("s");
			assertThat(v).extracting(Inheritance.SomeInterface::getRelated)
					.extracting(Inheritance.SomeInterface::getName).isEqualTo("e");
		});
	}

	@Test // GH-2201
	void shouldDealWithInterfacesWithNodeAnnotationRead(@Autowired Neo4jTemplate template) {

		Long id;
		try (Session session = driver.session()) {
			Transaction transaction = session.beginTransaction();
			id = transaction.run("CREATE (s:PrimaryLabelWN{name:'s'}) -[:RELATED]-> (:PrimaryLabelWN {name:'e'}) RETURN id(s)").single().get(0).asLong();
			transaction.commit();
		}

		Optional<Inheritance.SomeInterfaceEntity2> optionalEntity = template.findById(id, Inheritance.SomeInterfaceEntity2.class);
		assertThat(optionalEntity).hasValueSatisfying(v -> {
			assertThat(v.getName()).isEqualTo("s");
			assertThat(v).extracting(Inheritance.SomeInterface2::getRelated)
					.extracting(Inheritance.SomeInterface2::getName).isEqualTo("e");
		});
	}

	@Test // GH-2201
	void shouldDealWithInterfacesWithNodeAnnotationWrite(@Autowired Neo4jTemplate template) {

		Inheritance.SomeInterfaceEntity2 entity = new Inheritance.SomeInterfaceEntity2("s");
		entity.setRelated(new Inheritance.SomeInterfaceEntity2("e"));
		long id = template.save(entity).getId();

		Optional<Inheritance.SomeInterfaceEntity2> optionalEntity = template.findById(id, Inheritance.SomeInterfaceEntity2.class);
		assertThat(optionalEntity).hasValueSatisfying(v -> {
			assertThat(v.getName()).isEqualTo("s");
			assertThat(v).extracting(Inheritance.SomeInterface2::getRelated)
					.extracting(Inheritance.SomeInterface2::getName).isEqualTo("e");
		});
	}

	@Test // GH-2201
	void complexInterfaceMapping(@Autowired Neo4jTemplate template) {

		Long id;
		try (Session session = driver.session()) {
			Transaction transaction = session.beginTransaction();
			id = transaction.run("" +
					"CREATE (s:SomeInterface3:SomeInterface3a{name:'s'}) " +
					"-[:RELATED]-> (:SomeInterface3:SomeInterface3b {name:'m'}) " +
					"-[:RELATED]-> (:SomeInterface3:SomeInterface3a {name:'e'}) RETURN id(s)")
					.single().get(0).asLong();
			transaction.commit();
		}

		Optional<Inheritance.SomeInterfaceImpl3a> optionalEntity = template.findById(id, Inheritance.SomeInterfaceImpl3a.class);
		assertThat(optionalEntity).hasValueSatisfying(v -> {
			assertThat(v.getName()).isEqualTo("s");
			assertThat(v).extracting(Inheritance.SomeInterface3::getRelated)
					.extracting(Inheritance.SomeInterface3::getName).isEqualTo("m");
			assertThat(v).extracting(Inheritance.SomeInterface3::getRelated)
					.extracting(Inheritance.SomeInterface3::getRelated)
					.extracting(Inheritance.SomeInterface3::getName).isEqualTo("e");
		});
	}

	@Test // GH-2201
	void mixedImplementationsRead(@Autowired Neo4jTemplate template) {

		// tag::interface3[]
		Long id;
		try (Session session = driver.session()) {
			Transaction transaction = session.beginTransaction();
			id = transaction.run("" +
				"CREATE (s:ParentModel{name:'s'}) " +
				"CREATE (s)-[:RELATED_1]-> (:SomeInterface3:SomeInterface3b {name:'3b'}) " +
				"CREATE (s)-[:RELATED_2]-> (:SomeInterface3:SomeInterface3a {name:'3a'}) " +
				"RETURN id(s)")
				.single().get(0).asLong();
			transaction.commit();
		}

		Optional<Inheritance.ParentModel> optionalParentModel =
				template.findById(id, Inheritance.ParentModel.class);

		assertThat(optionalParentModel).hasValueSatisfying(v -> {
			assertThat(v.getName()).isEqualTo("s");
			assertThat(v).extracting(Inheritance.ParentModel::getRelated1)
					.isInstanceOf(Inheritance.SomeInterfaceImpl3b.class)
					.extracting(Inheritance.SomeInterface3::getName)
					.isEqualTo("3b");
			assertThat(v).extracting(Inheritance.ParentModel::getRelated2)
					.isInstanceOf(Inheritance.SomeInterfaceImpl3a.class)
					.extracting(Inheritance.SomeInterface3::getName)
					.isEqualTo("3a");
		});
		// end::interface3[]
	}

	@Test // GH-2201
	void mixedImplementationsWrite(@Autowired Neo4jTemplate template) {

		Inheritance.ParentModel entity = new Inheritance.ParentModel("d");
		entity.setRelated1(new Inheritance.SomeInterfaceImpl3b("r13b"));
		entity.setRelated2(new Inheritance.SomeInterfaceImpl3a("r13a"));

		entity = template.save(entity);

		Optional<Inheritance.ParentModel> optionalParentModel = template.findById(entity.getId(), Inheritance.ParentModel.class);
		assertThat(optionalParentModel).hasValueSatisfying(v -> {
			assertThat(v.getName()).isEqualTo("d");
			assertThat(v).extracting(Inheritance.ParentModel::getRelated1)
					.isInstanceOf(Inheritance.SomeInterfaceImpl3b.class)
					.extracting(Inheritance.SomeInterface3::getName)
					.isEqualTo("r13b");
			assertThat(v).extracting(Inheritance.ParentModel::getRelated2)
					.isInstanceOf(Inheritance.SomeInterfaceImpl3a.class)
					.extracting(Inheritance.SomeInterface3::getName)
					.isEqualTo("r13a");
		});
	}

	@Test // GH-2201
	void mixedInterfaces(@Autowired Neo4jTemplate template) {

		Inheritance.Mix1AndMix2 mix1AndMix2 = new Inheritance.Mix1AndMix2("a", "b");

		mix1AndMix2 = template.save(mix1AndMix2);

		assertThat(mix1AndMix2.getName()).isEqualTo("a");
		assertThat(mix1AndMix2.getValue()).isEqualTo("b");

		try (Session session = driver.session()) {
			List<Record> records = session.run("MATCH (n) RETURN n").list();
			assertThat(records).hasSize(1);
			Record record = records.get(0);
			Node node = record.get("n").asNode();
			assertThat(node.labels()).containsExactlyInAnyOrder("Mix1", "Mix2", "Mix1AndMix2");
		}
	}

	@Test // GH-2262
	void shouldMatchPolymorphicClassesWhenFetchedById(@Autowired DivisionRepository repository) {

		Record divisionAndTerritoryId = createDivisionAndTerritories();

		Optional<Inheritance.Division> optionalDivision = repository.findById(divisionAndTerritoryId.get("divisionId").asLong());
		assertThat(optionalDivision).isPresent();
		assertThat(optionalDivision).hasValueSatisfying(twoDifferentClassesHaveBeenLoaded());
	}

	@Test // GH-2262
	void shouldMatchPolymorphicClassesWhenFetchingAll(@Autowired DivisionRepository repository) {

		createDivisionAndTerritories();

		List<Inheritance.Division> divisions = repository.findAll();
		assertThat(divisions).hasSize(1);
		assertThat(divisions).first().satisfies(twoDifferentClassesHaveBeenLoaded());
	}

	private Consumer<Inheritance.Division> twoDifferentClassesHaveBeenLoaded() {
		return d -> {
			assertThat(d.getIsActiveIn()).hasSize(2);
			assertThat(d.getIsActiveIn()).extracting(Inheritance.BaseTerritory::getNameEn)
					.containsExactlyInAnyOrder("anotherCountry", "continent");
			Map<String, Class> classByName = d.getIsActiveIn().stream()
					.collect(Collectors.toMap(Inheritance.BaseTerritory::getNameEn, v -> v.getClass()));
			assertThat(classByName).containsEntry("anotherCountry", Inheritance.Country.class);
			assertThat(classByName).containsEntry("continent", Inheritance.Continent.class);
		};
	}

	@Test // GH-2262
	void shouldMatchPolymorphicInterfacesWhenFetchedById(@Autowired ParentModelRepository repository) {

		Record record = createRelationsToDifferentImplementations();

		Optional<Inheritance.ParentModel2> optionalDivision = repository.findById(record.get(0).asNode().id());
		assertThat(optionalDivision).isPresent();
		assertThat(optionalDivision).hasValueSatisfying(twoDifferentInterfacesHaveBeenLoaded());
	}

	@Test // GH-2262
	void shouldMatchPolymorphicInterfacesWhenFetchingAll(@Autowired ParentModelRepository repository) {

		createRelationsToDifferentImplementations();

		List<Inheritance.ParentModel2> divisions = repository.findAll();
		assertThat(divisions).hasSize(1);
		assertThat(divisions).first().satisfies(twoDifferentInterfacesHaveBeenLoaded());
	}

	@Test // GH-2262
	void shouldMatchPolymorphicKotlinInterfacesWhenFetchingAll(@Autowired CinemaRepository repository) {

		try (Session session = driver.session(); Transaction transaction = session.beginTransaction()) {
			transaction.run("CREATE (:KotlinMovie:KotlinAnimationMovie {id: 'movie001', name: 'movie-001', studio: 'Pixar'})<-[:Plays]-(c:KotlinCinema {id:'cine-01', name: 'GrandRex'}) RETURN id(c) AS id")
					.single().get(0).asLong();
			transaction.commit();
		}

		List<KotlinCinema> divisions = repository.findAll();
		assertThat(divisions).hasSize(1);
		assertThat(divisions).first().satisfies(c -> {
			assertThat(c.getPlays()).hasSize(1);
			assertThat(c.getPlays()).first().isInstanceOf(KotlinAnimationMovie.class)
					.extracting(m -> ((KotlinAnimationMovie) m).getStudio())
					.isEqualTo("Pixar");
		});
	}

	private Consumer<Inheritance.ParentModel2> twoDifferentInterfacesHaveBeenLoaded() {
		return d -> {
			assertThat(d.getIsRelatedTo()).hasSize(2);
			assertThat(d.getIsRelatedTo()).extracting(Inheritance.SomeInterface3::getName)
					.containsExactlyInAnyOrder("3a", "3b");
			Map<String, Class> classByName = d.getIsRelatedTo().stream()
					.collect(Collectors.toMap(Inheritance.SomeInterface3::getName, v -> v.getClass()));
			assertThat(classByName).containsEntry("3a", Inheritance.SomeInterfaceImpl3a.class);
			assertThat(classByName).containsEntry("3b", Inheritance.SomeInterfaceImpl3b.class);
		};
	}

	private Record createDivisionAndTerritories() {
		Record result;
		try (Session session = driver.session()) {

			result = session.run("CREATE (c:Country:BaseTerritory:BaseEntity{nameEn:'country', countryProperty:'baseCountry'}) " +
								 "CREATE (c)-[:LINK]->(ca:Country:BaseTerritory:BaseEntity{nameEn:'anotherCountry', countryProperty:'large'}) " +
								 "CREATE (c)-[:LINK]->(cb:Continent:BaseTerritory:BaseEntity{nameEn:'continent', continentProperty:'small'}) " +
								 "CREATE (c)-[:LINK]->(:GenericTerritory:BaseTerritory:BaseEntity{nameEn:'generic'}) " +
								 "CREATE (d:Division:BaseEntity{name:'Division'}) " +
								 "CREATE (d) -[:IS_ACTIVE_IN] -> (ca)" +
								 "CREATE (d) -[:IS_ACTIVE_IN] -> (cb)" +
								 "RETURN id(d) as divisionId, id(c) as territoryId").single();
		}
		return result;
	}

	private Record createRelationsToDifferentImplementations() {
		Record result;
		try (Session session = driver.session()) {

			result = session.run("CREATE (p:ParentModel2) " +
								 "CREATE (p)-[:IS_RELATED_TO]->(:SomeInterface3:SomeInterface3a {name: '3a'}) " +
								 "CREATE (p)-[:IS_RELATED_TO]->(:SomeInterface3:SomeInterface3b {name: '3b'}) " +
								 "RETURN p").single();
		}
		return result;
	}

	interface PetsRepository extends Neo4jRepository<AbstractPet, Long> {

		@Query("MATCH (n {name: $name}) RETURN n")
		List<AbstractPet> findPets(@Param("name") String name);

	}

	interface BuildingRepository extends Neo4jRepository<Inheritance.Building, Long> {}

	interface TerritoryRepository extends Neo4jRepository<Inheritance.BaseTerritory, Long> {}

	interface DivisionRepository extends Neo4jRepository<Inheritance.Division, Long> {}

	interface ParentModelRepository extends Neo4jRepository<Inheritance.ParentModel2, Long> {}

	interface CinemaRepository extends Neo4jRepository<KotlinCinema, String> {}

	@Configuration
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	@EnableTransactionManagement
	static class Config extends AbstractNeo4jConfig {

		@Override
		protected Collection<String> getMappingBasePackages() {
			return Collections.singleton(Inheritance.class.getPackage().getName());
		}

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

	}
}

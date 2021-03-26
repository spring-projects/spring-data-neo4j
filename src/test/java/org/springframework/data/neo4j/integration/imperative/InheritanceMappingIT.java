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
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.integration.shared.common.AbstractPet;
import org.springframework.data.neo4j.integration.shared.common.Cat;
import org.springframework.data.neo4j.integration.shared.common.Dog;
import org.springframework.data.neo4j.integration.shared.common.Inheritance;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gerrit Meier
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

		Long buildingId = null;
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

		Long territoryId = null;
		try (Session session = driver.session()) {
			territoryId = session.run("CREATE (c:Country:BaseTerritory:BaseEntity{nameEn:'country'}) " +
					"CREATE (c)-[:LINK]->(:Country:BaseTerritory:BaseEntity{nameEn:'anotherCountry', countryProperty:'large'}) " +
					"CREATE (c)-[:LINK]->(:Continent:BaseTerritory:BaseEntity{nameEn:'continent', continentProperty:'small'}) " +
					"CREATE (c)-[:LINK]->(:GenericTerritory:BaseTerritory:BaseEntity{nameEn:'generic'}) " +
					"return id(c) as id").single()
					.get(0).asLong();
		}

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

		try (Session session = driver.session()) {
			session.run("CREATE (c:Country:BaseTerritory:BaseEntity{nameEn:'country', countryProperty:'baseCountry'}) " +
					"CREATE (c)-[:LINK]->(:Country:BaseTerritory:BaseEntity{nameEn:'anotherCountry', countryProperty:'large'}) " +
					"CREATE (c)-[:LINK]->(:Continent:BaseTerritory:BaseEntity{nameEn:'continent', continentProperty:'small'}) " +
					"CREATE (c)-[:LINK]->(:GenericTerritory:BaseTerritory:BaseEntity{nameEn:'generic'})").consume();
		}

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

	interface PetsRepository extends Neo4jRepository<AbstractPet, Long> {

		@Query("MATCH (n {name: $name}) RETURN n")
		List<AbstractPet> findPets(@Param("name") String name);

	}

	interface BuildingRepository extends Neo4jRepository<Inheritance.Building, Long> {}

	interface TerritoryRepository extends Neo4jRepository<Inheritance.BaseTerritory, Long> {}

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

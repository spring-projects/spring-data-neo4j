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
package org.springframework.data.neo4j.conversion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.conversion.datagraph1018.Email;
import org.springframework.data.neo4j.conversion.datagraph1018.Person;
import org.springframework.data.neo4j.conversion.datagraph1018.PersonRepository;
import org.springframework.data.neo4j.conversion.datagraph1156.Hobby;
import org.springframework.data.neo4j.conversion.datagraph1156.HobbyRepository;
import org.springframework.data.neo4j.conversion.datagraph1156.User;
import org.springframework.data.neo4j.conversion.datagraph1156.UserRepository;
import org.springframework.data.neo4j.conversion.support.ConvertedClass;
import org.springframework.data.neo4j.conversion.support.EntityRepository;
import org.springframework.data.neo4j.conversion.support.EntityWithConvertedAttributes;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Michael J. Simons
 * @soundtrack Murray Gold - Doctor Who Season 9
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = MetaDataDrivenConversionServiceIntegrationTests.Config.class)
public class MetaDataDrivenConversionServiceIntegrationTests extends MultiDriverTestClass {

	@Autowired private EntityRepository entityRepository;

	@Autowired private UserRepository userRepository;

	@Autowired private HobbyRepository hobbyRepository;

	@Autowired private TransactionTemplate transactionTemplate;

	@Autowired private PersonRepository personRepository;

	@Test // DATAGRAPH-1131
	public void conversionWithConverterHierarchyShouldWork() {

		ConvertedClass convertedClass = new ConvertedClass();
		convertedClass.setValue("Some value");
		EntityWithConvertedAttributes entity = new EntityWithConvertedAttributes("name");
		entity.setConvertedClass(convertedClass);
		entity.setDoubles(Arrays.asList(21.0, 21.0));
		entity.setTheDouble(42.0);
		entityRepository.save(entity);

		Result result = getGraphDatabaseService()
				.execute("MATCH (e:EntityWithConvertedAttributes) RETURN e.convertedClass, e.doubles, e.theDouble");

		assertThat(result.hasNext()).isTrue();
		Map<String, Object> row = result.next();
		assertThat(row) //
				.containsEntry("e.convertedClass", "n/a") //
				.containsEntry("e.doubles", "21.0,21.0") //
				.containsEntry("e.theDouble", "that has been a double");

	}

	@Test // DATAGRAPH-1156
	public void relatedNodesWithCustomIdsShouldWork() {

		User testUser = transactionTemplate.execute(tx -> userRepository.save(new User("test@test.com")));
		User user = userRepository.findById(testUser.getId())
				.orElseThrow(() -> new RuntimeException("User not saved."));

		Hobby newHobby = transactionTemplate.execute(tx -> hobbyRepository.save(new Hobby("Cycling", user)));
		assertThat(hobbyRepository.findById(newHobby.getId())).isPresent().hasValueSatisfying(hobby -> {
			assertThat(hobby.getId()).isEqualTo(newHobby.getId());
			assertThat(hobby.getHobbyist()).extracting(User::getId).isEqualTo(user.getId());
		});
	}

	private List<UUID> preparePersonTestData() {

		GraphDatabaseService graphDatabaseService = getGraphDatabaseService();
		List<UUID> ids = new ArrayList<>();
		try (Transaction tx = graphDatabaseService.beginTx()) {
			graphDatabaseService.execute("MATCH (n) DETACH DELETE n");
			Result r = graphDatabaseService.execute(
					"UNWIND range(1,5) AS i WITH i CREATE (p:Person {id: randomUUID(), name: 'Person' + i, email: 'person' + i + '@test.com'}) RETURN p.id AS id");
			r.map(m -> (String) m.get("id")).map(UUID::fromString).forEachRemaining(ids::add);
			tx.success();
		}
		return ids;
	}


	@Test // DATAGRAPH-1018
	public void findByConvertedIdShouldWork() {

		List<UUID> ids = preparePersonTestData();
		Optional<Person> optionalPerson = personRepository.findById(ids.get(2));
		assertThat(optionalPerson).isPresent().map(Person::getName).hasValue("Person3");
	}

	@Test // DATAGRAPH-1018
	public void findByConvertedIdsShouldWork() {

		List<UUID> ids = preparePersonTestData();
		Iterable<Person> people = personRepository
				.findAllById(ids.stream().collect(Collectors.toList()));
		assertThat(people).hasSize(ids.size());
	}

	@Test // DATAGRAPH-1018
	public void findByConvertedAttributeShouldWork() {

		List<UUID> ids = preparePersonTestData();
		Optional<Person> optionalPerson = personRepository.findByEmail(new Email("person2@test.com"));
		assertThat(optionalPerson).isPresent().map(Person::getId).hasValue(ids.get(1));
	}

	@Test // DATAGRAPH-1018
	public void findByConvertedAttributesShouldWork() {

		List<UUID> ids = preparePersonTestData();
		Iterable<Person> people = personRepository
				.findAllByEmailIn(Arrays.asList(new Email("person3@test.com"), new Email("person4@test.com")));
		assertThat(people).extracting(Person::getId).containsOnly(ids.get(2), ids.get(3));
	}

	@Configuration
	@EnableNeo4jRepositories(basePackageClasses = {EntityWithConvertedAttributes.class, Hobby.class, Person.class})
	@EnableTransactionManagement
	public static class Config {

		@Bean
		public PlatformTransactionManager transactionManager() {
			return new Neo4jTransactionManager(sessionFactory());
		}

		@Bean
		public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
			return new TransactionTemplate(transactionManager);
		}

		@Bean
		public SessionFactory sessionFactory() {
			return new SessionFactory(getBaseConfiguration().build(),
					EntityWithConvertedAttributes.class.getPackage().getName(), Hobby.class.getPackage().getName(), Person.class.getPackage().getName());
		}
	}
}

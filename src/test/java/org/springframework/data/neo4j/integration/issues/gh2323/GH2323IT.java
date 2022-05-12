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
package org.springframework.data.neo4j.integration.issues.gh2323;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
class GH2323IT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	protected static String personId;

	@BeforeAll
	protected static void setupData(@Autowired BookmarkCapture bookmarkCapture) {
		try (Session session = neo4jConnectionSupport.getDriver().session(bookmarkCapture.createSessionConfig());
				Transaction transaction = session.beginTransaction();
		) {
			transaction.run("MATCH (n) detach delete n");
			personId = transaction.run("CREATE (n:Person {id: randomUUID(), name: 'Helge'}) return n.id").single()
					.get(0)
					.asString();
			transaction.run("unwind ['German', 'English'] as name create (n:Language {name: name}) return name")
					.consume();
			transaction.commit();
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	@BeforeEach
	protected void removeRelationships(@Autowired BookmarkCapture bookmarkCapture) {
		try (Session session = neo4jConnectionSupport.getDriver().session(bookmarkCapture.createSessionConfig());
				Transaction transaction = session.beginTransaction();
		) {
			transaction.run("MATCH ()- [r:KNOWS]-() DELETE r").consume();
			transaction.run("MATCH (n:Language) DELETE n").consume();
			transaction.run("MATCH (n:Person {name: 'Gerrit'}) DETACH DELETE n").consume();
			transaction.run("unwind ['German', 'English'] as name create (n:Language {name: name}) return name")
					.consume();
			transaction.commit();
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	@Test // GH-2323
	void listOfRelationshipPropertiesShouldBeUnwindable(@Autowired PersonService personService) {
		Person person = personService.updateRel(personId, Arrays.asList("German"));
		assertThat(person).isNotNull();
		assertThat(person.getKnownLanguages()).hasSize(1);
		assertThat(person.getKnownLanguages()).first().satisfies(knows -> {
			assertThat(knows.getDescription()).isEqualTo("Some description");
			assertThat(knows.getLanguage()).extracting(Language::getName).isEqualTo("German");
		});
	}

	@RepeatedTest(20) // GH-2386
	void dontMixRelatedNodes(@Autowired PersonRepository repository, @Autowired BookmarkCapture bookmarkCapture) {
		String id;
		try (Session session = neo4jConnectionSupport.getDriver().session(bookmarkCapture.createSessionConfig());
				Transaction transaction = session.beginTransaction();
		) {
			id = transaction.run(
							"CREATE (n:Person {id:randomUUID(), name: 'Gerrit'})-[:KNOWS]->(:Language{name:'English'}) return n.id")
					.single().get(0).asString();
			transaction.run(
							"MATCH (n:Person {name: 'Gerrit'}) MERGE (n)-[:MOTHER_TONGUE_IS]->(:Language{name:'German'})")
					.consume();
			transaction.commit();
			bookmarkCapture.seedWith(session.lastBookmark());

			Person gerrit = repository.findById(id).get();
			List<Knows> knownLanguages = gerrit.getKnownLanguages();
			assertThat(knownLanguages).hasSize(1);
			assertThat(knownLanguages.get(0).getLanguage().getName()).isEqualTo("English");
			KnowsMtEntity motherTongue = gerrit.getMotherTongue();
			assertThat(motherTongue).isNotNull();
			assertThat(motherTongue.getLanguage().getName()).isEqualTo("German");
		}
	}

	@Test // GH-2537
	void ensureRelationshipsAreSerialized(@Autowired PersonService personService) {

		Optional<Person> optionalPerson = personService.updateRel2(personId, Arrays.asList("German"));
		assertThat(optionalPerson).isPresent().hasValueSatisfying(person -> {
			assertThat(person.getKnownLanguages()).hasSize(1);
			assertThat(person.getKnownLanguages()).first().satisfies(knows -> {
				assertThat(knows.getDescription()).isEqualTo("Some description");
				assertThat(knows.getLanguage()).extracting(Language::getName).isEqualTo("German");
			});
		});
	}

	@Repository
	public interface PersonRepository extends Neo4jRepository<Person, String> {

		// Using separate id and than relationships on top level
		@Query(""
			   + "UNWIND $relations As rel WITH rel "
			   + "MATCH (f:Person {id: $from}) "
			   + "MATCH (t:Language {name: rel.__target__.__id__}) "
			   + "CREATE (f)- [r:KNOWS {description: rel.__properties__.description}] -> (t) "
			   + "RETURN f, collect(r), collect(t)"
		)
		Person updateRel(@Param("from") String from, @Param("relations") List<Knows> relations);

		// Using the whole person object
		@Query(""
			   + "UNWIND $person.__properties__.KNOWS As rel WITH rel "
			   + "MATCH (f:Person {id: $person.__id__}) "
			   + "MATCH  (t:Language {name: rel.__target__.__id__}) "
			   + "CREATE (f) - [r:KNOWS {description: rel.__properties__.description}] -> (t) "
			   + "RETURN f, collect(r), collect(t)")
		Person updateRel2(@Param("person") Person person);
	}

	@Service
	static class PersonService {

		private final PersonRepository personRepository;

		PersonService(PersonRepository personRepository) {
			this.personRepository = personRepository;
		}

		public Person updateRel(String from, List<String> languageNames) {

			List<Knows> knownLanguages = languageNames.stream().map(Language::new)
					.map(language -> new Knows("Some description", language))
					.collect(Collectors.toList());
			return personRepository.updateRel(from, knownLanguages);
		}

		public Optional<Person> updateRel2(String id, List<String> languageNames) {

			Optional<Person> original = personRepository.findById(id);
			if (original.isPresent()) {
				Person person = original.get();
				List<Knows> knownLanguages = languageNames.stream().map(Language::new)
						.map(language -> new Knows("Some description", language))
						.collect(Collectors.toList());
				person.setKnownLanguages(knownLanguages);
				return Optional.of(personRepository.updateRel2(person));
			}

			return original;
		}
	}

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	@ComponentScan
	static class Config extends AbstractNeo4jConfig {

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Bean
		public Driver driver() {

			return neo4jConnectionSupport.getDriver();
		}
	}
}

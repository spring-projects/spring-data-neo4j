/*
 * Copyright 2011-2023 the original author or authors.
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
package org.springframework.data.neo4j.integration.issues.gh2639;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jImperativeTestConfiguration;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to verify that relationships within generic entity based relationships work.
 *
 * @author Gerrit Meier
 */
@Neo4jIntegrationTest
public class Gh2639IT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@BeforeAll
	static void setup(@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture) {
		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			session.run("MATCH (n) detach delete n").consume();
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	@Test
	void relationshipsOfGenericRelationshipsGetResolvedCorrectly(@Autowired CompanyRepository companyRepository) {
		Person greg = new Sales("Greg");
		Person roy = new Sales("Roy");
		Person craig = new Sales("Craig");

		Language java = new Language("java", "1.5");
		java.inventor = new Enterprise("Sun", ";(");
		Language perl = new Language("perl", "6.0");
		perl.inventor = new Individual("Larry Wall", "larryW");

		List<LanguageRelationship> languageRelationships = new ArrayList<>();
		LanguageRelationship javaRelationship = new LanguageRelationship(5, java);
		LanguageRelationship perlRelationship = new LanguageRelationship(2, perl);
		languageRelationships.add(javaRelationship);
		languageRelationships.add(perlRelationship);

		Developer harry = new Developer("Harry", languageRelationships);
		List<Person> team = Arrays.asList(greg,	roy, craig,	harry);
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

	interface CompanyRepository extends Neo4jRepository<Company, Long> {
		Company findByName(String name);
	}

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends Neo4jImperativeTestConfiguration {

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Override
		public PlatformTransactionManager transactionManager(
				Driver driver, DatabaseSelectionProvider databaseNameProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new Neo4jTransactionManager(driver, databaseNameProvider,
					Neo4jBookmarkManager.create(bookmarkCapture));
		}

		@Override
		protected Collection<String> getMappingBasePackages() {
			return Collections.singleton(Company.class.getPackage().getName());
		}

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Override
		public boolean isCypher5Compatible() {
			return neo4jConnectionSupport.isCypher5SyntaxCompatible();
		}
	}
}

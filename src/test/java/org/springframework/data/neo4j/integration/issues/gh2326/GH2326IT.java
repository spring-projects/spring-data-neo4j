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
package org.springframework.data.neo4j.integration.issues.gh2326;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
class GH2326IT extends TestBase {

	@Test // GH-2326
	void saveShouldAddAllLabels(@Autowired AnimalRepository animalRepository, @Autowired BookmarkCapture bookmarkCapture) {

		List<Animal> animals = Arrays.asList(new Animal.Pet.Cat(), new Animal.Pet.Dog());
		List<String> ids = animals.stream().map(animalRepository::save).map(BaseEntity::getId)
				.collect(Collectors.toList());

		assertLabels(bookmarkCapture, ids);
	}

	@Test // GH-2326
	void saveAllShouldAddAllLabels(@Autowired AnimalRepository animalRepository, @Autowired BookmarkCapture bookmarkCapture) {

		List<Animal> animals = Arrays.asList(new Animal.Pet.Cat(), new Animal.Pet.Dog());
		List<String> ids = animalRepository.saveAll(animals).stream().map(BaseEntity::getId)
				.collect(Collectors.toList());

		assertLabels(bookmarkCapture, ids);
	}

	public interface AnimalRepository extends Neo4jRepository<Animal, String> {
	}

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories(considerNestedRepositories = true)
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

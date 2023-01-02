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
package org.springframework.data.neo4j.integration.issues.gh2326;

import org.springframework.data.neo4j.test.Neo4jReactiveTestConfiguration;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
class ReactiveGH2326IT extends TestBase {

	@Test // GH-2326
	void saveShouldAddAllLabels(@Autowired AnimalRepository animalRepository, @Autowired BookmarkCapture bookmarkCapture) {

		List<String> ids = new ArrayList<>();
		List<Animal> animals = Arrays.asList(new Animal.Pet.Cat(), new Animal.Pet.Dog());
		Flux.fromIterable(animals).flatMap(animalRepository::save)
				.map(BaseEntity::getId)
				.as(StepVerifier::create)
				.recordWith(() -> ids)
				.expectNextCount(2)
				.verifyComplete();

		assertLabels(bookmarkCapture, ids);
	}

	@Test // GH-2326
	void saveAllShouldAddAllLabels(@Autowired AnimalRepository animalRepository, @Autowired BookmarkCapture bookmarkCapture) {

		List<String> ids = new ArrayList<>();
		List<Animal> animals = Arrays.asList(new Animal.Pet.Cat(), new Animal.Pet.Dog());
		animalRepository.saveAll(animals)
				.map(BaseEntity::getId)
				.as(StepVerifier::create)
				.recordWith(() -> ids)
				.expectNextCount(2)
				.verifyComplete();

		assertLabels(bookmarkCapture, ids);
	}

	public interface AnimalRepository extends ReactiveNeo4jRepository<Animal, String> {
	}

	@Configuration
	@EnableTransactionManagement
	@EnableReactiveNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends Neo4jReactiveTestConfiguration {

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
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

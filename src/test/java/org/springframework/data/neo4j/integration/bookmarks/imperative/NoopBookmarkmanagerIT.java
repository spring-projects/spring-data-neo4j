/*
 * Copyright 2011-present the original author or authors.
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
package org.springframework.data.neo4j.integration.bookmarks.imperative;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.neo4j.driver.Driver;
import org.neo4j.driver.SessionConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.integration.bookmarks.DatabaseInitializer;
import org.springframework.data.neo4j.integration.bookmarks.Person;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jImperativeTestConfiguration;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
public class NoopBookmarkmanagerIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@Test
	void mustNotUseBookmarks(@Autowired PersonService personService, @Autowired Driver driver)
			throws ExecutionException, InterruptedException {

		var movies = personService.getMoviesByActorNameLike("Bill");
		assertThat(movies).hasSize(5);
		var sessionConfigCaptor = ArgumentCaptor.forClass(SessionConfig.class);
		verify(driver, times(5)).session(any(), sessionConfigCaptor.capture());
		assertThat(sessionConfigCaptor.getAllValues()).allMatch(cfg -> {
			var bookmarks = new ArrayList<>();
			if (cfg.bookmarks() != null) {
				cfg.bookmarks().forEach(bookmarks::add);
			}
			return bookmarks.isEmpty();
		});
	}

	interface PersonRepository extends Neo4jRepository<Person, String> {

		@Async
		@Query("MATCH (p:Person) WHERE p.name =~ (('.*' + $name) + '.*') RETURN p.name")
		CompletableFuture<List<String>> findMatchingNames(String name);

		@Query("MATCH (m:Movie)<-[:ACTED_IN]-(p:Person) WHERE p.name= $name return m.title")
		CompletableFuture<List<String>> getPersonMovies(String name);

	}

	@Configuration
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	@EnableTransactionManagement
	@ComponentScan
	@EnableAsync
	static class Config extends Neo4jImperativeTestConfiguration {

		@Bean
		DatabaseInitializer databaseInitializer(Driver driver) {
			return new DatabaseInitializer(driver);
		}

		@Bean
		@Override
		public Driver driver() {
			var driver = neo4jConnectionSupport.getDriver();
			return Mockito.spy(driver);
		}

		@Override
		public Neo4jBookmarkManager bookmarkManager() {
			return Neo4jBookmarkManager.noop();
		}

		@Override
		public boolean isCypher5Compatible() {
			return neo4jConnectionSupport.isCypher5SyntaxCompatible();
		}

	}

	@Service
	static class PersonService {

		private final PersonRepository personRepository;

		PersonService(PersonRepository personRepository) {
			this.personRepository = personRepository;
		}

		List<String> getMoviesByActorNameLike(String namePattern) throws ExecutionException, InterruptedException {

			CompletableFuture<List<String>> completableFutureCompletableFuture = this.personRepository
				.findMatchingNames(namePattern)
				.thenCompose(names -> {
					List<String> result = Collections.synchronizedList(new ArrayList<String>());
					var futures = names.stream()
						.map(this.personRepository::getPersonMovies)
						.map(cf -> cf.thenAccept(result::addAll))
						.toArray(CompletableFuture[]::new);
					return CompletableFuture.allOf(futures).thenApply(__ -> result);
				});
			return completableFutureCompletableFuture.get();
		}

	}

}

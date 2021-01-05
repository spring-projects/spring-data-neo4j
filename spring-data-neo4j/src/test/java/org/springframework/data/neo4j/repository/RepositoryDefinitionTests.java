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
package org.springframework.data.neo4j.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.domain.sample.Movie;
import org.springframework.data.neo4j.repository.sample.repo.MovieRepository;
import org.springframework.data.neo4j.test.GraphDatabaseServiceAssert;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Michal Bachman
 * @author Mark Angrish
 * @author Michael J. Simons
 */
@ContextConfiguration(classes = RepositoryDefinitionTests.RepositoriesTestContext.class)
@RunWith(SpringRunner.class)
public class RepositoryDefinitionTests {

	@Autowired private GraphDatabaseService graphDatabaseService;

	@Autowired private TransactionTemplate transactionTemplate;

	@Autowired private MovieRepository movieRepository;

	@Before
	public void clearDatabase() {
		graphDatabaseService.execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n");
	}

	@Test
	public void shouldProxyAndAutoImplementRepositoryDefinitionAnnotatedRepo() {
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				Movie movie = new Movie("PF");
				movieRepository.save(movie);

				assertThat(StreamSupport
						.stream(movieRepository.findAll().spliterator(), false).count())
						.isEqualTo(1);
			}
		});

		Map<String, Object> params = new HashMap<>();
		params.put("title", "PF");
		GraphDatabaseServiceAssert.assertThat(graphDatabaseService)
				.containsNode("MATCH (n:Movie) WHERE n.title = $title RETURN n", params);

	}

	@Configuration
	@Neo4jIntegrationTest(domainPackages = "org.springframework.data.neo4j.domain.sample",
			repositoryPackages = "org.springframework.data.neo4j.repository.sample.repo")
	static class RepositoriesTestContext {}

}

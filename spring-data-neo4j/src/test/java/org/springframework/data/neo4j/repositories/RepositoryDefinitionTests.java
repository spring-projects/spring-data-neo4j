/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.repositories;

import static org.junit.Assert.*;
import static org.neo4j.ogm.testutil.GraphTestUtils.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.repositories.domain.Movie;
import org.springframework.data.neo4j.repositories.repo.MovieRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.util.IterableUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Michal Bachman
 * @author Mark Angrish
 */
@ContextConfiguration(classes = { RepositoryDefinitionTests.RepositoriesTestContext.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class RepositoryDefinitionTests extends MultiDriverTestClass {

	@Autowired PlatformTransactionManager platformTransactionManager;

	private TransactionTemplate transactionTemplate;

	@Autowired private MovieRepository movieRepository;

	@Before
	public void clearDatabase() {
		transactionTemplate = new TransactionTemplate(platformTransactionManager);
		getGraphDatabaseService().execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n");
	}

	@Test
	public void shouldProxyAndAutoImplementRepositoryDefinitionAnnotatedRepo() {
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				Movie movie = new Movie("PF");
				movieRepository.save(movie);

				assertEquals(1, IterableUtils.count(movieRepository.findAll()));
			}
		});
		assertSameGraph(getGraphDatabaseService(), "CREATE (m:Movie {title:'PF'})");
	}

	@Configuration
	@EnableNeo4jRepositories("org.springframework.data.neo4j.repositories.repo")
	@EnableTransactionManagement
	static class RepositoriesTestContext {

		@Bean
		public PlatformTransactionManager transactionManager() {
			return new Neo4jTransactionManager(sessionFactory());
		}

		@Bean
		public SessionFactory sessionFactory() {
			return new SessionFactory(getBaseConfiguration().build(), "org.springframework.data.neo4j.repositories.domain");
		}
	}

}

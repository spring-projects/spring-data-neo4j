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
package org.springframework.data.neo4j.repository.config;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.domain.sample.User;
import org.springframework.data.neo4j.repository.sample.UserRepository;
import org.springframework.data.neo4j.repository.support.TransactionalRepositoryTests;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.data.repository.support.Repositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Integration test for the combination of JavaConfig and an {@link Repositories} wrapper.
 *
 * @author Mark Angrish
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = RepositoriesJavaConfigTests.Config.class)
public class RepositoriesJavaConfigTests extends MultiDriverTestClass {

	@Configuration
	@EnableNeo4jRepositories(basePackageClasses = UserRepository.class)
	static class Config {

		@Autowired ApplicationContext context;

		@Bean
		public Repositories repositories() {
			return new Repositories(context);
		}

		@Bean
		public TransactionalRepositoryTests.DelegatingTransactionManager transactionManager() throws Exception {
			return new TransactionalRepositoryTests.DelegatingTransactionManager(
					new Neo4jTransactionManager(sessionFactory()));
		}

		@Bean
		public SessionFactory sessionFactory() {
			return new SessionFactory(getBaseConfiguration().build(), "org.springframework.data.neo4j.domain.sample");
		}
	}

	@Autowired Repositories repositories;

	/**
	 */
	@Test
	public void foo() {
		assertThat(repositories.hasRepositoryFor(User.class), is(true));
	}
}

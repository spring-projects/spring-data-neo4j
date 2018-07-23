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
package org.springframework.data.neo4j.extensions;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author: Vince Bickers
 */
@ContextConfiguration(classes = { CustomGraphRepositoryTests.CustomPersistenceContext.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class CustomGraphRepositoryTests extends MultiDriverTestClass {

	@Autowired private UserRepository repository;

	/**
	 * asserts that the correct proxied object is created by Spring and that we can integrate with it.
	 */
	@Test
	public void shouldExposeCommonMethodOnExtendedRepository() {
		assertTrue(repository.sharedCustomMethod());
	}

	@Configuration
	@EnableNeo4jRepositories(repositoryBaseClass = CustomGraphRepositoryImpl.class)
	@EnableTransactionManagement
	static class CustomPersistenceContext {

		@Bean
		public PlatformTransactionManager transactionManager() {
			return new Neo4jTransactionManager(sessionFactory());
		}

		@Bean
		public SessionFactory sessionFactory() {
			return new SessionFactory(getBaseConfiguration().build(), "org.springframework.data.neo4j.extensions.domain");
		}
	}
}

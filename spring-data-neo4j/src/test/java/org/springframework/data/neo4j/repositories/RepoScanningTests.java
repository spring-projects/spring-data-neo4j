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

import static org.neo4j.ogm.testutil.GraphTestUtils.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.repositories.domain.User;
import org.springframework.data.neo4j.repositories.repo.UserRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michal Bachman
 * @author Mark Angrish
 */
@ContextConfiguration(classes = { RepoScanningTests.PersistenceContextInTheSamePackage.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class RepoScanningTests extends MultiDriverTestClass {

	@Autowired private UserRepository userRepository;

	@Before
	public void clearDatabase() {
		getGraphDatabaseService().execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n");
	}

	@Test
	public void enableNeo4jRepositoriesShouldScanSelfPackageByDefault() {

		User user = new User("Michal");
		userRepository.save(user);

		assertSameGraph(getGraphDatabaseService(), "CREATE (u:User {name:'Michal'})");
	}

	@Configuration
	@EnableNeo4jRepositories // no package specified, that's the point of this test
	@EnableTransactionManagement
	static class PersistenceContextInTheSamePackage {

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
